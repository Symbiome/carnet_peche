/*-
 * #%L
 * Carnet de pêche :: Mobile
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


/** Clé sous laquelle les versions précédentes stockaient le mot de passe. */
const LEGACY_PASSWORD_KEY = "latestPassword";

/**
 * Efface le mot de passe que les versions précédentes de l'app mémorisaient en
 * clair dans le localStorage (#123). À appeler à chaque démarrage : une
 * installation mise à jour ne doit pas garder la valeur jusqu'à la
 * déconnexion. L'e-mail (`latestEmail`) est conservé pour pré-remplir le
 * formulaire.
 *
 * @param storage stockage à nettoyer, le localStorage par défaut
 */
export function forgetStoredPassword(storage?: Storage): void {
  try {
    (storage ?? window.localStorage).removeItem(LEGACY_PASSWORD_KEY);
  } catch (e) {
    // Stockage indisponible (navigation privée) : rien à effacer.
  }
}
