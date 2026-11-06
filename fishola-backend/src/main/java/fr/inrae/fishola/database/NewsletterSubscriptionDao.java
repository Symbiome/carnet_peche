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

import fr.inrae.fishola.entities.tables.pojos.NewsletterSubscription;
import jakarta.inject.Singleton;
import org.jooq.Condition;
import org.jooq.impl.DSL;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static fr.inrae.fishola.entities.Tables.NEWSLETTER_SUBSCRIPTION;

/**
 * Inscrits à la newsletter de la page d'accueil publique (#313), en double
 * opt-in : une inscription n'est confirmée qu'après le clic sur le lien reçu.
 */
@Singleton
public class NewsletterSubscriptionDao extends AbstractCarnetDao {

    /**
     * Enregistre une demande d'inscription. Une adresse déjà confirmée et non
     * désinscrite n'est pas modifiée ; sinon la demande repart de zéro (nouveau
     * jeton, confirmation à refaire), sans jamais créer de doublon.
     *
     * @param email          e-mail en minuscules, déjà validé
     * @param departmentCode département déclaré, ou {@code null}
     * @return le jeton à envoyer dans l'e-mail de confirmation, vide si
     * l'adresse est déjà inscrite et confirmée
     */
    public Optional<UUID> subscribe(String email, String departmentCode) {
        UUID token = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        return withContext(context -> context.insertInto(NEWSLETTER_SUBSCRIPTION)
                .set(NEWSLETTER_SUBSCRIPTION.EMAIL, email)
                .set(NEWSLETTER_SUBSCRIPTION.DEPARTMENT_CODE, departmentCode)
                .set(NEWSLETTER_SUBSCRIPTION.SUBSCRIBED_ON, now)
                .set(NEWSLETTER_SUBSCRIPTION.TOKEN, token)
                .onConflict(NEWSLETTER_SUBSCRIPTION.EMAIL)
                .doUpdate()
                .set(NEWSLETTER_SUBSCRIPTION.DEPARTMENT_CODE, departmentCode)
                .set(NEWSLETTER_SUBSCRIPTION.SUBSCRIBED_ON, now)
                .set(NEWSLETTER_SUBSCRIPTION.TOKEN, token)
                .setNull(NEWSLETTER_SUBSCRIPTION.CONFIRMED_ON)
                .setNull(NEWSLETTER_SUBSCRIPTION.UNSUBSCRIBED_ON)
                .where(NEWSLETTER_SUBSCRIPTION.CONFIRMED_ON.isNull()
                        .or(NEWSLETTER_SUBSCRIPTION.UNSUBSCRIBED_ON.isNotNull()))
                .returning(NEWSLETTER_SUBSCRIPTION.TOKEN)
                .fetchOptional(NEWSLETTER_SUBSCRIPTION.TOKEN));
    }

    /**
     * Confirme l'adresse portant ce jeton. Rejouer le lien n'a pas d'effet.
     *
     * @param token jeton reçu dans l'e-mail de confirmation
     * @return {@code true} si une inscription active porte ce jeton
     */
    public boolean confirm(UUID token) {
        return withContext(context -> context.update(NEWSLETTER_SUBSCRIPTION)
                .set(NEWSLETTER_SUBSCRIPTION.CONFIRMED_ON,
                        DSL.coalesce(NEWSLETTER_SUBSCRIPTION.CONFIRMED_ON, DSL.currentOffsetDateTime()))
                .where(NEWSLETTER_SUBSCRIPTION.TOKEN.eq(token))
                .and(NEWSLETTER_SUBSCRIPTION.UNSUBSCRIBED_ON.isNull())
                .execute()) > 0;
    }

    /**
     * Désinscrit l'adresse portant ce jeton. Rejouer le lien n'a pas d'effet.
     *
     * @param token jeton reçu dans un e-mail
     * @return {@code true} si une inscription porte ce jeton
     */
    public boolean unsubscribe(UUID token) {
        return withContext(context -> context.update(NEWSLETTER_SUBSCRIPTION)
                .set(NEWSLETTER_SUBSCRIPTION.UNSUBSCRIBED_ON,
                        DSL.coalesce(NEWSLETTER_SUBSCRIPTION.UNSUBSCRIBED_ON, DSL.currentOffsetDateTime()))
                .where(NEWSLETTER_SUBSCRIPTION.TOKEN.eq(token))
                .execute()) > 0;
    }

    /**
     * Inscrits non désinscrits, du plus récent au plus ancien.
     *
     * @param departmentCodes départements du périmètre de l'administrateur
     *                        régional ; vide pour un administrateur national,
     *                        qui voit aussi les inscrits sans département
     * @return les inscriptions, confirmées ou non
     */
    public List<NewsletterSubscription> listActive(Optional<Set<String>> departmentCodes) {
        Condition perimeter = departmentCodes
                .map(NEWSLETTER_SUBSCRIPTION.DEPARTMENT_CODE::in)
                .orElse(DSL.noCondition());
        return withContext(context -> context.selectFrom(NEWSLETTER_SUBSCRIPTION)
                .where(NEWSLETTER_SUBSCRIPTION.UNSUBSCRIBED_ON.isNull())
                .and(perimeter)
                .orderBy(NEWSLETTER_SUBSCRIPTION.SUBSCRIBED_ON.desc())
                .fetchInto(NewsletterSubscription.class));
    }
}
