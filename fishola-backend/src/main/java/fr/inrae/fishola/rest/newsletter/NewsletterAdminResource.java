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

import fr.inrae.fishola.database.NewsletterSubscriptionDao;
import fr.inrae.fishola.entities.tables.pojos.FisholaAdmin;
import fr.inrae.fishola.entities.tables.pojos.NewsletterSubscription;
import fr.inrae.fishola.rest.AbstractCarnetResource;
import fr.inrae.fishola.rest.audit.Audited;
import fr.inrae.fishola.rest.department.Departments;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Inscrits à la newsletter (#313), réservés aux administrateurs : un
 * administrateur national voit tous les inscrits, un administrateur régional
 * ceux des départements de son périmètre. Les désinscrits n'apparaissent pas.
 */
@Path("/api/v1/admin/newsletter-subscriptions")
@Produces(MediaType.APPLICATION_JSON)
public class NewsletterAdminResource extends AbstractCarnetResource {

    private static final String CSV_SEPARATOR = ";";
    private static final String UTF8_BOM = "﻿";
    private static final List<String> CSV_HEADER = List.of(
            "email", "departement", "date_inscription", "date_confirmation", "statut", "lien_desinscription");
    private static final DateTimeFormatter CSV_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.of("Europe/Paris"));

    @Inject
    protected NewsletterSubscriptionDao newsletterSubscriptionDao;

    /** @return les inscrits du périmètre de l'administrateur, du plus récent au plus ancien */
    @GET
    public List<NewsletterSubscriber> list() {
        return listForCurrentAdmin().stream().map(this::toSubscriber).toList();
    }

    /**
     * Export CSV (séparateur « ; », UTF-8 avec BOM pour Excel) des inscrits du
     * périmètre, avec leur lien de désinscription personnel à reprendre dans
     * tout e-mail envoyé depuis un autre outil.
     */
    @GET
    @Path("/export")
    @Produces("text/csv; charset=UTF-8")
    @Audited(value = "newsletter.export", entityType = "newsletter_subscription")
    public Response export(@Context HttpServletRequest request) {
        Stream<String> header = Stream.of(String.join(CSV_SEPARATOR, CSV_HEADER));
        Stream<String> lines = listForCurrentAdmin().stream().map(subscription -> toCsvLine(subscription, request));
        String csv = UTF8_BOM + Stream.concat(header, lines).collect(Collectors.joining("\n", "", "\n"));
        String fileName = "inscrits-newsletter_%s.csv".formatted(LocalDate.now());
        return Response.ok(csv)
                .header("Content-Disposition", "attachment; filename=\"%s\"".formatted(fileName))
                .build();
    }

    protected List<NewsletterSubscription> listForCurrentAdmin() {
        FisholaAdmin admin = checkIsAdmin();
        Optional<Set<String>> perimeter = Boolean.TRUE.equals(admin.getIsNationalAdmin())
                ? Optional.empty()
                : Optional.of(adminDao.getAllowedDepartments(admin.getId()));
        return newsletterSubscriptionDao.listActive(perimeter);
    }

    protected NewsletterSubscriber toSubscriber(NewsletterSubscription subscription) {
        String departmentCode = subscription.getDepartmentCode();
        return new NewsletterSubscriber(
                subscription.getEmail(),
                departmentCode,
                departmentCode == null ? null : Departments.nameOf(departmentCode),
                subscription.getSubscribedOn(),
                subscription.getConfirmedOn());
    }

    protected String toCsvLine(NewsletterSubscription subscription, HttpServletRequest request) {
        OffsetDateTime confirmedOn = subscription.getConfirmedOn();
        return Stream.of(
                        subscription.getEmail(),
                        StringUtils.defaultString(subscription.getDepartmentCode()),
                        CSV_DATE_FORMAT.format(subscription.getSubscribedOn()),
                        confirmedOn == null ? "" : CSV_DATE_FORMAT.format(confirmedOn),
                        confirmedOn == null ? "en attente" : "confirmé",
                        NewsletterResource.unsubscribeUrl(config, subscription.getToken(), request))
                .map(this::csvCell)
                .collect(Collectors.joining(CSV_SEPARATOR));
    }

    /** Met entre guillemets une valeur contenant le séparateur, un guillemet ou un saut de ligne. */
    protected String csvCell(String value) {
        if (StringUtils.containsAny(value, CSV_SEPARATOR, "\"", "\n", "\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
