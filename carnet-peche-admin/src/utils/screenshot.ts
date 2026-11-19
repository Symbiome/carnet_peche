/*-
 * #%L
 * Carnet de pêche :: Admin
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

/** Plus grand côté, en pixels, d'une capture jointe à un retour. */
export const SCREENSHOT_MAX_SIDE = 1600;

/**
 * Dimensions d'une image réduite pour que son plus grand côté ne dépasse pas
 * `maxSide`, proportions conservées ; une image plus petite est gardée telle quelle.
 */
export function fitWithin(width: number, height: number, maxSide: number): { width: number; height: number } {
  const ratio = Math.min(1, maxSide / Math.max(width, height));
  return { width: Math.round(width * ratio), height: Math.round(height * ratio) };
}

/**
 * Lit une image choisie par l'utilisateur et la renvoie en data URL JPEG
 * réduite (`SCREENSHOT_MAX_SIDE`), pour qu'une photo de téléphone reste
 * légère à envoyer. Rejette si le navigateur ne sait pas décoder le fichier.
 */
export function resizeScreenshot(file: File): Promise<string> {
  return new Promise<string>((resolve, reject) => {
    const objectUrl = URL.createObjectURL(file);
    const image = new Image();
    image.onload = () => {
      const size = fitWithin(image.naturalWidth, image.naturalHeight, SCREENSHOT_MAX_SIDE);
      const canvas = document.createElement("canvas");
      canvas.width = size.width;
      canvas.height = size.height;
      const context = canvas.getContext("2d");
      URL.revokeObjectURL(objectUrl);
      if (!context) {
        reject(new Error("Canvas indisponible"));
        return;
      }
      context.drawImage(image, 0, 0, size.width, size.height);
      resolve(canvas.toDataURL("image/jpeg", 0.85));
    };
    image.onerror = () => {
      URL.revokeObjectURL(objectUrl);
      reject(new Error("Image illisible"));
    };
    image.src = objectUrl;
  });
}
