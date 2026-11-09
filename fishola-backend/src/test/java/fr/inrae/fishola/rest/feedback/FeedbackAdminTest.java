package fr.inrae.fishola.rest.feedback;

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
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

/**
 * Retours utilisateurs : chaque retour reçu est conservé, la liste et l'export
 * (CSV échappé, XLSX) sont réservés aux administrateurs nationaux.
 */
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FeedbackAdminTest {

    private static final String DOMAIN = "@feedback.carnet-peche.test";
    private static final String TRICKY_DESCRIPTION = "Bonjour, la carte; ne \"charge\" pas\nDeuxième ligne";

    @Inject
    JwtHelper jwtHelper;

    @Inject
    AgroalDataSource dataSource;

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

    @AfterAll
    @Transactional
    void deleteFeedbacks() {
        DSL.using(dataSource, SQLDialect.POSTGRES)
                .execute("DELETE FROM user_feedback WHERE email LIKE ?", "%" + DOMAIN);
    }

    @Test
    void receivedFeedbackIsListedForNationalAdmin() {
        String email = "liste" + DOMAIN;
        sendFeedback(email, "Ergonomie", null);

        given().cookie(AbstractCarnetResource.ADMIN_AUTHENTICATION_COOKIE_NAME, nationalToken)
                .when().get("/api/v1/admin/feedbacks")
                .then().statusCode(200)
                .body("find { it.email == '" + email + "' }.category", equalTo("Ergonomie"))
                .body("find { it.email == '" + email + "' }.source", equalTo("Application pêcheur"))
                .body("find { it.email == '" + email + "' }.description", equalTo(TRICKY_DESCRIPTION));
    }

    @Test
    void csvExportEscapesSeparatorsQuotesAndNewlines() {
        String email = "csv" + DOMAIN;
        sendFeedback(email, "Bug", "Administration");

        String expectedCell = "\"" + TRICKY_DESCRIPTION.replace("\"", "\"\"") + "\"";
        exportAs(nationalToken, "csv").then().statusCode(200)
                .header("Content-Disposition", containsString(".csv"))
                .body(containsString("Bug;Administration;Carte;" + email + ";" + expectedCell + ";oui;"));
    }

    @Test
    void xlsxExportKeepsDescriptionInOneCell() throws IOException {
        String email = "xlsx" + DOMAIN;
        sendFeedback(email, "Performance", null);

        byte[] bytes = exportAs(nationalToken, "xlsx").then().statusCode(200).extract().asByteArray();
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            Assertions.assertEquals("description", sheet.getRow(0).getCell(5).getStringCellValue());
            Row row = findRowByEmail(sheet, email);
            Assertions.assertEquals("Performance", row.getCell(1).getStringCellValue());
            Assertions.assertEquals(TRICKY_DESCRIPTION, row.getCell(5).getStringCellValue());
        }
    }

    @Test
    void formulaLikeCellIsNeutralisedInCsv() {
        Assertions.assertEquals("\"'=HYPERLINK(\"\"x\"\")\"", FeedbackExport.csvCell("=HYPERLINK(\"x\")"));
        Assertions.assertEquals("'+33 6", FeedbackExport.csvCell("+33 6"));
    }

    @Test
    void screenshotIsStoredAndServedToNationalAdminOnly() {
        String email = "capture" + DOMAIN;
        sendFeedback(email, "Bug", null);
        String feedbackId = given().cookie(AbstractCarnetResource.ADMIN_AUTHENTICATION_COOKIE_NAME, nationalToken)
                .when().get("/api/v1/admin/feedbacks")
                .then().statusCode(200)
                .body("find { it.email == '" + email + "' }.hasScreenshot", equalTo(true))
                .extract().path("find { it.email == '" + email + "' }.id");

        byte[] png = screenshotAs(nationalToken, feedbackId).then().statusCode(200)
                .contentType("image/png").extract().asByteArray();
        Assertions.assertEquals((byte) 0x89, png[0]);
        Assertions.assertEquals('P', png[1]);
        screenshotAs(regionalToken, feedbackId).then().statusCode(403);
        screenshotAs(nationalToken, UUID.randomUUID().toString()).then().statusCode(404);
    }

    @Test
    void regionalAdminAndOperatorAreDenied() {
        for (String token : new String[]{regionalToken, operatorToken}) {
            given().cookie(AbstractCarnetResource.ADMIN_AUTHENTICATION_COOKIE_NAME, token)
                    .when().get("/api/v1/admin/feedbacks")
                    .then().statusCode(403);
            exportAs(token, "csv").then().statusCode(403);
        }
    }

    private void sendFeedback(String email, String category, String source) {
        Map<String, Object> body = new HashMap<>(Map.of(
                "category", category,
                "module", "Carte",
                "email", email,
                "description", TRICKY_DESCRIPTION,
                "screenshot", "data:image/png;base64," + ONE_PIXEL_PNG));
        if (source != null) {
            body.put("source", source);
        }
        given().contentType(MediaType.APPLICATION_JSON).body(body)
                .when().put("/api/v1/feedback")
                .then().statusCode(204);
    }

    private io.restassured.response.Response exportAs(String adminToken, String format) {
        return given().cookie(AbstractCarnetResource.ADMIN_AUTHENTICATION_COOKIE_NAME, adminToken)
                .queryParam("format", format)
                .when().get("/api/v1/admin/feedbacks/export");
    }

    private io.restassured.response.Response screenshotAs(String adminToken, String feedbackId) {
        return given().cookie(AbstractCarnetResource.ADMIN_AUTHENTICATION_COOKIE_NAME, adminToken)
                .when().get("/api/v1/admin/feedbacks/" + feedbackId + "/screenshot");
    }

    private static Row findRowByEmail(Sheet sheet, String email) {
        for (Row row : sheet) {
            if (row.getCell(4) != null && email.equals(row.getCell(4).getStringCellValue())) {
                return row;
            }
        }
        throw new AssertionError("Aucune ligne pour " + email);
    }

    private void insertStaff(DSLContext ctx, UUID id, String name, boolean national, boolean operator) {
        ctx.execute("INSERT INTO fishola_admin (id, email, password, created_on, can_create_admin, is_national_admin, is_operator) "
                + "VALUES (?, ?, 'x', now(), false, ?, ?)", id, name + "-staff" + DOMAIN, national, operator);
    }

    private static final String ONE_PIXEL_PNG =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";
}
