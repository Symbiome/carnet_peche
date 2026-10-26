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


/** Position d'une sortie sur la carte. */
export interface TripPosition {
  tripId: string;
  latitude: number;
  longitude: number;
}

/**
 * Sortie médiane en latitude, chaque sortie comptant une fois quel que soit
 * son nombre de prises. Sert de centre par défaut à la carte « Mes sorties » :
 * contrairement au centre de l'emprise, c'est toujours un lieu de pêche réel,
 * même quand les sorties sont réparties sur deux zones éloignées. Pour un
 * nombre pair de sorties, on retient la plus au sud des deux médianes.
 */
export function medianLatitudeTrip<T extends TripPosition>(positions: T[]): T | undefined {
  const firstPerTrip = new Map<string, T>();
  positions.forEach((position) => {
    if (!firstPerTrip.has(position.tripId)) {
      firstPerTrip.set(position.tripId, position);
    }
  });
  const sorted = [...firstPerTrip.values()].sort((a, b) => a.latitude - b.latitude);
  return sorted[Math.floor((sorted.length - 1) / 2)];
}
