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
 * Champ « Espèce » à autocomplétion du formulaire de capture (#311) : un seul
 * champ, filtré dès les premières lettres, où l'on choisit l'espèce d'un geste.
 *
 * Une sortie est créée par l'API pour chaque test. Gardé derrière
 * `Cypress.env('liveBackend')`.
 */
const apiUrl = () => Cypress.env("apiUrl");

const createTrip = () =>
  cy.request(`${apiUrl()}/v1/referential/species`).then(({ body: species }) =>
    cy.request(`${apiUrl()}/v1/referential/techniques`).then(({ body: techniques }) =>
      cy.request(`${apiUrl()}/v1/referential/waterEntities/summary`).then(({ body: waterEntities }) => {
        const yesterday = new Date(Date.now() - 24 * 3600 * 1000).toISOString().slice(0, 10);
        const trip = {
          id: "species-combobox-e2e",
          mode: "Afterwards",
          type: "Border",
          name: "Espèce à autocomplétion e2e",
          waterEntityId: waterEntities[0].id,
          speciesIds: [species.find((s) => s.name == "Brochet").id],
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

const fillRequiredFields = () => {
  cy.get("#field-size").clear().type("60");
  cy.get("#field-keep-false").check({ force: true });
  cy.get("#field-technique option").eq(1).then(($option) => cy.get("#field-technique").select($option.val()));
};

const validate = () => cy.get(".footer").contains("Valider").click();

describe("Champ « Espèce » à autocomplétion (#311)", () => {
  beforeEach(function () {
    if (!Cypress.env("liveBackend")) {
      this.skip();
    }
    cy.viewport(390, 844);
    cy.loginAngler();
    createTrip().as("tripId").then((tripId) => cy.visit(`/#/trips/${tripId}/catchs/NEW`));
    cy.get("#field-species", { timeout: 15000 });
  });

  it("n'a qu'un champ pour l'espèce", () => {
    cy.get("#field-species").should("have.attr", "role", "combobox");
    cy.get("#field-speciesSearch").should("not.exist");
    cy.get(".edit-catch-form select#field-species").should("not.exist");
  });

  it("tape « bro », choisit « Brochet » et enregistre ; la capture rouverte affiche Brochet", function () {
    cy.get("#field-species").type("bro");
    cy.get('[role="option"]').then(($options) => {
      const names = [...$options].map((option) => option.textContent.trim());
      expect(names[0]).to.eq("Brochet");
      expect(names).not.to.include("Perche");
      expect(names[names.length - 1]).to.eq("Autre ...");
    });
    cy.contains('[role="option"]', "Brochet").click();
    cy.get('[role="option"]').should("not.exist");
    cy.get("#field-species").should("have.value", "Brochet");
    fillRequiredFields();
    validate();
    cy.location("hash").should("match", /^#\/trips\/[^/]+$/);
    cy.get(".preview-wrapper").should("have.length", 1).click();
    cy.get("#field-species", { timeout: 15000 }).should("have.value", "Brochet");
  });

  it("trouve le brochet par son nom scientifique (« esox »)", function () {
    cy.request(`${apiUrl()}/v1/referential/species`).then(({ body: species }) => {
      const pike = species.find((s) => s.name == "Brochet");
      if (!pike.scientificName) {
        this.skip();
      }
      cy.get("#field-species").type(pike.scientificName.split(" ")[0].toLowerCase());
      cy.contains('[role="option"]', "Brochet").should("be.visible");
    });
  });

  it("choisit « Autre » et renseigne le champ « Si autre »", () => {
    cy.get("#field-species").type("poisson-lune");
    cy.get('[role="option"]').should("have.length", 1).and("contain.text", "Autre");
    cy.contains('[role="option"]', "Autre").click();
    cy.get("#field-otherSpecies").should("be.visible").type("Poisson-lune");
    fillRequiredFields();
    validate();
    cy.location("hash").should("match", /^#\/trips\/[^/]+$/);
    cy.get(".preview-wrapper").should("have.length", 1);
  });

  it("se pilote au clavier : flèches, Entrée et Échap", () => {
    cy.get("#field-species").type("bro{enter}");
    cy.get("#field-species").should("have.value", "Brochet");
    cy.get("#field-species").type("{downarrow}");
    cy.get('[role="listbox"]').should("be.visible");
    cy.get("#field-species").type("{esc}");
    cy.get('[role="listbox"]').should("not.exist");
    cy.get("#field-species").should("have.value", "Brochet");
  });
});
