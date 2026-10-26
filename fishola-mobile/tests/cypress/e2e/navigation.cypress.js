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
 * « Mes prises » la liste des sorties, et « Statistiques » (anciennement
 * « Résumé de mon activité ») le tableau de bord personnel. Les anciennes routes redirigent vers les nouvelles.
 *
 * « Compte » simplifié (#307) : plus de « Communauté » ni de « Le projet »,
 * repris dans le menu ; les statistiques du réseau se masquent dans « Statistiques ».
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
    // Présentation du premier lancement (#194) : elle recouvre l'accueil.
    cy.on("window:before:load", (win) => win.localStorage.setItem("carnet-onboarding-done", "true"));
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

  it("« Statistiques » du Compte affiche les statistiques personnelles, sans celles du réseau", () => {
    cy.visit("/#/account");
    cy.contains(".account-row", "Statistiques").click();
    cy.location("hash").should("match", /^#\/activity-summary/);
    cy.contains("h1", "Mes statistiques").should("be.visible");
    cy.get(".year-toggle").should("be.visible");
    cy.get(".network-toggle").should("not.exist");
  });

  it("« Compte » n'affiche que Mon espace et Aide (#307)", () => {
    cy.visit("/#/account");
    cy.get(".account-section h2").then(($titles) => {
      expect([...$titles].map((t) => t.textContent.trim())).to.deep.eq(["Mon espace", "Aide"]);
    });
    cy.get(".account-row .row-label").then(($labels) => {
      expect([...$labels].map((l) => l.textContent.trim())).to.deep.eq([
        "Mon profil",
        "Statistiques",
        "Mes badges",
        "Données et confidentialité",
        "Mode hors ligne",
        "Paramètres",
        "Contact",
        "A propos",
        "Revoir la présentation",
        "Déconnexion",
      ]);
    });
  });

  it("le bandeau de gauche donne accès aux Statistiques personnelles et au projet (#307)", () => {
    cy.viewport(1280, 800);
    cy.visit("/#/home");
    cy.contains(".menu .item", "Communauté").should("not.exist");
    cy.get(".menu .item:visible").filter(":contains('Statistiques')").should("have.length", 1).click();
    cy.location("hash").should("match", /^#\/activity-summary/);
    ["Pourquoi carnet de pêche ?", "Qui porte le projet ?"].forEach((label) => {
      cy.contains(".menu .item", label).should("be.visible");
    });
    cy.contains(".menu .item", "Qui porte le projet ?").click();
    cy.location("hash").should("eq", "#/credits");
    cy.contains(".menu .item", "Pourquoi carnet de pêche ?").click();
    cy.location("hash").should("eq", "#/about");
  });

  it("sur mobile, pas de menu à côté des initiales du pêcheur", () => {
    cy.visit("/#/home");
    cy.get(".home-avatar").should("be.visible");
    cy.get(".home-header .icon-menu").should("not.exist");
    cy.visit("/#/badges");
    cy.get(".header .header-avatar").should("be.visible");
    cy.get(".header .icon-menu").should("not.exist");
  });

  it("« Statistiques » masque les statistiques du réseau et retient le choix (#307)", () => {
    cy.visit("/#/dashboard-global/dashboard");
    cy.contains("h1", "Statistiques").should("be.visible");
    cy.get(".network-hidden").should("not.exist");
    cy.get(".network-toggle img").click();
    cy.get(".network-hidden").should("be.visible");
    cy.reload();
    cy.get(".network-hidden").should("be.visible");
    cy.get(".network-toggle img").click();
    cy.get(".network-hidden").should("not.exist");
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
