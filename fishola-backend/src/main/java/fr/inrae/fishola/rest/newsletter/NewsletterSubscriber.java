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

import java.time.OffsetDateTime;

/**
 * Inscrit à la newsletter (#313), tel que listé dans l'admin, sans son jeton.
 *
 * @param email          e-mail
 * @param departmentCode code du département, ou {@code null}
 * @param departmentName nom du département, ou {@code null}
 * @param subscribedOn   date de la dernière demande d'inscription
 * @param confirmedOn    date de confirmation, {@code null} tant que l'adresse n'est pas confirmée
 */
public record NewsletterSubscriber(String email,
                                   String departmentCode,
                                   String departmentName,
                                   OffsetDateTime subscribedOn,
                                   OffsetDateTime confirmedOn) {
}
