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
import { DayMoment, PersonalStatistics } from "@/pojos/BackendPojos";
import Helpers from "@/services/Helpers";
import moment from "moment";

/** Période de la page « Mes statistiques ». */
export type StatisticsPeriod = "30d" | "season";

/** Premier et dernier jour (inclus) d'une période. */
export interface StatisticsRange {
  from: Date;
  to: Date;
}

/** Une barre : un mois (saison) ou un jour (30 jours). */
export interface ActivityBar {
  key: string;
  shortLabel: string;
  longLabel: string;
  trips: number;
  catches: number;
  current: boolean;
}

/** Une case de la frise de régularité : une semaine (saison) ou un jour (30 jours). */
export interface RegularityCell {
  key: string;
  level: "none" | "low" | "high" | "current";
}

/** Une ligne libellé / valeur avec barre de progression (0 à 1). */
export interface ProgressRow {
  label: string;
  value: string;
  ratio: number;
  hint?: string;
}

/** Contenu de la page « Mes statistiques » pour une période. */
export interface StatisticsView {
  period: StatisticsPeriod;
  range: StatisticsRange;
  tripsCount: number;
  catchesCount: number;
  bars: ActivityBar[];
  peakBar?: ActivityBar;
  cells: RegularityCell[];
  activeCellsCount: number;
  elapsedCellsCount: number;
  rhythmLabel: string;
  topMonthsLabel: string;
  moments: ProgressRow[];
  species: ProgressRow[];
  speciesCount: number;
  quality: ProgressRow[];
}

const DAYS_IN_SHORT_PERIOD = 30;
const MAX_SPECIES_ROWS = 6;
const MOMENT_LABELS: [DayMoment, string][] = [
  ["DAWN", "Aube"],
  ["MORNING", "Matin"],
  ["AFTERNOON", "Après-midi"],
  ["EVENING", "Soir"],
];

/** Date en français ; moment n'embarque pas sa locale « fr » dans l'app. */
export function frenchDate(date: Date, options: Intl.DateTimeFormatOptions): string {
  return date.toLocaleDateString("fr-FR", options);
}

/** Jour local au format AAAA-MM-JJ attendu par l'API. */
export function toIsoDay(date: Date): string {
  return moment(date).format("YYYY-MM-DD");
}

/** Saison (année civile en cours) ou 30 derniers jours, aujourd'hui compris. */
export function statisticsRange(period: StatisticsPeriod, today: Date = new Date()): StatisticsRange {
  const day = moment(today).startOf("day");
  if (period === "season") {
    return {
      from: day.clone().startOf("year").toDate(),
      to: day.clone().endOf("year").startOf("day").toDate(),
    };
  }
  return { from: day.clone().subtract(DAYS_IN_SHORT_PERIOD - 1, "days").toDate(), to: day.toDate() };
}

/** Sorties et prises par jour (AAAA-MM-JJ) ; l'API envoie les dates en tableau. */
function activityByDay(stats: PersonalStatistics): Map<string, { trips: number; catches: number }> {
  const result = new Map<string, { trips: number; catches: number }>();
  stats.dailyActivity.forEach((activity) => {
    const day = Helpers.parseLocalDate(activity.day as unknown as number[]);
    result.set(toIsoDay(day), { trips: activity.tripsCount, catches: activity.catchesCount });
  });
  return result;
}

/** Barres de la période : 12 mois de la saison ou 30 jours. */
function buildBars(
  byDay: Map<string, { trips: number; catches: number }>,
  period: StatisticsPeriod,
  range: StatisticsRange,
  today: Date
): ActivityBar[] {
  const unit = period === "season" ? "month" : "day";
  const keyFormat = period === "season" ? "YYYY-MM" : "YYYY-MM-DD";
  const result: ActivityBar[] = [];
  const cursor = moment(range.from).startOf(unit);
  while (!cursor.isAfter(range.to)) {
    const date = cursor.toDate();
    result.push({
      key: cursor.format(keyFormat),
      shortLabel: period === "season"
        ? frenchDate(date, { month: "short" })
        : frenchDate(date, { day: "numeric", month: "short" }),
      longLabel: period === "season"
        ? "en " + frenchDate(date, { month: "long" })
        : "le " + frenchDate(date, { day: "numeric", month: "short" }),
      trips: 0,
      catches: 0,
      current: cursor.isSame(today, unit),
    });
    cursor.add(1, unit);
  }
  const barsByKey = new Map(result.map((bar) => [bar.key, bar]));
  byDay.forEach((activity, day) => {
    const bar = barsByKey.get(moment(day).format(keyFormat));
    if (bar) {
      bar.trips += activity.trips;
      bar.catches += activity.catches;
    }
  });
  return result;
}

