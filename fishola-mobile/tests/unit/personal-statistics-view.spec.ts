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

import { PersonalStatistics } from "@/pojos/BackendPojos";
import {
  buildStatisticsView,
  medianGapInDays,
  rhythmLabel,
  statisticsRange,
  toIsoDay,
} from "@/services/PersonalStatisticsView";

// Mercredi 21 octobre 2026.
const TODAY = new Date(2026, 9, 21);

function day(month: number, dayOfMonth: number, tripsCount: number, catchesCount: number) {
  return { day: [2026, month, dayOfMonth] as unknown as Date, tripsCount, catchesCount };
}

function stats(overrides: Partial<PersonalStatistics> = {}): PersonalStatistics {
  return {
    from: [2026, 1, 1] as unknown as Date,
    to: [2026, 12, 31] as unknown as Date,
    dailyActivity: [day(4, 2, 1, 5), day(4, 20, 2, 4), day(5, 3, 1, 6), day(10, 15, 1, 1)],
    tripsPerMoment: { DAWN: 3, MORNING: 1, EVENING: 1 },
    catchesPerSpecies: { truite: 10, perche: 4, gardon: 2 },
    completeTripsCount: 4,
    catchesWithPictureCount: 8,
    tripsEnteredWithinADayCount: 0,
    waterEntities: [],
    ...overrides,
  };
}

const NAMES = new Map([["truite", "Truite fario"], ["perche", "Perche"], ["gardon", "Gardon"]]);

// Page « Mes statistiques » (maquette Figma « Statistiques »).
describe("buildStatisticsView", () => {
  it("borne la saison à l'année civile et les 30 jours à aujourd'hui inclus", () => {
    const season = statisticsRange("season", TODAY);
    expect([toIsoDay(season.from), toIsoDay(season.to)]).toEqual(["2026-01-01", "2026-12-31"]);
    const recent = statisticsRange("30d", TODAY);
    expect([toIsoDay(recent.from), toIsoDay(recent.to)]).toEqual(["2026-09-22", "2026-10-21"]);
  });

  it("répartit la saison en 12 mois, mois courant signalé, pic de prises", () => {
    const view = buildStatisticsView(stats(), "season", NAMES, TODAY);
    expect(view.bars).toHaveLength(12);
    expect(view.bars.map((bar) => bar.trips)).toEqual([0, 0, 0, 3, 1, 0, 0, 0, 0, 1, 0, 0]);
    expect(view.bars.filter((bar) => bar.current).map((bar) => bar.key)).toEqual(["2026-10"]);
    expect(view.tripsCount).toBe(5);
    expect(view.catchesCount).toBe(16);
    expect(view.peakBar?.longLabel).toBe("en avril");
    expect(view.topMonthsLabel).toBe("avril et mai");
  });

  it("compte les semaines actives parmi les semaines écoulées", () => {
    const view = buildStatisticsView(stats(), "season", NAMES, TODAY);
    expect(view.cells).toHaveLength(53);
    expect(view.activeCellsCount).toBe(4);
    expect(view.elapsedCellsCount).toBe(43);
    expect(view.cells.filter((cell) => cell.level === "current")).toHaveLength(1);
    expect(view.cells.filter((cell) => cell.level === "high")).toHaveLength(1);
  });

  it("passe aux jours sur la période de 30 jours", () => {
    const view = buildStatisticsView(stats(), "30d", NAMES, TODAY);
    expect(view.bars).toHaveLength(30);
    expect(view.cells).toHaveLength(30);
    expect(view.tripsCount).toBe(1);
    expect(view.activeCellsCount).toBe(1);
    expect(view.elapsedCellsCount).toBe(30);
    expect(view.peakBar?.longLabel).toBe("le 15 oct.");
  });

  it("donne les moments de sortie relativement au plus fréquent", () => {
    const view = buildStatisticsView(stats(), "season", NAMES, TODAY);
    expect(view.moments.map((row) => [row.label, row.value, row.ratio])).toEqual([
      ["Aube", "3", 1],
      ["Matin", "1", 1 / 3],
      ["Après-midi", "0", 0],
      ["Soir", "1", 1 / 3],
    ]);
  });

  it("classe les espèces et regroupe les moins pêchées au-delà de six", () => {
    const view = buildStatisticsView(stats(), "season", NAMES, TODAY);
    expect(view.speciesCount).toBe(3);
    expect(view.species.map((row) => [row.label, row.value])).toEqual([
      ["Truite fario", "10 prises · 63%"],
      ["Perche", "4 prises · 25%"],
      ["Gardon", "2 prises · 13%"],
    ]);
    const many = { a: 9, b: 8, c: 7, d: 6, e: 5, f: 1, g: 1 };
    const grouped = buildStatisticsView(stats({ catchesPerSpecies: many }), "season", new Map(), TODAY);
    expect(grouped.speciesCount).toBe(7);
    expect(grouped.species).toHaveLength(6);
    expect(grouped.species[5].label).toBe("2 autres espèces");
    expect(grouped.species[5].value).toBe("2 prises · 5%");
  });

  it("exprime la qualité en part des sorties ou des prises", () => {
    const view = buildStatisticsView(stats(), "season", NAMES, TODAY);
    expect(view.quality.map((row) => row.value)).toEqual(["80% · 4 sur 5", "50% · 8 sur 16", "0% · 0 sur 5"]);
    const empty = buildStatisticsView(stats({ dailyActivity: [] }), "season", NAMES, TODAY);
    expect(empty.quality[0].value).toBe("–");
    expect(empty.rhythmLabel).toBe("Pas assez de sorties");
  });
});

describe("rhythmLabel", () => {
  it("prend l'écart médian entre jours de sortie", () => {
    expect(medianGapInDays(["2026-04-01", "2026-04-15", "2026-04-29", "2026-06-01"])).toBe(14);
    expect(medianGapInDays(["2026-04-01"])).toBeUndefined();
  });

  it("l'exprime en jours, semaines ou mois", () => {
    expect(rhythmLabel(1)).toBe("Tous les jours");
    expect(rhythmLabel(3)).toBe("Tous les 3 jours");
    expect(rhythmLabel(7)).toBe("Toutes les semaines");
    expect(rhythmLabel(14)).toBe("Toutes les 2 semaines");
    expect(rhythmLabel(31)).toBe("Tous les mois");
    expect(rhythmLabel(95)).toBe("Tous les 3 mois");
  });
});
