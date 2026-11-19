package fr.inrae.fishola.database;

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

import fr.inrae.fishola.entities.tables.pojos.UserFeedback;
import fr.inrae.fishola.rest.feedback.Feedback;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static fr.inrae.fishola.entities.Tables.USER_FEEDBACK;
import static fr.inrae.fishola.entities.Tables.USER_FEEDBACK_SCREENSHOT;

/**
 * Retours utilisateurs envoyés depuis l'app pêcheur ou l'administration,
 * conservés avec leur éventuelle capture d'écran pour les administrateurs nationaux.
 */
@Singleton
public class UserFeedbackDao extends AbstractCarnetDao {

    public static final String DEFAULT_SOURCE = "Application pêcheur";

    /**
     * Enregistre un retour reçu et sa capture d'écran éventuelle.
     *
     * @param feedback   retour envoyé par le frontend
     * @param screenshot capture d'écran convertie en PNG, si jointe
     */
    public void insert(Feedback feedback, Optional<byte[]> screenshot) {
        UUID feedbackId = insertFeedback(feedback, screenshot.isPresent());
        screenshot.ifPresent(content -> withContextNoResult(context -> context.insertInto(USER_FEEDBACK_SCREENSHOT)
                .set(USER_FEEDBACK_SCREENSHOT.FEEDBACK_ID, feedbackId)
                .set(USER_FEEDBACK_SCREENSHOT.CONTENT, content)
                .execute()));
    }

    /**
     * @param feedbackId identifiant du retour
     * @return la capture d'écran PNG du retour, vide s'il n'en a pas
     */
    public Optional<byte[]> getScreenshot(UUID feedbackId) {
        return withContext(context -> context.select(USER_FEEDBACK_SCREENSHOT.CONTENT)
                .from(USER_FEEDBACK_SCREENSHOT)
                .where(USER_FEEDBACK_SCREENSHOT.FEEDBACK_ID.eq(feedbackId))
                .fetchOptional(USER_FEEDBACK_SCREENSHOT.CONTENT));
    }

    private UUID insertFeedback(Feedback feedback, boolean hasScreenshot) {
        return withContext(context -> context.insertInto(USER_FEEDBACK)
                .set(USER_FEEDBACK.SOURCE, feedback.source().orElse(DEFAULT_SOURCE))
                .set(USER_FEEDBACK.CATEGORY, feedback.category())
                .set(USER_FEEDBACK.MODULE, feedback.module().orElse(null))
                .set(USER_FEEDBACK.EMAIL, feedback.email().orElse(null))
                .set(USER_FEEDBACK.DESCRIPTION, feedback.description().orElse(null))
                .set(USER_FEEDBACK.HAS_SCREENSHOT, hasScreenshot)
                .set(USER_FEEDBACK.LOCATION, feedback.location().orElse(null))
                .set(USER_FEEDBACK.FRONTEND_VERSION, feedback.frontendVersion().orElse(null))
                .set(USER_FEEDBACK.BACKEND_VERSION, feedback.backendVersion().orElse(null))
                .set(USER_FEEDBACK.BROWSER, feedback.browser().orElse(null))
                .set(USER_FEEDBACK.OS, feedback.os().orElse(null))
                .set(USER_FEEDBACK.PLATFORM, feedback.platform().orElse(null))
                .set(USER_FEEDBACK.DEVICE, feedback.device().orElse(null))
                .set(USER_FEEDBACK.SCREEN_RESOLUTION, feedback.screenResolution().orElse(null))
                .set(USER_FEEDBACK.DISPLAY_SIZE, feedback.displaySize().orElse(null))
                .set(USER_FEEDBACK.LOCALE, feedback.locale().orElse(null))
                .returning(USER_FEEDBACK.ID)
                .fetchOne(USER_FEEDBACK.ID));
    }

    /** @return tous les retours, classés par catégorie puis du plus récent au plus ancien */
    public List<UserFeedback> listAll() {
        return withContext(context -> context.selectFrom(USER_FEEDBACK)
                .orderBy(USER_FEEDBACK.CATEGORY, USER_FEEDBACK.RECEIVED_ON.desc())
                .fetchInto(UserFeedback.class));
    }
}
