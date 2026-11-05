package fr.inrae.fishola.rest.newsletter;

/*-
 * #%L
 * Carnet de pêche :: Backend
 * %%
 * Copyright (C) 2019 - 2026 INRAE - UMR CARRTEL
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import fr.inrae.fishola.rest.AbstractCarnetResource;
import fr.inrae.fishola.rest.JwtHelper;
import io.agroal.api.AgroalDataSource;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.MediaType;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Inscription à la newsletter de la page d'accueil publique (#313) : double
 * opt-in, pas de doublon, désinscription, consentement obligatoire, limitation
 * de débit et liste / export réservés aux administrateurs de leur périmètre.
 */
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NewsletterTest {

    private static final String DOMAIN = "@newsletter.carnet-peche.test";

    @Inject
    JwtHelper jwtHelper;

    @Inject
    AgroalDataSource dataSource;

    @Inject
    NewsletterRateLimiter rateLimiter;

    private final UUID nationalAdminId = UUID.randomUUID();
    private final UUID regionalAdminId = UUID.randomUUID();
    private final UUID operatorId = UUID.randomUUID();
    private String nationalToken;
    private String regionalToken;
    private String operatorToken;

    @BeforeAll
    @Transactional
    void seedStaff() {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        insertStaff(ctx, nationalAdminId, "national", true, false);
        insertStaff(ctx, regionalAdminId, "regional", false, false);
        insertStaff(ctx, operatorId, "operateur", false, true);
        ctx.execute("INSERT INTO fishola_admin_departments (fishola_admin_id, department_code) VALUES (?, '74')", regionalAdminId);
        ManagedContext requestContext = Arc.container().requestContext();
        requestContext.activate();
        try {
            nationalToken = jwtHelper.createAdminToken(nationalAdminId);
            regionalToken = jwtHelper.createAdminToken(regionalAdminId);
            operatorToken = jwtHelper.createAdminToken(operatorId);
        } finally {
            requestContext.deactivate();
        }
    }

    @BeforeEach
    void resetRateLimiter() {
        rateLimiter.reset();
    }

    @AfterAll
    @Transactional
    void deleteSubscriptions() {
        DSL.using(dataSource, SQLDialect.POSTGRES)
                .execute("DELETE FROM newsletter_subscription WHERE email LIKE ?", "%" + DOMAIN);
    }

    @Test
    void subscriptionWithoutConsentIsRefused() {
        String email = "sans-consentement" + DOMAIN;
        subscribe(new NewsletterSubscriptionRequest(email, "74", false))
                .then().statusCode(400).body("consent", notNullValue());
        Assertions.assertEquals(0, countSubscriptions(email));
    }

    @Test
    void invalidEmailOrDepartmentIsRefused() {
        subscribe(new NewsletterSubscriptionRequest("pas-un-email", null, true))
                .then().statusCode(400).body("email", notNullValue());
        subscribe(new NewsletterSubscriptionRequest("departement" + DOMAIN, "00", true))
                .then().statusCode(400).body("departmentCode", notNullValue());
    }

    @Test
    void subscriptionStaysPendingUntilConfirmed() {
        String email = "en-attente" + DOMAIN;
        subscribe(new NewsletterSubscriptionRequest(" En-Attente" + DOMAIN.toUpperCase() + " ", "74", true))
                .then().statusCode(200);

        Assertions.assertEquals(1, countSubscriptions(email));
        given().cookie(AbstractCarnetResource.ADMIN_AUTHENTICATION_COOKIE_NAME, nationalToken)
                .when().get("/api/v1/admin/newsletter-subscriptions")
                .then().statusCode(200)
                .body("find { it.email == '" + email + "' }.confirmedOn", nullValue())
                .body("find { it.email == '" + email + "' }.departmentName", equalTo("Haute-Savoie"));
        exportAs(nationalToken).then().body(containsString(email + ";74;"), containsString(";en attente;"));
    }

    @Test
    void subscribingTwiceCreatesNoDuplicate() {
        String email = "deux-fois" + DOMAIN;
        subscribe(new NewsletterSubscriptionRequest(email, "74", true)).then().statusCode(200);
        subscribe(new NewsletterSubscriptionRequest(email, "73", true)).then().statusCode(200);
        Assertions.assertEquals(1, countSubscriptions(email));

        confirm(tokenOf(email)).then().statusCode(307);
        UUID confirmedToken = tokenOf(email);
        subscribe(new NewsletterSubscriptionRequest(email, "01", true)).then().statusCode(200);
        Assertions.assertEquals(confirmedToken, tokenOf(email), "Une adresse confirmée n'est pas réinitialisée");
    }

    @Test
    void confirmationLinkMarksSubscriberConfirmedInExport() {
        String email = "confirme" + DOMAIN;
        subscribe(new NewsletterSubscriptionRequest(email, null, true)).then().statusCode(200);

        confirm(tokenOf(email)).then().statusCode(307)
                .header("Location", endsWith("/api/newsletter_confirm_ok.html"));
        exportAs(nationalToken).then().statusCode(200)
                .body(containsString(email + ";;"), containsString(";confirmé;"))
                .body(containsString("/api/v1/newsletter/unsubscribe?t=" + tokenOf(email)));
    }

    @Test
    void unknownConfirmationTokenFails() {
        confirm(UUID.randomUUID()).then().statusCode(307)
                .header("Location", endsWith("/api/newsletter_confirm_fail.html"));
        given().redirects().follow(false).queryParam("t", "pas-un-jeton")
                .when().get("/api/v1/newsletter/confirm")
                .then().statusCode(307).header("Location", endsWith("/api/newsletter_confirm_fail.html"));
    }

    @Test
    void unsubscribeLinkRemovesSubscriberFromExport() {
        String email = "desinscrit" + DOMAIN;
        subscribe(new NewsletterSubscriptionRequest(email, "74", true)).then().statusCode(200);
        UUID token = tokenOf(email);
        confirm(token).then().statusCode(307);

        given().queryParam("t", token.toString()).when().get("/api/v1/newsletter/unsubscribe")
                .then().statusCode(200).body(containsString("/api/v1/newsletter/unsubscribe?t=" + token));
        exportAs(nationalToken).then().body(containsString(email));

        given().queryParam("t", token.toString()).when().post("/api/v1/newsletter/unsubscribe")
                .then().statusCode(200).body(containsString("désinscrit"));
        exportAs(nationalToken).then().body(not(containsString(email)));
        given().cookie(AbstractCarnetResource.ADMIN_AUTHENTICATION_COOKIE_NAME, nationalToken)
                .when().get("/api/v1/admin/newsletter-subscriptions")
                .then().body("email", not(hasItem(email)));
        confirm(token).then().header("Location", endsWith("/api/newsletter_confirm_fail.html"));
    }

    @Test
    void resubscribingAfterUnsubscribeNeedsNewConfirmation() {
        String email = "retour" + DOMAIN;
        subscribe(new NewsletterSubscriptionRequest(email, null, true)).then().statusCode(200);
        UUID firstToken = tokenOf(email);
        confirm(firstToken).then().statusCode(307);
        given().queryParam("t", firstToken.toString()).when().post("/api/v1/newsletter/unsubscribe");

        subscribe(new NewsletterSubscriptionRequest(email, null, true)).then().statusCode(200);
        Assertions.assertNotEquals(firstToken, tokenOf(email));
        exportAs(nationalToken).then().body(containsString(email + ";;"), containsString(";en attente;"));
    }

    @Test
    void subscriptionsAreRateLimitedPerIp() {
        for (int i = 0; i < 5; i++) {
            subscribeFrom("203.0.113.7", "flood" + i + DOMAIN).then().statusCode(200);
        }
        subscribeFrom("203.0.113.7", "flood-refuse" + DOMAIN).then().statusCode(429);
        subscribeFrom("203.0.113.8", "autre-ip" + DOMAIN).then().statusCode(200);
        Assertions.assertEquals(0, countSubscriptions("flood-refuse" + DOMAIN));
    }

    @Test
    void regionalAdminSeesOnlyPerimeterAndOperatorIsDenied() {
        String inPerimeter = "haute-savoie" + DOMAIN;
        String outOfPerimeter = "bouches-du-rhone" + DOMAIN;
        String withoutDepartment = "sans-departement" + DOMAIN;
        subscribe(new NewsletterSubscriptionRequest(inPerimeter, "74", true)).then().statusCode(200);
        subscribe(new NewsletterSubscriptionRequest(outOfPerimeter, "13", true)).then().statusCode(200);
        subscribe(new NewsletterSubscriptionRequest(withoutDepartment, null, true)).then().statusCode(200);

        exportAs(regionalToken).then().statusCode(200)
                .body(containsString(inPerimeter), not(containsString(outOfPerimeter)),
                        not(containsString(withoutDepartment)));
        exportAs(nationalToken).then()
                .body(containsString(inPerimeter), containsString(outOfPerimeter), containsString(withoutDepartment));
        exportAs(operatorToken).then().statusCode(403);
        given().when().get("/api/v1/admin/newsletter-subscriptions").then().statusCode(401);
    }

    private io.restassured.response.Response subscribe(NewsletterSubscriptionRequest bean) {
        return given().contentType(MediaType.APPLICATION_JSON).body(bean)
                .when().post("/api/v1/newsletter/subscribe");
    }

    private io.restassured.response.Response subscribeFrom(String ip, String email) {
        return given().header("X-Forwarded-For", ip + ", 10.0.0.1")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new NewsletterSubscriptionRequest(email, null, true))
                .when().post("/api/v1/newsletter/subscribe");
    }

    private io.restassured.response.Response confirm(UUID token) {
        return given().redirects().follow(false).queryParam("t", token.toString())
                .when().get("/api/v1/newsletter/confirm");
    }

    private io.restassured.response.Response exportAs(String adminToken) {
        return given().cookie(AbstractCarnetResource.ADMIN_AUTHENTICATION_COOKIE_NAME, adminToken)
                .when().get("/api/v1/admin/newsletter-subscriptions/export");
    }

    private void insertStaff(DSLContext ctx, UUID id, String name, boolean national, boolean operator) {
        ctx.execute("INSERT INTO fishola_admin (id, email, password, created_on, can_create_admin, is_national_admin, is_operator) "
                + "VALUES (?, ?, 'x', now(), false, ?, ?)", id, name + "-staff" + DOMAIN, national, operator);
    }

    private UUID tokenOf(String email) {
        return (UUID) DSL.using(dataSource, SQLDialect.POSTGRES)
                .fetchValue("SELECT token FROM newsletter_subscription WHERE email = ?", email);
    }

    private int countSubscriptions(String email) {
        return DSL.using(dataSource, SQLDialect.POSTGRES)
                .fetchCount(DSL.table("newsletter_subscription"), DSL.field("email").eq(email));
    }
}
