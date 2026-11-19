package fr.inrae.fishola.rest.feedback;

/*-
 * #%L
 * Carnet de pêche :: Backend
 * %%
 * Copyright (C) 2019 - 2021 INRAE - UMR CARRTEL
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
import fr.inrae.fishola.exceptions.CarnetTechnicalException;
import fr.inrae.fishola.exceptions.NotAuthenticatedException;
import fr.inrae.fishola.mails.CarnetMail;
import fr.inrae.fishola.mails.CarnetMailAttachment;
import fr.inrae.fishola.mails.ImmutableCarnetMail;
import fr.inrae.fishola.mails.ImmutableCarnetMailAttachment;
import fr.inrae.fishola.mails.MailService;
import fr.inrae.fishola.rest.AbstractCarnetResource;
import fr.inrae.fishola.rest.ImageHelper;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@Path("/api/v1/feedback")
public class FeedbackResource extends AbstractCarnetResource {

    private static final String ANONYMOUS_CONTACT_CATEGORY = "CONTACT";

    @Inject
    protected MailService mailService;

    @Inject
    protected UserFeedbackDao userFeedbackDao;

    @PUT
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void newFeedback(Feedback bean) {

        checkFeedbackAllowed(bean);

        log.infof("Réception d'un feedback %s", bean);

        Optional<byte[]> screenshotBytes = Optional.empty();

        if (bean.screenshot().isPresent()) {
            // tokenize the data
            String[] contentSplitted = bean.screenshot().get().split(",");
            String base64Image = contentSplitted[1];

            byte[] bytes = Base64.getDecoder().decode(base64Image);
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            try {
                BufferedImage image = ImageIO.read(bis);
                bis.close();

                byte[] pngBytes = ImageHelper.imageToBytes(image, "png", .6f);

                screenshotBytes = Optional.of(pngBytes);

            } catch (IOException ioe) {
                throw new CarnetTechnicalException("Impossible de lire l'image", ioe);
            }
        }

        ImmutableFeedback feedback = ImmutableFeedback.builder()
                .fromInstance(bean)
                .backendVersion(config.getFullVersion())
                .build();

        userFeedbackDao.insert(feedback, screenshotBytes);

        CarnetMail carnetMail = toCarnetMail(feedback, screenshotBytes);
        mailService.sendMail(carnetMail);

    }

    /**
     * Réserve le feedback aux pêcheurs connectés et aux comptes staff. Sans authentification,
     * seul le formulaire de contact public (catégorie CONTACT, sans capture d'écran) passe :
     * aucune donnée Base64 anonyme n'atteint ainsi le décodeur (CVE-2026-114938).
     *
     * @param bean le feedback reçu
     * @throws NotAuthenticatedException si l'appelant n'est pas authentifié et que le feedback
     *                                   n'est pas un simple message de contact
     */
    protected void checkFeedbackAllowed(Feedback bean) throws NotAuthenticatedException {
        boolean isAnonymousContact = ANONYMOUS_CONTACT_CATEGORY.equals(bean.category())
                && bean.screenshot().isEmpty();
        if (!isAnonymousContact && !isAuthenticatedAnglerOrStaff()) {
            throw new NotAuthenticatedException("Il faut d'abord s'authentifier pour envoyer un retour");
        }
    }

    /**
     * Indique si la requête porte un token pêcheur valide (app mobile) ou un token staff valide
     * (back-office).
     *
     * @return true si l'un des deux cookies d'authentification est valide
     */
    protected boolean isAuthenticatedAnglerOrStaff() {
        return isValidCredential(userToken, this::getUserIdOrRenew)
                || isValidCredential(adminToken, this::checkIsStaff);
    }

    /**
     * Exécute une vérification d'authentification sans propager son échec.
     *
     * @param token               le token du cookie concerné, éventuellement absent
     * @param authenticationCheck la vérification à exécuter sur ce token
     * @return true si le token est présent et que la vérification réussit
     */
    private static boolean isValidCredential(String token, Supplier<?> authenticationCheck) {
        if (token == null) {
            return false;
        }
        try {
            authenticationCheck.get();
            return true;
        } catch (RuntimeException invalidCredential) {
            return false;
        }
    }

    protected CarnetMail toCarnetMail(Feedback feedback, Optional<byte[]> screenshotBytes) {
        String description = feedback.description().orElse("Pas de description");
        String screenshot = screenshotBytes.map(file -> "L'utilisateur a fournit une capture d'écran (cf PJ)")
                .orElse("L'utilisation n'a pas fournit de capture d'écran");

        Map<String, Object> args = Map.of(
                "feedback", feedback,
                "descriptionText", description,
                "screenshotText", screenshot,
                "moduleText", feedback.module().orElse("Non précisé"),
                "sourceText", feedback.source().orElse("Application pêcheur")
                                         );

        ImmutableCarnetMail.Builder builder = mailService.newMailFromTemplate(
                "emails/new-feedback.html",
                args);
        builder.subject(feedback.source().map(source -> "[" + source + "] ").orElse("")
                + "Nouveau feedback : " + feedback.category()
                + feedback.module().map(module -> " · " + module).orElse(""));
        builder.addTos(config.feedbackMailTo());
        if (screenshotBytes.isPresent()) {
            byte[] bytes = screenshotBytes.get();
            CarnetMailAttachment attachment = ImmutableCarnetMailAttachment.builder()
                    .bytes(bytes)
                    .name(String.format("feedback-%s.png", feedback.id().toString()))
                    .type(com.google.common.net.MediaType.PNG)
                    .build();
            builder.addAttachments(attachment);
        }
        CarnetMail result = builder.build();
        return result;
    }

}
