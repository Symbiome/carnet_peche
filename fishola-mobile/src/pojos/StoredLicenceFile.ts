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


/**
 * Fichier d'une carte de pêche gardé sur l'appareil (#331), pour la présenter
 * lors d'un contrôle même sans réseau. Comme les tuiles (CachedTile), le
 * contenu est stocké en ArrayBuffer plutôt qu'en Blob.
 */
export default interface StoredLicenceFile {
  /** Identifiant de la carte de pêche (clé Dexie). */
  licenceId: string;
  /** Contenu brut du fichier (PDF, PNG ou JPEG). */
  data: ArrayBuffer;
  /** Type MIME renvoyé par le serveur. */
  mimeType: string;
  /** Horodatage de l'enregistrement (epoch ms). */
  storedAt: number;
}
