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


import { medianLatitudeTrip } from "@/services/MapFraming";
import { describe, expect, it } from "vitest";

const at = (tripId: string, latitude: number, longitude = 6) => ({ tripId, latitude, longitude });

describe("medianLatitudeTrip", () => {
  it("retient une sortie réelle même avec deux zones éloignées", () => {
    const positions = [at("annecy-1", 45.9), at("annecy-2", 45.95), at("corse", 42.1)];
    expect(medianLatitudeTrip(positions)?.tripId).toBe("annecy-1");
  });

  it("compte chaque sortie une seule fois, quel que soit son nombre de prises", () => {
    const positions = [at("corse", 42.1), at("corse", 42.1), at("corse", 42.1), at("annecy", 45.9), at("leman", 46.4)];
    expect(medianLatitudeTrip(positions)?.tripId).toBe("annecy");
  });

  it("prend la plus au sud des deux médianes pour un nombre pair de sorties", () => {
    expect(medianLatitudeTrip([at("nord", 46), at("sud", 44)])?.tripId).toBe("sud");
  });

  it("ne renvoie rien sans sortie", () => {
    expect(medianLatitudeTrip([])).toBeUndefined();
  });
});
