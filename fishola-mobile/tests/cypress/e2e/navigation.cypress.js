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
/* eslint-disable no-undef */
/* eslint-disable no-undef */
/**
 * Navigation de l'app pêcheur (#301) : « Carte » n'affiche que la carte,
 * « Mes prises » la liste des sorties, et « Résumé de mon activité » réunit le
 * tableau de bord personnel et les statistiques du réseau. Les anciennes routes
 * redirigent vers les nouvelles.
 *
 * Gardé derrière `Cypress.env('liveBackend')`, comme les autres parcours qui
 * demandent un backend seedé (cf. offline-revalidation.cypress.js).
 */
describe("Navigation (#301)", () => {
  beforeEach(function () {
    if (!Cypress.env("liveBackend")) {
      this.skip();
    }
    cy.viewport(390, 844);
    cy.loginAngler();
  });

  it("« Carte » affiche la carte seule, sans onglet Liste", () => {
    cy.visit("/#/home");
    cy.contains(".bottom-nav .nav-item", "Carte").click();
    cy.location("hash").should("eq", "#/map");
    cy.get(".my-trips-page").should("exist");
    cy.get(".my-trips-wrapper").should("not.exist");
    cy.get(".main-tabs").should("not.exist");
    cy.contains("h1", "Mes Sorties").should("not.exist");
    cy.contains(".bottom-nav .nav-item", "Carte").should("have.attr", "aria-current", "page");
  });

  it("« Mes prises » affiche la liste des sorties avec la recherche", () => {
    cy.visit("/#/home");
    cy.contains(".bottom-nav .nav-item", "Mes prises").click();
    cy.location("hash").should("eq", "#/trips");
    cy.get(".my-trips-wrapper").should("be.visible");
    cy.get(".my-trips-search input").should("exist");
    cy.get(".main-tabs").should("not.exist");
    cy.contains(".bottom-nav .nav-item", "Mes prises").should("have.attr", "aria-current", "page");
  });

  it("« Résumé de mon activité » réunit Mes données et les statistiques du réseau", () => {
    cy.visit("/#/account");
    cy.contains(".account-row", "Résumé de mon activité").click();
    cy.location("hash").should("match", /^#\/activity-summary/);
    cy.contains("h1", "Mes données").should("be.visible");
    cy.contains("h1", "Statistiques du réseau").should("exist");
    // Même export que « Mes données » : pas de second lien dans la section réseau.
    cy.get(".network-section #export-button").should("not.exist");
  });

  it("l'année des statistiques du réseau ne pilote pas celle de « Mes données »", () => {
    // Sans donnée réseau sur l'année en cours, la section réseau se replie sur
    // l'année précédente : elle l'écrit sous sa propre clé, pas sous currentYear.
    cy.visit("/#/activity-summary");
    cy.location("hash", { timeout: 10000 }).should("contain", "networkYear=");
    cy.location("hash").should("not.contain", "currentYear=");
  });

  it("« Statistiques du réseau » reste une page à part", () => {
    cy.visit("/#/account");
    cy.contains(".account-row", "Statistiques du réseau").click();
    cy.location("hash").should("match", /^#\/dashboard-global\/dashboard/);
    cy.contains("h1", "Statistiques").should("be.visible");
    cy.contains("h1", "Mes données").should("not.exist");
  });

  it("redirige les anciennes routes", () => {
    cy.visit("/#/my-trips/map");
    cy.location("hash").should("eq", "#/map");
    cy.visit("/#/my-trips/list");
    cy.location("hash").should("eq", "#/trips");
    cy.visit("/#/dashboard-personal/dashboard");
    cy.location("hash").should("match", /^#\/activity-summary/);
  });
});
