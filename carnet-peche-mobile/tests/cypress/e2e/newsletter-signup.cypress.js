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
 * E2e de l'encart newsletter de la page d'accueil publique (#313) : affiché
 * tant que l'inscription à l'app est fermée, il devient « Créer un compte » à
 * l'ouverture. Le backend est stubbé (`cy.intercept`) : visiteur non connecté,
 * état de l'inscription, référentiel des départements et inscription.
 */
describe("Encart newsletter de la page d'accueil (#313)", () => {
  function stubPublicBackend(registrationEnabled) {
    cy.intercept("GET", "**/v1/security/profile", { statusCode: 401 });
    cy.intercept("GET", "**/v1/security/registration", {
      statusCode: 200,
      body: { enabled: registrationEnabled },
    });
    cy.intercept("GET", "**/v1/referential/departments", {
      statusCode: 200,
      body: [
        { code: "73", name: "Savoie" },
        { code: "74", name: "Haute-Savoie" },
      ],
    });
  }

  it("inscriptions fermées : refuse sans consentement puis confirme l'envoi", () => {
    stubPublicBackend(false);
    cy.intercept("POST", "**/v1/newsletter/subscribe", { statusCode: 200, body: {} }).as("subscribe");
    cy.visit("/#/about");

    cy.contains(".newsletter-signup", "Être prévenu·e de l'ouverture").should("be.visible");
    cy.get("#field-newsletter-email").type("visiteur@exemple.fr");
    cy.get("#field-newsletter-department").select("74 - Haute-Savoie");
    cy.contains(".newsletter-signup button", "S'inscrire").click();
    cy.contains(".newsletter-consent-error", "Votre consentement est nécessaire").should("be.visible");

    cy.get(".newsletter-signup label[for=newsletter-consent]").first().click();
    cy.contains(".newsletter-signup button", "S'inscrire").click();
    cy.wait("@subscribe").its("request.body").should("deep.equal", {
      email: "visiteur@exemple.fr",
      departmentCode: "74",
      consent: true,
    });
    cy.contains(".newsletter-success", "visiteur@exemple.fr").scrollIntoView().should("be.visible");
  });

  it("affiche les erreurs renvoyées par le backend", () => {
    stubPublicBackend(false);
    cy.intercept("POST", "**/v1/newsletter/subscribe", {
      statusCode: 429,
      body: { email: "Trop de demandes, réessayez dans une heure" },
    });
    cy.visit("/#/about");

    cy.get("#field-newsletter-email").type("visiteur@exemple.fr");
    cy.get(".newsletter-signup label[for=newsletter-consent]").first().click();
    cy.contains(".newsletter-signup button", "S'inscrire").click();
    cy.contains(".newsletter-signup", "Trop de demandes, réessayez dans une heure").scrollIntoView().should("be.visible");
    cy.get(".newsletter-success").should("not.exist");
  });

  it("inscriptions ouvertes : l'encart devient « Créer un compte »", () => {
    stubPublicBackend(true);
    cy.visit("/#/about");

    cy.contains(".newsletter-signup", "Les inscriptions sont ouvertes").should("be.visible");
    cy.get("#field-newsletter-email").should("not.exist");
    cy.contains(".newsletter-signup button", "Créer un compte").click();
    cy.location("hash").should("eq", "#/register");
  });
});
