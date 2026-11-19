/*-
 * #%L
 * Carnet de pêche :: Mobile
 * %%
 * Copyright (C) 2019 - 2024 INRAE - UMR CARRTEL
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
import {
  LicenceFromClientBean,
  LicenceResponseBean,
} from "@/pojos/BackendPojos";
import StoredLicenceFile from "@/pojos/StoredLicenceFile";
import AbstractCarnetService from "@/services/AbstractCarnetService";
import Constants from "./Constants";

const LICENCES_URI = "/v1/licences/";
const LICENCE_FILE_TIMEOUT_MS = 20000;

/**
 * Cartes de pêche du pêcheur. La liste et les fichiers sont gardés sur
 * l'appareil (#331) : le pêcheur doit pouvoir présenter sa carte lors d'un
 * contrôle, souvent au bord de l'eau et sans réseau.
 */
export default class FishingLicenceService extends AbstractCarnetService {
  constructor() {
    super();
  }

  /**
   * Liste des cartes, depuis le serveur ou, sans réseau, depuis la dernière
   * liste reçue. En ligne, télécharge les fichiers pas encore gardés sur
   * l'appareil et oublie ceux des cartes supprimées.
   */
  static getAllLicences(): Promise<LicenceResponseBean[]> {
    return this.backendGetOrOfflineStorage(LICENCES_URI).then((licences: any) => {
      if (!licences.offlineMarker) {
        this.syncStoredLicenceFiles(licences).catch((error) =>
          console.warn("Cartes de pêche non gardées hors ligne", error)
        );
      }
      return licences;
    });
  }

  /**
   * Fichier d'une carte : la copie gardée sur l'appareil s'il y en a une
   * (rafraîchie en arrière-plan), sinon le serveur. Rejette avec un message
   * lisible par le pêcheur.
   */
  static async getLicenceFile(licenceId: string): Promise<Blob> {
    const stored = await this.readStoredLicenceFile(licenceId);
    if (stored) {
      this.fetchAndStoreLicenceFile(licenceId).catch((error) =>
        console.warn("Carte de pêche non rafraîchie", error)
      );
      return new Blob([stored.data], { type: stored.mimeType });
    }
    try {
      return await this.fetchAndStoreLicenceFile(licenceId);
    } catch (error) {
      throw typeof error === "number"
        ? `Le serveur n'a pas pu fournir la carte de pêche (erreur ${error}).`
        : "La carte de pêche n'est pas encore enregistrée sur cet appareil. "
          + "Ouvrez-la une première fois avec une connexion internet.";
    }
  }

  static async getLicence(licenceId: string): Promise<LicenceResponseBean> {
    return this.backendGet(`/v1/licences/${licenceId}`);
  }

  static postLicence(licence: LicenceFromClientBean): Promise<void> {
    return this.backendPost(LICENCES_URI, licence);
  }

  static putLicence(licence: LicenceFromClientBean, id: string): Promise<void> {
    return this.backendPut(LICENCES_URI + id, licence).then((result) => {
      this.forgetStoredLicenceFile(id);
      return result;
    });
  }

  static deleteLicence(licenceId: string): Promise<void> {
    return this.backendDelete(LICENCES_URI + licenceId).then((result) => {
      this.forgetStoredLicenceFile(licenceId);
      return result;
    });
  }

  /** Efface les cartes gardées sur l'appareil (déconnexion). */
  static clearOfflineCopies(): Promise<void> {
    this.deleteFromOfflineStorage(LICENCES_URI);
    return this.getDatabase().licenceFiles.clear().catch((error) =>
      console.warn("Cartes de pêche hors ligne non effacées", error)
    );
  }

  private static async syncStoredLicenceFiles(licences: LicenceResponseBean[]): Promise<void> {
    const table = this.getDatabase().licenceFiles;
    const currentIds = new Set(licences.map((licence) => licence.id));
    const storedIds = (await table.toCollection().primaryKeys()) as string[];
    await table.bulkDelete(storedIds.filter((id) => !currentIds.has(id)));
    for (const licence of licences) {
      if (!storedIds.includes(licence.id)) {
        await this.fetchAndStoreLicenceFile(licence.id);
      }
    }
  }

  private static async readStoredLicenceFile(licenceId: string): Promise<StoredLicenceFile | undefined> {
    try {
      return await this.getDatabase().licenceFiles.get(licenceId);
    } catch (error) {
      console.warn("Stockage local indisponible pour les cartes de pêche", error);
      return undefined;
    }
  }

  private static forgetStoredLicenceFile(licenceId: string) {
    this.getDatabase().licenceFiles.delete(licenceId).catch((error) =>
      console.warn("Carte de pêche hors ligne non effacée", error)
    );
  }

  private static async fetchAndStoreLicenceFile(licenceId: string): Promise<Blob> {
    const blob = await this.fetchLicenceFile(licenceId);
    try {
      await this.getDatabase().licenceFiles.put({
        licenceId,
        data: await blob.arrayBuffer(),
        mimeType: blob.type,
        storedAt: Date.now(),
      });
    } catch (error) {
      console.warn("Carte de pêche non gardée hors ligne", error);
    }
    return blob;
  }

  /** Rejette avec le statut HTTP (nombre) ou, sans réponse du serveur, un message. */
  private static fetchLicenceFile(licenceId: string): Promise<Blob> {
    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest();
      xhr.open("GET", Constants.apiUrl(`/v1/licences/${licenceId}/file`), true);
      xhr.withCredentials = true;
      xhr.responseType = "blob";
      xhr.timeout = LICENCE_FILE_TIMEOUT_MS;
      xhr.onload = () => (xhr.status == 200 ? resolve(xhr.response) : reject(xhr.status));
      xhr.onerror = () => reject("Impossible de contacter le serveur");
      xhr.ontimeout = () => reject("Le serveur ne répond pas");
      xhr.send();
    });
  }
}
