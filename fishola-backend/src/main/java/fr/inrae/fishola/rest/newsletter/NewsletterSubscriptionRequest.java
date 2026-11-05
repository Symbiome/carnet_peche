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

/**
 * Formulaire d'inscription à la newsletter de la page d'accueil publique (#313).
 */
public class NewsletterSubscriptionRequest {

    /** E-mail, obligatoire. */
    public String email;

    /** Code du département (ex. {@code "74"}), optionnel. */
    public String departmentCode;

    /** Case de consentement RGPD, non pré-cochée : l'inscription est refusée sans elle. */
    public boolean consent;

    public NewsletterSubscriptionRequest() {
    }

    public NewsletterSubscriptionRequest(String email, String departmentCode, boolean consent) {
        this.email = email;
        this.departmentCode = departmentCode;
        this.consent = consent;
    }
}