/** Barre la plus chargée en prises, absente s'il n'y a aucune prise. */
function findPeak(bars: ActivityBar[]): ActivityBar | undefined {
  const result = bars.reduce<ActivityBar | undefined>(
    (best, bar) => (!best || bar.catches > best.catches ? bar : best),
    undefined
  );
  return result && result.catches > 0 ? result : undefined;
}

/**
 * Frise de régularité : semaines (du lundi au dimanche) de la saison, ou
 * jours de la période de 30 jours. Une case est active dès qu'une sortie y
 * est enregistrée ; la case du jour (ou de la semaine) en cours est signalée.
 */
function buildCells(
  byDay: Map<string, { trips: number; catches: number }>,
  period: StatisticsPeriod,
  range: StatisticsRange,
  today: Date
): { cells: RegularityCell[]; active: number; elapsed: number } {
  const unit = period === "season" ? "isoWeek" : "day";
  const step = period === "season" ? "week" : "day";
  const cells: RegularityCell[] = [];
  let active = 0;
  let elapsed = 0;
  const cursor = moment(range.from).startOf(unit);
  while (!cursor.isAfter(range.to)) {
    const end = cursor.clone().endOf(unit);
    let trips = 0;
    byDay.forEach((activity, day) => {
      if (moment(day).isBetween(cursor, end, "day", "[]")) {
        trips += activity.trips;
      }
    });
    if (!cursor.isAfter(today)) {
      elapsed += 1;
    }
    if (trips > 0) {
      active += 1;
    }
    const level = cursor.isSame(today, unit) ? "current" : trips >= 2 ? "high" : trips === 1 ? "low" : "none";
    cells.push({ key: cursor.format("YYYY-MM-DD"), level });
    cursor.add(1, step);
  }
  return { cells, active, elapsed };
}

/** Écart médian, en jours, entre deux jours de sortie consécutifs. */
export function medianGapInDays(days: string[]): number | undefined {
  const sorted = [...days].sort();
  const gaps: number[] = [];
  for (let index = 1; index < sorted.length; index++) {
    gaps.push(moment(sorted[index]).diff(moment(sorted[index - 1]), "days"));
  }
  if (gaps.length === 0) {
    return undefined;
  }
  gaps.sort((a, b) => a - b);
  const middle = Math.floor(gaps.length / 2);
  return gaps.length % 2 === 1 ? gaps[middle] : (gaps[middle - 1] + gaps[middle]) / 2;
}

/** « Tous les jours », « Toutes les 2 semaines », « Tous les 3 mois »… */
export function rhythmLabel(gapInDays: number | undefined): string {
  if (gapInDays === undefined) {
    return "Pas assez de sorties";
  }
  if (gapInDays < 6) {
    const days = Math.max(1, Math.round(gapInDays));
    return days === 1 ? "Tous les jours" : "Tous les " + days + " jours";
  }
  if (gapInDays < 25) {
    const weeks = Math.max(1, Math.round(gapInDays / 7));
    return weeks === 1 ? "Toutes les semaines" : "Toutes les " + weeks + " semaines";
  }
  const months = Math.round(gapInDays / 30);
  return months === 1 ? "Tous les mois" : "Tous les " + months + " mois";
}

/** Les deux mois aux plus nombreuses prises, dans l'ordre du calendrier. */
function topMonthsLabel(bars: ActivityBar[]): string {
  const top = bars
    .filter((bar) => bar.catches > 0)
    .sort((a, b) => b.catches - a.catches || a.key.localeCompare(b.key))
    .slice(0, 2)
    .sort((a, b) => a.key.localeCompare(b.key))
    .map((bar) => frenchDate(moment(bar.key, "YYYY-MM").toDate(), { month: "long" }));
  return top.length > 0 ? top.join(" et ") : "Aucune prise";
}

/** Nombre au singulier ou au pluriel : « 1 prise », « 3 prises ». */
export function plural(count: number, singular: string, pluralForm: string = singular + "s"): string {
  return count + " " + (count > 1 ? pluralForm : singular);
}

