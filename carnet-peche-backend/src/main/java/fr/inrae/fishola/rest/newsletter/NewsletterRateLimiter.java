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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import fr.inrae.fishola.CarnetConfiguration;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limitation de débit du formulaire public d'inscription à la newsletter
 * (#313) : au plus {@code fishola.newsletter-subscriptions-per-ip-per-hour}
 * demandes par adresse IP sur une fenêtre d'une heure ouverte par la première
 * demande. Compteurs en mémoire : ils repartent à zéro au redémarrage.
 */
@Singleton
public class NewsletterRateLimiter {

    private static final Duration WINDOW = Duration.ofHours(1);
    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";

    @Inject
    protected CarnetConfiguration config;

    private final Cache<String, AtomicInteger> requestsPerIp = Caffeine.newBuilder()
            .expireAfterWrite(WINDOW)
            .maximumSize(100_000)
            .build();

    /**
     * Compte une demande et indique si elle reste sous la limite.
     *
     * @param request requête HTTP du visiteur
     * @return {@code true} si la demande est acceptée
     */
    public boolean tryAcquire(HttpServletRequest request) {
        String clientIp = clientIp(request);
        int count = requestsPerIp.get(clientIp, ip -> new AtomicInteger()).incrementAndGet();
        return count <= config.newsletterSubscriptionsPerIpPerHour();
    }

    /** Vide les compteurs (tests). */
    public void reset() {
        requestsPerIp.invalidateAll();
    }

    /**
     * Adresse du visiteur : derrière le reverse proxy, la première adresse de
     * {@code X-Forwarded-For}, sinon l'adresse de la connexion.
     */
    protected String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader(FORWARDED_FOR_HEADER);
        String firstForwarded = StringUtils.trimToNull(StringUtils.substringBefore(forwardedFor, ","));
        return firstForwarded != null ? firstForwarded : request.getRemoteAddr();
    }
}
