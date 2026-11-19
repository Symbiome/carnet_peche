package fr.inrae.fishola.rest.feedback;

import fr.inrae.fishola.rest.AbstractCarnetResource;
import fr.inrae.fishola.rest.JwtHelper;
import io.agroal.api.AgroalDataSource;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.MediaType;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;

/**
 * Accès à {@code PUT /api/v1/feedback} : réservé aux pêcheurs connectés et aux comptes staff,
 * hormis le formulaire de contact public (catégorie CONTACT sans capture d'écran).
 */
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FeedbackAccessTest {

    private static final String DOMAIN = "@feedback-access.carnet-peche.test";
    private static final String SCREENSHOT = "data:image/png;base64,"
            + "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";

    @Inject
    JwtHelper jwtHelper;

    @Inject
    AgroalDataSource dataSource;

    private final UUID anglerId = UUID.randomUUID();
    private final UUID staffId = UUID.randomUUID();
    private String anglerToken;
    private String staffToken;

    /**
     * Crée un pêcheur et un opérateur dédiés à cette suite, puis forge leurs jetons.
     */
    @BeforeAll
    @Transactional
    void seedAccounts() {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        ctx.execute("INSERT INTO fishola_user (id, first_name, last_name, email, password, created_on, pseudo) "
                + "VALUES (?, 'Feedback', 'Access', ?, 'x', now(), 'feedback-access-test')", anglerId, "angler" + DOMAIN);
        ctx.execute("INSERT INTO fishola_admin (id, email, password, created_on, can_create_admin, is_national_admin, is_operator) "
                + "VALUES (?, ?, 'x', now(), false, false, true)", staffId, "staff" + DOMAIN);
        ManagedContext requestContext = Arc.container().requestContext();
        requestContext.activate();
        try {
            anglerToken = jwtHelper.createUserToken(anglerId);
            staffToken = jwtHelper.createAdminToken(staffId);
        } finally {
            requestContext.deactivate();
        }
    }

    /**
     * Supprime les feedbacks et comptes créés par cette suite.
     */
    @AfterAll
    @Transactional
    void deleteSeededData() {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        ctx.execute("DELETE FROM user_feedback WHERE email LIKE ?", "%" + DOMAIN);
        ctx.execute("DELETE FROM fishola_user WHERE id = ?", anglerId);
        ctx.execute("DELETE FROM fishola_admin WHERE id = ?", staffId);
    }

    @Test
    void anonymousFeedbackIsRejected() {
        putFeedback(given(), "Bug", null).then().statusCode(401);
    }

    @Test
    void anonymousScreenshotIsRejectedEvenAsContact() {
        putFeedback(given(), "Bug", SCREENSHOT).then().statusCode(401);
        putFeedback(given(), "CONTACT", SCREENSHOT).then().statusCode(401);
    }

    @Test
    void forgedTokenIsRejected() {
        putFeedback(given().cookie(AbstractCarnetResource.USER_AUTHENTICATION_COOKIE_NAME, "forged"), "Bug", null)
                .then().statusCode(401);
        putFeedback(given().cookie(AbstractCarnetResource.ADMIN_AUTHENTICATION_COOKIE_NAME, "forged"), "Bug", null)
                .then().statusCode(401);
    }

    @Test
    void anonymousContactMessageIsAccepted() {
        putFeedback(given(), "CONTACT", null).then().statusCode(204);
    }

    @Test
    void authenticatedAnglerCanSendScreenshot() {
        putFeedback(given().cookie(AbstractCarnetResource.USER_AUTHENTICATION_COOKIE_NAME, anglerToken), "Bug", SCREENSHOT)
                .then().statusCode(204);
    }

    @Test
    void authenticatedStaffCanSendScreenshot() {
        putFeedback(given().cookie(AbstractCarnetResource.ADMIN_AUTHENTICATION_COOKIE_NAME, staffToken), "Bug", SCREENSHOT)
                .then().statusCode(204);
    }

    /**
     * Envoie un feedback avec la requête fournie (cookies éventuels déjà positionnés).
     *
     * @param request    la requête RestAssured à compléter
     * @param category   la catégorie du feedback
     * @param screenshot la capture d'écran en data URL, ou null
     * @return la réponse du serveur
     */
    private io.restassured.response.Response putFeedback(RequestSpecification request, String category, String screenshot) {
        Map<String, Object> body = new HashMap<>(Map.of(
                "category", category,
                "email", category.toLowerCase() + DOMAIN,
                "description", "Test d'accès au feedback"));
        if (screenshot != null) {
            body.put("screenshot", screenshot);
        }
        return request.contentType(MediaType.APPLICATION_JSON).body(body)
                .when().put("/api/v1/feedback");
    }
}