/** Moments de sortie, barres relatives au moment le plus fréquent. */
function buildMoments(stats: PersonalStatistics): ProgressRow[] {
  const counts = MOMENT_LABELS.map(([moment]) => stats.tripsPerMoment[moment] || 0);
  const max = Math.max(1, ...counts);
  return MOMENT_LABELS.map(([, label], index) => ({
    label,
    value: String(counts[index]),
    ratio: counts[index] / max,
  }));
}

/**
 * Espèces de la plus pêchée à la moins pêchée, barres relatives à la
 * première, part en pourcentage des prises ; au-delà de six, les moins
 * pêchées sont regroupées.
 */
function buildSpecies(stats: PersonalStatistics, speciesNames: Map<string, string>) {
  const entries = Object.entries(stats.catchesPerSpecies)
    .map(([id, count]) => ({ name: speciesNames.get(id) || "Espèce inconnue", count }))
    .sort((a, b) => b.count - a.count || a.name.localeCompare(b.name));
  let shown = entries;
  if (entries.length > MAX_SPECIES_ROWS) {
    const others = entries.slice(MAX_SPECIES_ROWS - 1);
    shown = entries.slice(0, MAX_SPECIES_ROWS - 1);
    shown.push({
      name: plural(others.length, "autre espèce", "autres espèces"),
      count: others.reduce((sum, entry) => sum + entry.count, 0),
    });
  }
  const max = Math.max(1, ...shown.map((entry) => entry.count));
  const total = Math.max(1, entries.reduce((sum, entry) => sum + entry.count, 0));
  const rows: ProgressRow[] = shown.map((entry) => ({
    label: entry.name,
    value: plural(entry.count, "prise") + " · " + Math.round((100 * entry.count) / total) + "%",
    ratio: entry.count / max,
  }));
  return { rows, count: entries.length };
}

/** Part « n sur total » : « 83% · 15 sur 18 », ou un tiret sans total. */
function shareRow(label: string, hint: string, count: number, total: number): ProgressRow {
  if (total === 0) {
    return { label, hint, value: "–", ratio: 0 };
  }
  return { label, hint, value: Math.round((100 * count) / total) + "% · " + count + " sur " + total, ratio: count / total };
}

/** Qualité des fiches : complètes, avec photo, saisies dans les 24 h. */
function buildQuality(stats: PersonalStatistics, tripsCount: number, catchesCount: number): ProgressRow[] {
  return [
    shareRow("Fiches complètes", "Espèce, taille, photo et lieu renseignés", stats.completeTripsCount, tripsCount),
    shareRow("Captures avec photo", "Une photo permet de vérifier l’espèce", stats.catchesWithPictureCount, catchesCount),
    shareRow("Saisies dans les 24h", "Les souvenirs sont plus précis le jour même",
      stats.tripsEnteredWithinADayCount, tripsCount),
  ];
}

/**
 * Calcule le contenu de la page « Mes statistiques » à partir des agrégats
 * de l'API.
 *
 * @param stats statistiques de la période, renvoyées par l'API
 * @param period période affichée
 * @param speciesNames nom des espèces par identifiant
 * @param today jour de référence, aujourd'hui par défaut
 */
export function buildStatisticsView(
  stats: PersonalStatistics,
  period: StatisticsPeriod,
  speciesNames: Map<string, string>,
  today: Date = new Date()
): StatisticsView {
  const range = statisticsRange(period, today);
  const byDay = activityByDay(stats);
  const bars = buildBars(byDay, period, range, today);
  const regularity = buildCells(byDay, period, range, today);
  const tripsCount = bars.reduce((sum, bar) => sum + bar.trips, 0);
  const catchesCount = bars.reduce((sum, bar) => sum + bar.catches, 0);
  const species = buildSpecies(stats, speciesNames);
  return {
    period,
    range,
    tripsCount,
    catchesCount,
    bars,
    peakBar: findPeak(bars),
    cells: regularity.cells,
    activeCellsCount: regularity.active,
    elapsedCellsCount: regularity.elapsed,
    rhythmLabel: rhythmLabel(medianGapInDays(Array.from(byDay.keys()))),
    topMonthsLabel: topMonthsLabel(bars),
    moments: buildMoments(stats),
    species: species.rows,
    speciesCount: species.count,
    quality: buildQuality(stats, tripsCount, catchesCount),
  };
}
