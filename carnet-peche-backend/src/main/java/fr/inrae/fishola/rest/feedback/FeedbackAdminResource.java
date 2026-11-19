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

import fr.inrae.fishola.database.UserFeedbackDao;
import fr.inrae.fishola.entities.tables.pojos.UserFeedback;
import fr.inrae.fishola.rest.AbstractCarnetResource;
import fr.inrae.fishola.rest.audit.Audited;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Retours utilisateurs (app pêcheur et administration), réservés aux
 * administrateurs nationaux : liste classée par catégorie et export CSV ou XLSX.
 */
@Path("/api/v1/admin/feedbacks")
@Produces(MediaType.APPLICATION_JSON)
public class FeedbackAdminResource extends AbstractCarnetResource {

    private static final String XLSX_MEDIA_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String CSV_MEDIA_TYPE = "text/csv; charset=UTF-8";

    @Inject
    protected UserFeedbackDao userFeedbackDao;

    /** @return tous les retours, classés par catégorie puis du plus récent au plus ancien */
    @GET
    public List<UserFeedback> list() {
        checkIsNationalAdmin();
        return userFeedbackDao.listAll();
    }

    /**
     * Export de tous les retours, classés par catégorie.
     *
     * @param format {@code xlsx} (défaut) ou {@code csv}
     * @return le fichier en pièce jointe
     */
    @GET
    @Path("/export")
    @Produces({XLSX_MEDIA_TYPE, CSV_MEDIA_TYPE})
    @Audited(value = "feedback.export", entityType = "user_feedback")
    public Response export(@QueryParam("format") @DefaultValue("xlsx") String format) {
        checkIsNationalAdmin();
        List<UserFeedback> feedbacks = userFeedbackDao.listAll();
        String baseName = "retours-utilisateurs_" + LocalDate.now();
        if ("csv".equals(format.toLowerCase(Locale.ROOT))) {
            return attachment(FeedbackExport.toCsv(feedbacks), CSV_MEDIA_TYPE, baseName + ".csv");
        }
        return attachment(FeedbackExport.toXlsx(feedbacks), XLSX_MEDIA_TYPE, baseName + ".xlsx");
    }

    /**
     * Capture d'écran jointe à un retour.
     *
     * @param feedbackId identifiant du retour
     * @return l'image PNG, ou 404 si le retour n'en a pas
     */
    @GET
    @Path("/{feedbackId}/screenshot")
    @Produces("image/png")
    public Response screenshot(@PathParam("feedbackId") UUID feedbackId) {
        checkIsNationalAdmin();
        return userFeedbackDao.getScreenshot(feedbackId)
                .map(this::wrapAsStreamingOutput)
                .map(Response::ok)
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND))
                .build();
    }

    private static Response attachment(Object content, String mediaType, String fileName) {
        return Response.ok(content)
                .type(mediaType)
                .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                .build();
    }
}
