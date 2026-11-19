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
/**
 * Espèces recherchées d'une sortie (#305) : liste déroulante avec recherche, choix
 * multiple avec retrait, « Autre » en texte libre et « Toutes espèces ».
 *
 * La sortie est créée par l'API pour chaque test, avec une espèce déjà choisie.
 * Gardé derrière `Cypress.env('liveBackend')`, comme les autres parcours qui
 * demandent un backend seedé (cf. navigation.cypress.js).
 */
const apiUrl = () => Cypress.env("apiUrl");

const createTripWithSpecies = (speciesName) =>
  cy.request(`${apiUrl()}/v1/referential/species`).then(({ body: species }) =>
    cy.request(`${apiUrl()}/v1/referential/techniques`).then(({ body: techniques }) =>
      cy.request(`${apiUrl()}/v1/referential/waterEntities/summary`).then(({ body: waterEntities }) => {
        const yesterday = new Date(Date.now() - 24 * 3600 * 1000).toISOString().slice(0, 10);
        const trip = {
          id: "trip-species-e2e",
          mode: "Afterwards",
          type: "Border",
          name: "Espèces recherchées e2e",
          waterEntityId: waterEntities[0].id,
          speciesIds: [species.find((s) => s.name == speciesName).id],
          otherSpecies: "",
          date: yesterday,
          startedAt: "08:00",
          finishedAt: "10:00",
          catchs: [],
          techniqueIds: [techniques[0].id],
          source: "web",
        };
        return cy.request("POST", `${apiUrl()}/v1/trips`, trip).then(({ body }) => body[trip.id]);
      })
    )
  );

const save = () => cy.get(".footer").contains("Enregistrer").click();

const expectedSpeciesOnTrip = () => cy.contains(".form-multi-values", /Espèces? recherchées?/).find("li");

describe("Espèces recherchées (#305)", () => {
  beforeEach(function () {
    if (!Cypress.env("liveBackend")) {
      this.skip();
    }
    cy.viewport(390, 844);
    cy.loginAngler();
    createTripWithSpecies("Truite fario").as("tripId");
  });

  it("recherche « bro », ajoute le brochet puis retire l'espèce déjà choisie", function () {
    cy.visit(`/#/trips/${this.tripId}/species`);
    cy.get(".selected-species li").should("have.length", 1).and("contain.text", "Truite fario");
    cy.get(".species-search-select input").type("bro");
    cy.get("#field-species option").eq(1).invoke("text").invoke("trim").should("eq", "Brochet");
    cy.get("#field-species").select("Brochet");
    cy.get(".selected-species li").should("have.length", 2);
    cy.contains(".selected-species li", "Truite fario").find(".remove-species").click();
    cy.get(".selected-species li").should("have.length", 1).and("contain.text", "Brochet");
    save();
    cy.location("hash").should("eq", `#/trips/${this.tripId}`);
    expectedSpeciesOnTrip().should("have.length", 1).and("contain.text", "Brochet");
  });

  it("« Toutes espèces » retire les espèces choisies, s'enregistre et s'affiche", function () {
    cy.visit(`/#/trips/${this.tripId}/species`);
    cy.contains("button", "Toutes espèces").click().should("have.attr", "aria-pressed", "true");
    cy.get(".selected-species").should("not.exist");
    save();
    expectedSpeciesOnTrip().should("have.length", 1).and("have.text", "Toutes espèces");

    cy.visit(`/#/trips/${this.tripId}/species`);
    cy.contains("button", "Toutes espèces").should("have.attr", "aria-pressed", "true");
    cy.get("#field-species").select("Brochet");
    cy.contains("button", "Toutes espèces").should("have.attr", "aria-pressed", "false");
    cy.get(".selected-species li").should("contain.text", "Brochet");
  });

  it("« Autre » garde son champ texte libre", function () {
    cy.visit(`/#/trips/${this.tripId}/species`);
    cy.get("#field-species").select("Autre ...");
    cy.get(".other-species input").type("Poisson mystère");
    save();
    expectedSpeciesOnTrip().should("contain.text", "Poisson mystère").and("contain.text", "Truite fario");
  });
});
