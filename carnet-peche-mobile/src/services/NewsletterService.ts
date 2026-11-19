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
import { NewsletterSubscriptionRequest } from "@/pojos/BackendPojos";
import AbstractCarnetService from "@/services/AbstractCarnetService";

/** Département du référentiel, ex. `{ code: "74", name: "Haute-Savoie" }`. */
export interface DepartmentName {
  code: string;
  name: string;
}

/**
 * Inscription à la newsletter depuis la page d'accueil publique (#313), tant
 * que l'inscription à l'app est fermée.
 */
export default class NewsletterService extends AbstractCarnetService {
  /**
   * Enregistre l'inscription ; le backend envoie l'e-mail de confirmation.
   * Rejette avec `{ status, content }` : 400 et les erreurs par champ, 429 au-delà
   * de la limite de demandes.
   */
  static subscribe(request: NewsletterSubscriptionRequest): Promise<void> {
    return this.backendPost("/v1/newsletter/subscribe", request);
  }

  /** Référentiel des départements, pour le champ optionnel du formulaire. */
  static getDepartments(): Promise<DepartmentName[]> {
    return this.backendGet("/v1/referential/departments");
  }
}
