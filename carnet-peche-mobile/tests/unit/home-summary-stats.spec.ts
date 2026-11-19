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

import { CatchMarker, TripLight } from "@/pojos/BackendPojos";
import { buildHomeSummary } from "@/services/HomeSummaryStats";

const TODAY = new Date(2026, 9, 21);

function trip(id: string, date: Date, catchsCount: number): TripLight {
  return { id, name: id, waterEntityId: "w", date, endDate: date, durationInSeconds: 3600, catchsCount, modifiable: true };
}

function marker(tripId: string, specieName?: string, id = tripId + specieName): CatchMarker {
  return {
    id, date: TODAY, tripId, tripName: tripId, specieName, waterEntityName: "w",
    latitude: 0, longitude: 0, size: 0, weight: 0, hasValidCoordinates: true,
  };
}

// Bloc « Résumé » de l'accueil (maquette Master content).
describe("buildHomeSummary", () => {
  const trips = [
    trip("oct", new Date(2026, 9, 15), 3),
    trip("avr", new Date(2026, 3, 2), 5),
    trip("avr2", new Date(2026, 3, 20), 2),
    trip("old", new Date(2025, 9, 31), 9),
  ];

  it("répartit sorties et prises sur les 12 derniers mois, mois courant en dernier", () => {
    const summary = buildHomeSummary(trips, [], "12m", TODAY);
    expect(summary.buckets).toHaveLength(12);
    expect(summary.buckets[0].key).toBe("2025-11");
    expect(summary.buckets[11].key).toBe("2026-10");
    expect(summary.tripsCount).toBe(3);
    expect(summary.catchesCount).toBe(10);
    expect(summary.peakBucket?.key).toBe("2026-04");
    expect(summary.peakBucket?.longLabel).toBe("en avril");
  });

  it("ne garde que les 30 derniers jours, aujourd'hui inclus", () => {
    const summary = buildHomeSummary(trips, [], "30d", TODAY);
    expect(summary.buckets).toHaveLength(30);
    expect(summary.buckets[29].key).toBe("2026-10-21");
    expect(summary.tripsCount).toBe(1);
    expect(summary.peakBucket?.longLabel).toBe("le 15 oct.");
  });

  it("classe les espèces des seules sorties de la période", () => {
    const markers = [
      marker("oct", "Perche", "c1"),
      marker("avr", "Truite fario", "c2"),
      marker("avr2", "Truite fario", "c3"),
      marker("old", "Brochet", "c4"),
      marker("avr", undefined, undefined as unknown as string),
    ];
    const summary = buildHomeSummary(trips, markers, "12m", TODAY);
    expect(summary.species).toEqual([
      { name: "Truite fario", count: 2 },
      { name: "Perche", count: 1 },
    ]);
  });

  it("n'annonce pas de pic sans prise", () => {
    const summary = buildHomeSummary([trip("vide", new Date(2026, 9, 1), 0)], [], "12m", TODAY);
    expect(summary.tripsCount).toBe(1);
    expect(summary.peakBucket).toBeUndefined();
  });
});
