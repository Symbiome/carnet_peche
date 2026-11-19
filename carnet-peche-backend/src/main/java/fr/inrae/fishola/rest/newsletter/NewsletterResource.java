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

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import fr.inrae.fishola.CarnetConfiguration;
import fr.inrae.fishola.database.NewsletterSubscriptionDao;
import fr.inrae.fishola.mails.CarnetMail;
import fr.inrae.fishola.mails.MailService;
import fr.inrae.fishola.rest.AbstractCarnetResource;
import fr.inrae.fishola.rest.department.Departments;
import fr.inrae.fishola.rest.security.AccountRules;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;

import java.io.StringWriter;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Inscription à la newsletter depuis la page d'accueil publique (#313), en
 * double opt-in : inscription, confirmation par le lien reçu, désinscription
 * par le lien présent dans chaque e-mail. Endpoints publics, l'inscription est
 * limitée en débit par adresse IP.
 */
@Path("/api/v1/newsletter")
@Produces(MediaType.APPLICATION_JSON)
public class NewsletterResource extends AbstractCarnetResource {

    @Inject
    protected NewsletterSubscriptionDao newsletterSubscriptionDao;

    @Inject
    protected NewsletterRateLimiter rateLimiter;

    @Inject
    protected MailService mailService;

    /**
     * Enregistre une inscription et envoie l'e-mail de confirmation. La réponse
     * est la même qu'une adresse soit nouvelle ou déjà inscrite, pour ne pas
     * révéler qui est inscrit.
     *
     * @param bean    formulaire
     * @param request requête HTTP (adresse IP, URL des liens)
     * @return 200, 400 avec les erreurs par champ, ou 429 au-delà de la limite
     */
    @POST
    @Path("/subscribe")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response subscribe(NewsletterSubscriptionRequest bean, @Context HttpServletRequest request) {
        if (bean == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        String email = StringUtils.trimToEmpty(bean.email).toLowerCase();
        String departmentCode = StringUtils.trimToNull(bean.departmentCode);
        Map<String, String> validationErrors = validate(email, departmentCode, bean.consent);
        if (!validationErrors.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity(validationErrors).build();
        }
        if (!rateLimiter.tryAcquire(request)) {
            return Response.status(Response.Status.TOO_MANY_REQUESTS)
                    .entity(Map.of("email", "Trop de demandes, réessayez dans une heure"))
                    .build();
        }
        Optional<UUID> token = newsletterSubscriptionDao.subscribe(email, departmentCode);
        token.ifPresent(value -> sendConfirmationMail(email, value, request));
        return Response.ok().build();
    }

    /**
     * Contrôle le formulaire.
     *
     * @return les messages d'erreur par champ, vide si le formulaire est valide
     */
    protected Map<String, String> validate(String email, String departmentCode, boolean consent) {
        Map<String, String> validationErrors = new LinkedHashMap<>();
        if (StringUtils.isEmpty(email)) {
            validationErrors.put("email", "L'e-mail est obligatoire");
        } else if (!AccountRules.isValidEmail(email)) {
            validationErrors.put("email", "Le format n'est pas correct");
        }
        if (departmentCode != null && !Departments.isValidCode(departmentCode)) {
            validationErrors.put("departmentCode", "Département inconnu");
        }
        if (!consent) {
            validationErrors.put("consent", "Votre consentement est nécessaire pour vous inscrire");
        }
        return validationErrors;
    }

    /** Envoie l'e-mail de double opt-in, avec le lien de désinscription. */
    protected void sendConfirmationMail(String email, UUID token, HttpServletRequest request) {
        String confirmUrl = "%s?t=%s".formatted(config.getApiUrl("/api/v1/newsletter/confirm", request), token);
        String unsubscribeUrl = unsubscribeUrl(config, token, request);
        CarnetMail mail = mailService.newMailFromTemplate(
                        "emails/newsletter-confirmation.html",
                        "confirmLink", confirmUrl,
                        "unsubscribeLink", unsubscribeUrl)
                .addTos(email)
                .subject("Carnet de pêche - Confirmez votre inscription")
                .build();
        mailService.sendMail(mail);
    }

    /**
     * Lien de désinscription d'un inscrit, repris dans l'export CSV pour les
     * e-mails envoyés depuis un autre outil.
     */
    public static String unsubscribeUrl(CarnetConfiguration config, UUID token, HttpServletRequest request) {
        return "%s?t=%s".formatted(config.getApiUrl("/api/v1/newsletter/unsubscribe", request), token);
    }

    /**
     * Lien de confirmation reçu par e-mail : redirige vers une page de succès
     * ou d'échec.
     */
    @GET
    @Path("/confirm")
    public Response confirm(@Context HttpServletRequest request, @QueryParam("t") String token) {
        boolean confirmed = parseToken(token).map(newsletterSubscriptionDao::confirm).orElse(false);
        String page = confirmed ? "/api/newsletter_confirm_ok.html" : "/api/newsletter_confirm_fail.html";
        return Response.temporaryRedirect(URI.create(config.getApiUrl(page, request))).build();
    }

    /**
     * Lien de désinscription : affiche une page de confirmation. La
     * désinscription n'a lieu qu'au clic, pour qu'un anti-spam qui ouvre les
     * liens ne désinscrive personne.
     */
    @GET
    @Path("/unsubscribe")
    @Produces(MediaType.TEXT_HTML)
    public Response unsubscribeForm(@QueryParam("t") String token) {
        String safeToken = parseToken(token).map(UUID::toString).orElse("");
        return Response.ok(render("emails/newsletter-unsubscribe.html", Map.of("token", safeToken))).build();
    }

    /** Désinscrit l'adresse portant le jeton, depuis la page de confirmation. */
    @POST
    @Path("/unsubscribe")
    @Produces(MediaType.TEXT_HTML)
    public Response unsubscribe(@QueryParam("t") String token) {
        boolean unsubscribed = parseToken(token).map(newsletterSubscriptionDao::unsubscribe).orElse(false);
        return Response.ok(render("emails/newsletter-unsubscribed.html", Map.of("unsubscribed", unsubscribed)))
                .build();
    }

    protected Optional<UUID> parseToken(String token) {
        try {
            return Optional.of(UUID.fromString(StringUtils.trimToEmpty(token)));
        } catch (IllegalArgumentException eee) {
            return Optional.empty();
        }
    }

    protected String render(String templatePath, Map<String, Object> parameters) {
        Mustache mustache = new DefaultMustacheFactory().compile(templatePath);
        StringWriter stringWriter = new StringWriter();
        mustache.execute(stringWriter, parameters);
        return stringWriter.toString();
    }
}
