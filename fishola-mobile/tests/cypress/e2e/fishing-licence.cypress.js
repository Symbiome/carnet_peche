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
 * Carte de pêche présentable lors d'un contrôle (#331) : ajout d'une carte PDF
 * ou JPEG, ouverture en plein écran, puis réouverture sans réseau depuis la
 * copie gardée sur l'appareil.
 *
 * En dev (`npm run serve`), le visualiseur PDF intégré est désactivé (`.env`) :
 * on accepte alors le bouton « Ouvrir le PDF » à la place des pages rendues.
 * Gardé derrière `Cypress.env('liveBackend')`, comme les autres parcours qui
 * demandent un backend seedé (cf. navigation.cypress.js).
 */
const apiUrl = () => Cypress.env("apiUrl");
const E2E_PREFIX = "Carte e2e";

/** PDF d'une page, avec une table xref exacte pour que pdf.js le lise sans réparation. */
const minimalPdf = () => {
  const objects = [
    "<< /Type /Catalog /Pages 2 0 R >>",
    "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
    "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 200 100] >>",
  ];
  let body = "%PDF-1.4\n";
  const offsets = objects.map((object, index) => {
    const offset = body.length;
    body += `${index + 1} 0 obj\n${object}\nendobj\n`;
    return offset;
  });
  const xref = body.length;
  body += `xref\n0 ${objects.length + 1}\n0000000000 65535 f \n`;
  body += offsets.map((offset) => `${String(offset).padStart(10, "0")} 00000 n \n`).join("");
  body += `trailer\n<< /Size ${objects.length + 1} /Root 1 0 R >>\nstartxref\n${xref}\n%%EOF\n`;
  return Cypress.Buffer.from(body);
};

/** Petit JPEG dessiné par le navigateur de test. */
const minimalJpeg = () =>
  cy.window().then(
    (win) =>
      new Cypress.Promise((resolve) => {
        const canvas = win.document.createElement("canvas");
        canvas.width = 40;
        canvas.height = 20;
        canvas.getContext("2d").fillRect(0, 0, 40, 20);
        canvas.toBlob((blob) => blob.arrayBuffer().then((data) => resolve(Cypress.Buffer.from(data))), "image/jpeg");
      })
  );

const deleteE2eLicences = () =>
  cy.request(`${apiUrl()}/v1/licences/`).then(({ body: licences }) =>
    licences
      .filter((licence) => licence.name.startsWith(E2E_PREFIX))
      .forEach((licence) => cy.request("DELETE", `${apiUrl()}/v1/licences/${licence.id}`))
  );

const addLicence = (name, contents, fileName, mimeType) => {
  cy.visit("/#/licences/new");
  cy.get(".new-licence input:not([type])").type(name);
  cy.get(".new-licence input[type=file]").selectFile({ contents, fileName, mimeType }, { force: true });
  cy.contains(".save button", "Enregistrer").click();
  cy.contains("Une nouvelle carte de pêche a été ajoutée.");
};

const openLicence = (name) => {
  cy.visit("/#/profile/profile");
  cy.contains(".licence-item", name).find(".item-description").click();
  cy.contains("Chargement en cours").should("not.exist");
};

const shouldShowPdf = () =>
  cy.get(".fullscreen-pdf canvas, .licence-message button").should("be.visible");

const goOffline = () =>
  cy.intercept({ url: `${apiUrl()}/**` }, { forceNetworkError: true });

describe("Carte de pêche en plein écran (#331)", () => {
  beforeEach(function () {
    if (!Cypress.env("liveBackend")) {
      this.skip();
    }
    cy.viewport(1280, 800);
    cy.loginAngler();
    deleteE2eLicences();
  });

  it("affiche une carte PDF ajoutée par le pêcheur", () => {
    addLicence(`${E2E_PREFIX} PDF`, minimalPdf(), "carte.pdf", "application/pdf");
    openLicence(`${E2E_PREFIX} PDF`);
    shouldShowPdf();
  });

  it("affiche une carte JPEG ajoutée par le pêcheur", () => {
    minimalJpeg().then((jpeg) => addLicence(`${E2E_PREFIX} JPEG`, jpeg, "carte.jpg", "image/jpeg"));
    openLicence(`${E2E_PREFIX} JPEG`);
    cy.get(".fullscreen-img").should("be.visible").and(($img) => {
      expect($img[0].naturalWidth).to.be.greaterThan(0);
    });
  });

  it("rouvre sans réseau une carte déjà affichée", () => {
    addLicence(`${E2E_PREFIX} hors ligne`, minimalPdf(), "carte.pdf", "application/pdf");
    openLicence(`${E2E_PREFIX} hors ligne`);
    shouldShowPdf();
    cy.go("back");
    goOffline();
    cy.contains(".licence-item", `${E2E_PREFIX} hors ligne`).find(".item-description").click();
    shouldShowPdf();
  });
});
