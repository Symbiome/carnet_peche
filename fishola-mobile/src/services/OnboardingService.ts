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


/** Clé locale : l'onboarding est mémorisé par appareil (arbitrage #194). */
const DONE_KEY = "carnet-onboarding-done";

/**
 * Mémorise, sur l'appareil, que le pêcheur a vu ou passé l'onboarding (#194).
 * Le localStorage suffit hors ligne et n'a pas besoin du backend.
 */
export default class OnboardingService {
  /** Vrai si l'onboarding a été terminé ou passé sur cet appareil. */
  static isDone(storage?: Storage): boolean {
    try {
      return (storage ?? window.localStorage).getItem(DONE_KEY) === "true";
    } catch (e) {
      // Stockage indisponible : ne pas imposer l'onboarding à chaque écran.
      return true;
    }
  }

  /** Ferme l'onboarding définitivement (fin du parcours ou « Passer »). */
  static markDone(storage?: Storage): void {
    try {
      (storage ?? window.localStorage).setItem(DONE_KEY, "true");
    } catch (e) {
      console.warn("Fin de l'onboarding non mémorisée", e);
    }
  }

  /**
   * Réaffiche l'onboarding au prochain passage sur l'accueil (depuis
   * « Compte › Revoir la présentation »).
   */
  static replay(storage?: Storage): void {
    try {
      (storage ?? window.localStorage).removeItem(DONE_KEY);
    } catch (e) {
      console.warn("Relance de l'onboarding impossible", e);
    }
  }
}
