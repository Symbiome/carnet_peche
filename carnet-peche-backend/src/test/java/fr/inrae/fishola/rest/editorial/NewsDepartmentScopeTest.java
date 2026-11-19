package fr.inrae.fishola.rest.editorial;

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
import fr.inrae.fishola.database.NewsCarnetDao;
import fr.inrae.fishola.database.ReferentialDao;
import fr.inrae.fishola.entities.tables.pojos.News;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static io.restassured.RestAssured.given;

/**
 * Portée des communications (#233) : une actu d'un admin national est
 * nationale, celle d'un admin régional cible les départements de son
 * périmètre ; le pêcheur voit les nationales, celles du département de son
 * code postal et celles des départements de ses milieux favoris.
 * <p>
 * Départements d'outre-mer (976, 974) pour rester à l'écart des données de la
 * fixture.
 */
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NewsDepartmentScopeTest {

    private static final String REGIONAL_DEPARTMENT = "976";
    private static final String OTHER_DEPARTMENT = "974";

    @Inject
    JwtHelper jwtHelper;

    @Inject
    AgroalDataSource dataSource;

    @Inject
    NewsCarnetDao newsDao;

    @Inject
    ReferentialDao referentialDao;

    private final UUID nationalId = UUID.randomUUID();
    private final UUID regionalId = UUID.randomUUID();
    private final UUID otherRegionalId = UUID.randomUUID();
    private final UUID localAnglerId = UUID.randomUUID();
    private final UUID remoteAnglerId = UUID.randomUUID();
    private final UUID favoriteAnglerId = UUID.randomUUID();
    private UUID waterEntityInRegionalDepartment;
    private String nationalToken;
    private String regionalToken;
    private String otherRegionalToken;
    private String localAnglerToken;
    private String remoteAnglerToken;
    private String favoriteAnglerToken;

    @BeforeAll
    @Transactional
    void seedAccounts() {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        insertAdmin(ctx, nationalId, true);
        insertAdmin(ctx, regionalId, false);
        insertAdmin(ctx, otherRegionalId, false);
        ctx.execute("INSERT INTO fishola_admin_departments (fishola_admin_id, department_code) VALUES (?, ?)",
                regionalId, REGIONAL_DEPARTMENT);
        ctx.execute("INSERT INTO fishola_admin_departments (fishola_admin_id, department_code) VALUES (?, ?)",
                otherRegionalId, OTHER_DEPARTMENT);

        insertAngler(ctx, localAnglerId, "97600");
        insertAngler(ctx, remoteAnglerId, "69000");
        insertAngler(ctx, favoriteAnglerId, "69000");
        waterEntityInRegionalDepartment = referentialDao.listWaterEntities().get(0).getId();
        ctx.execute("INSERT INTO water_entity_department (water_entity_id, department_code) VALUES (?, ?)",
                waterEntityInRegionalDepartment, REGIONAL_DEPARTMENT);
        ctx.execute("INSERT INTO fishola_user_favorite_water_entities (fishola_user_id, water_entity_id) VALUES (?, ?)",
                favoriteAnglerId, waterEntityInRegionalDepartment);

        ManagedContext requestContext = Arc.container().requestContext();
        requestContext.activate();
        try {
            nationalToken = jwtHelper.createAdminToken(nationalId);
            regionalToken = jwtHelper.createAdminToken(regionalId);
            otherRegionalToken = jwtHelper.createAdminToken(otherRegionalId);
            localAnglerToken = jwtHelper.createUserToken(localAnglerId);
            remoteAnglerToken = jwtHelper.createUserToken(remoteAnglerId);
            favoriteAnglerToken = jwtHelper.createUserToken(favoriteAnglerId);
        } finally {
            requestContext.deactivate();
        }
    }

    private void insertAdmin(DSLContext ctx, UUID id, boolean isNational) {
        ctx.execute("INSERT INTO fishola_admin (id, email, password, created_on, can_create_admin, is_national_admin, is_operator) "
                + "VALUES (?, ?, 'x', now(), false, ?, false)", id, "news-scope-" + id + "@carnet-peche.test", isNational);
    }

    private void insertAngler(DSLContext ctx, UUID id, String postalCode) {
        ctx.execute("INSERT INTO fishola_user (id, first_name, last_name, email, password, created_on, pseudo, postal_code) "
                + "VALUES (?, 'News', 'Scope', ?, 'x', now(), ?, ?)",
                id, "news-scope-" + id + "@carnet-peche.test", "news-scope-" + id, postalCode);
    }

    @AfterAll
    @Transactional
    void cleanup() {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        ctx.execute("DELETE FROM news WHERE name LIKE 'news-scope-%'");
        ctx.execute("DELETE FROM water_entity_department WHERE water_entity_id = ? AND department_code = ?",
                waterEntityInRegionalDepartment, REGIONAL_DEPARTMENT);
        ctx.execute("DELETE FROM fishola_user_favorite_water_entities WHERE fishola_user_id = ?", favoriteAnglerId);
        ctx.execute("DELETE FROM fishola_user WHERE id IN (?, ?, ?)", localAnglerId, remoteAnglerId, favoriteAnglerId);
        ctx.execute("DELETE FROM fishola_admin WHERE id IN (?, ?, ?)", nationalId, regionalId, otherRegionalId);
    }

    private RequestSpecification asAdmin(String token) {
        return given().contentType(MediaType.APPLICATION_JSON)
                .cookie(AbstractCarnetResource.ADMIN_AUTHENTICATION_COOKIE_NAME, token);
    }

    private RequestSpecification asAngler(String token) {
        return given().cookie(AbstractCarnetResource.USER_AUTHENTICATION_COOKIE_NAME, token);
    }

    private News publish(String adminToken, String name, boolean claimsNational) {
        LocalDateTime now = LocalDateTime.now();
        NewsBean bean = new NewsBean(null, name, "<p>contenu</p>", now.minusDays(1), now.plusDays(10),
                null, null, claimsNational, null);
        asAdmin(adminToken).body(bean).when().post("/api/v1/news-all").then().statusCode(204);
        return newsDao.getNews(false, java.util.Optional.empty()).stream()
                .filter(news -> news.getName().equals(name))
                .findFirst().orElseThrow();
    }

    private List<String> newsNamesSeenBy(RequestSpecification request, String path) {
        return request.when().get(path).then().statusCode(200).extract().jsonPath().getList("name", String.class);
    }

    @Test
    @Transactional
    void nationalAdminPublishesNationalNews() {
        News news = publish(nationalToken, "news-scope-national", false);

        Assertions.assertTrue(news.getIsNational());
        Assertions.assertEquals(Set.of(), newsDao.getDepartmentCodes(news.getId()));
        Assertions.assertTrue(newsNamesSeenBy(given(), "/api/v1/news").contains("news-scope-national"));
    }

    @Test
    @Transactional
    void regionalAdminPublishesOnItsDepartmentsOnly() {
        News news = publish(regionalToken, "news-scope-regional", true);

        Assertions.assertFalse(news.getIsNational());
        Assertions.assertEquals(Set.of(REGIONAL_DEPARTMENT), newsDao.getDepartmentCodes(news.getId()));
    }

    @Test
    @Transactional
    void anglerSeesNewsOfPostalCodeAndFavoriteDepartments() {
        publish(regionalToken, "news-scope-angler", false);

        Assertions.assertTrue(newsNamesSeenBy(asAngler(localAnglerToken), "/api/v1/news").contains("news-scope-angler"));
        Assertions.assertTrue(newsNamesSeenBy(asAngler(favoriteAnglerToken), "/api/v1/news").contains("news-scope-angler"));
        Assertions.assertFalse(newsNamesSeenBy(asAngler(remoteAnglerToken), "/api/v1/news").contains("news-scope-angler"));
        Assertions.assertFalse(newsNamesSeenBy(given(), "/api/v1/news").contains("news-scope-angler"));
    }

    @Test
    @Transactional
    void waterEntityPageShowsNewsOfItsDepartments() {
        publish(regionalToken, "news-scope-water-entity", false);

        List<String> names = newsNamesSeenBy(given(), "/api/v1/news/waterEntity/" + waterEntityInRegionalDepartment);
        Assertions.assertTrue(names.contains("news-scope-water-entity"));
    }

    @Test
    @Transactional
    void otherRegionalAdminCannotSeeNorTouchNews() {
        News news = publish(regionalToken, "news-scope-foreign", false);

        List<String> listed = asAdmin(otherRegionalToken).when().get("/api/v1/news-all")
                .then().statusCode(200).extract().jsonPath().getList("name", String.class);
        Assertions.assertFalse(listed.contains("news-scope-foreign"));
        NewsBean bean = new NewsBean(news.getId(), "news-scope-foreign-renamed", news.getContent(),
                news.getDatePublicationDebut(), news.getDatePublicationFin(), null, null, false, null);
        asAdmin(otherRegionalToken).body(bean).when().put("/api/v1/news-all/" + news.getId()).then().statusCode(403);
        asAdmin(otherRegionalToken).when().delete("/api/v1/news-all/" + news.getId()).then().statusCode(403);
    }

    @Test
    @Transactional
    void regionalAdminCannotEditNationalNewsAndUpdateKeepsScope() {
        News national = publish(nationalToken, "news-scope-national-locked", false);
        NewsBean bean = new NewsBean(national.getId(), "news-scope-national-locked", national.getContent(),
                national.getDatePublicationDebut(), national.getDatePublicationFin(), null, null, false, null);
        asAdmin(regionalToken).body(bean).when().put("/api/v1/news-all/" + national.getId()).then().statusCode(403);

        News regional = publish(regionalToken, "news-scope-regional-update", false);
        NewsBean update = new NewsBean(regional.getId(), "news-scope-regional-updated", regional.getContent(),
                regional.getDatePublicationDebut(), regional.getDatePublicationFin(), null, null, true, null);
        asAdmin(regionalToken).body(update).when().put("/api/v1/news-all/" + regional.getId()).then().statusCode(204);
        News updated = newsDao.findById(regional.getId());
        Assertions.assertEquals("news-scope-regional-updated", updated.getName());
        Assertions.assertFalse(updated.getIsNational());
        Assertions.assertEquals(Set.of(REGIONAL_DEPARTMENT), newsDao.getDepartmentCodes(regional.getId()));
    }
}
