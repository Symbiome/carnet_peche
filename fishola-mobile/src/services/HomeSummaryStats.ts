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
import moment from "moment";

export type SummaryPeriod = "30d" | "12m";

/** Une barre du résumé : un jour (30 jours) ou un mois (12 mois). */
export interface SummaryBucket {
  key: string;
  shortLabel: string;
  longLabel: string;
  trips: number;
  catches: number;
}

/** Nombre de prises d'une espèce sur la période. */
export interface SpeciesShare {
  name: string;
  count: number;
}

/** Contenu du bloc « Résumé » de l'accueil pour une période. */
export interface HomeSummary {
  period: SummaryPeriod;
  tripsCount: number;
  catchesCount: number;
  buckets: SummaryBucket[];
  peakBucket?: SummaryBucket;
  species: SpeciesShare[];
}

const DAYS_IN_SHORT_PERIOD = 30;
const MONTHS_IN_LONG_PERIOD = 12;

/**
 * Barres vides de la période, de la plus ancienne à la plus récente (celle
 * qui contient `today`).
 */
function emptyBuckets(period: SummaryPeriod, today: moment.Moment): SummaryBucket[] {
  const unit = period === "30d" ? "day" : "month";
  const size = period === "30d" ? DAYS_IN_SHORT_PERIOD : MONTHS_IN_LONG_PERIOD;
  const result: SummaryBucket[] = [];
  for (let offset = size - 1; offset >= 0; offset--) {
    const start = today.clone().startOf(unit).subtract(offset, unit);
    result.push({
      key: bucketKey(period, start),
      shortLabel: frenchDate(start, period === "30d" ? { day: "numeric", month: "short" } : { month: "short" }),
      longLabel: period === "30d"
        ? "le " + frenchDate(start, { day: "numeric", month: "short" })
        : "en " + frenchDate(start, { month: "long" }),
      trips: 0,
      catches: 0,
    });
  }
  return result;
}

/** Date en français ; moment n'embarque pas sa locale « fr » dans l'app. */
function frenchDate(date: moment.Moment, options: Intl.DateTimeFormatOptions): string {
  return date.toDate().toLocaleDateString("fr-FR", options);
}

/** Clé de la barre à laquelle appartient une date. */
function bucketKey(period: SummaryPeriod, date: moment.Moment): string {
  return period === "30d" ? date.format("YYYY-MM-DD") : date.format("YYYY-MM");
}

/** Barre la plus chargée en prises, absente s'il n'y a aucune prise. */
function findPeak(buckets: SummaryBucket[]): SummaryBucket | undefined {
  const result = buckets.reduce<SummaryBucket | undefined>(
    (best, bucket) => (!best || bucket.catches > best.catches ? bucket : best),
    undefined
  );
  return result && result.catches > 0 ? result : undefined;
}

/**
 * Prises par espèce des sorties de la période, de la plus pêchée à la moins
 * pêchée. Une capture en lot compte pour une prise.
 */
function speciesShares(markers: CatchMarker[], tripIds: Set<string>): SpeciesShare[] {
  const counts = new Map<string, number>();
  markers
    .filter((marker) => marker.id && marker.specieName && tripIds.has(marker.tripId))
    .forEach((marker) => {
      const name = marker.specieName as string;
      counts.set(name, (counts.get(name) || 0) + 1);
    });
  return Array.from(counts.entries())
    .map(([name, count]) => ({ name, count }))
    .sort((a, b) => b.count - a.count || a.name.localeCompare(b.name));
}

/**
 * Calcule le résumé de l'accueil : sorties et prises par barre, pic de prises
 * et espèces pêchées sur les 30 derniers jours ou les 12 derniers mois.
 *
 * @param trips sorties du pêcheur (date de début et nombre de prises)
 * @param markers une ligne par capture, pour l'espèce
 * @param period période affichée
 * @param today jour de référence, aujourd'hui par défaut
 */
export function buildHomeSummary(
  trips: TripLight[],
  markers: CatchMarker[],
  period: SummaryPeriod,
  today: Date = new Date()
): HomeSummary {
  const buckets = emptyBuckets(period, moment(today));
  const bucketsByKey = new Map(buckets.map((bucket) => [bucket.key, bucket]));
  const tripIds = new Set<string>();
  trips.forEach((trip) => {
    const bucket = bucketsByKey.get(bucketKey(period, moment(trip.date)));
    if (bucket) {
      bucket.trips += 1;
      bucket.catches += trip.catchsCount || 0;
      tripIds.add(trip.id);
    }
  });
  return {
    period,
    tripsCount: tripIds.size,
    catchesCount: buckets.reduce((sum, bucket) => sum + bucket.catches, 0),
    buckets,
    peakBucket: findPeak(buckets),
    species: speciesShares(markers, tripIds),
  };
}
