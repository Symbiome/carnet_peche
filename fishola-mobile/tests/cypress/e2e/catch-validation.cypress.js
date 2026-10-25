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
 * Contrôles du formulaire de capture (#308) : un poids incohérent avec la taille
 * n'est qu'un avertissement ; le bandeau d'erreur distingue un champ obligatoire
 * manquant d'une valeur incorrecte.
 *
 * Une sortie est créée par l'API pour chaque test, et la saisie du poids activée
 * dans les paramètres du pêcheur. Gardé derrière `Cypress.env('liveBackend')`.
 */
const apiUrl = () => Cypress.env("apiUrl");

const createTrip = () =>
  cy.request(`${apiUrl()}/v1/referential/species`).then(({ body: species }) =>
    cy.request(`${apiUrl()}/v1/referential/techniques`).then(({ body: techniques }) =>
      cy.request(`${apiUrl()}/v1/referential/waterEntities/summary`).then(({ body: waterEntities }) => {
        const yesterday = new Date(Date.now() - 24 * 3600 * 1000).toISOString().slice(0, 10);
        const trip = {
          id: "catch-validation-e2e",
          mode: "Afterwards",
          type: "Border",
          name: "Contrôles de capture e2e",
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

const enableWeightPrompt = () =>
  cy.request(`${apiUrl()}/v1/security/settings`).then(({ body: settings }) =>
    cy.request("PUT", `${apiUrl()}/v1/security/settings`, { ...settings, promptWeight: true })
  );

const fillCatch = ({ size, weight, technique = true }) => {
  cy.get("#field-species").type("bro");
  cy.contains('[role="option"]', "Brochet").click();
  cy.get("#field-size").clear().type(String(size));
  cy.get("#field-weight").clear().type(String(weight));
  cy.get("#field-keep-false").check({ force: true });
  if (technique) {
    cy.get("#field-technique option").eq(1).then(($option) => cy.get("#field-technique").select($option.val()));
  }
};

const validate = () => cy.get(".footer").contains("Valider").click();

describe("Contrôles de la saisie d'une capture (#308)", () => {
  beforeEach(function () {
    if (!Cypress.env("liveBackend")) {
      this.skip();
    }
    cy.viewport(390, 844);
    cy.loginAngler();
    enableWeightPrompt();
    createTrip().then((tripId) => cy.visit(`/#/trips/${tripId}/catchs/NEW`));
    cy.get("#field-species", { timeout: 15000 });
  });

  it("enregistre une capture de 30 cm et 5 kg avec l'avertissement « Le poids n'est pas cohérent »", () => {
    fillCatch({ size: 30, weight: 5000 });
    cy.get(".form-input div.field-warning").should("be.visible")
      .invoke("text").invoke("trim").should("eq", "Le poids n'est pas cohérent");
    cy.get("#field-weight").should("have.class", "field-warning");
    validate();
    cy.location("hash").should("match", /^#\/trips\/[^/]+$/);
    cy.get(".preview-wrapper").should("have.length", 1);
  });

  it("refuse une capture sans technique de pêche : champs obligatoires", () => {
    fillCatch({ size: 30, weight: 200, technique: false });
    validate();
    cy.get(".toaster").should("contain.text", "Vous devez renseigner les champs obligatoires");
    cy.location("hash").should("match", /\/catchs\/NEW$/);
  });

  it("refuse un poids négatif sans parler de champs obligatoires", () => {
    fillCatch({ size: 30, weight: -5 });
    validate();
    cy.get(".toaster")
      .should("contain.text", "Certaines valeurs saisies sont incorrectes")
      .and("not.contain.text", "champs obligatoires");
    cy.contains(".form-input .field-error", "Le poids doit être strictement positif");
    cy.location("hash").should("match", /\/catchs\/NEW$/);
  });
});
