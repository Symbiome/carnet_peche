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
import moment from "moment";
import TripDates from "@/services/TripDates";

/** Sorties de plusieurs jours (#237). */
describe("TripDates", () => {
  const carpSession = {
    date: new Date(2026, 8, 12),
    startedAt: "18:00",
    endDate: "2026-09-14",
    finishedAt: "10:00",
  };

  it("calcule la durée d'une sortie de plusieurs jours sur les dates", () => {
    expect(TripDates.durationSeconds(carpSession)).toBe(40 * 3600);
    expect(TripDates.isMultiDay(carpSession)).toBe(true);
    expect(TripDates.formatMultiDayPeriod(carpSession)).toBe("du 12/09 18:00 au 14/09 10:00");
  });

  it("garde la règle « fin avant début → lendemain » sans date de fin", () => {
    const legacy = { date: new Date(2026, 8, 12), startedAt: "22:00", finishedAt: "02:00" };
    expect(TripDates.durationSeconds(legacy)).toBe(4 * 3600);
    expect(TripDates.endIsoDate(legacy)).toBe("2026-09-13");
  });

  it("n'affiche pas de période pour une sortie d'une journée", () => {
    const oneDay = { ...carpSession, endDate: "2026-09-12", finishedAt: "21:00" };
    expect(TripDates.isMultiDay(oneDay)).toBe(false);
    expect(TripDates.formatMultiDayPeriod(oneDay)).toBe("");
  });

  it("accepte une capture datée dans la sortie et refuse celle qui en sort", () => {
    expect(TripDates.isCatchWithinTrip(carpSession, { caughtOn: "2026-09-13", caughtAt: "03:00" })).toBe(true);
    expect(TripDates.isCatchWithinTrip(carpSession, { caughtOn: "2026-09-14", caughtAt: "12:00" })).toBe(false);
    expect(TripDates.isCatchWithinTrip(carpSession, { caughtOn: "2026-09-12", caughtAt: "17:00" })).toBe(false);
  });

  it("rattache une capture non datée au premier jour où elle tombe dans la sortie", () => {
    const night = TripDates.catchMoment(carpSession, { caughtAt: "03:00" });
    expect(night!.format("YYYY-MM-DD HH:mm")).toBe("2026-09-13 03:00");
    expect(TripDates.countCatchsOutsideTrip(carpSession, [{ caughtAt: "03:00" }, {}])).toBe(0);
  });

  it("borne une sortie en cours à maintenant", () => {
    const running = { date: new Date(2026, 8, 12), startedAt: "18:00" };
    const now = moment("2026-09-13T08:00");
    expect(TripDates.isCatchWithinTrip(running, { caughtOn: "2026-09-13", caughtAt: "07:00" }, now)).toBe(true);
    expect(TripDates.isCatchWithinTrip(running, { caughtOn: "2026-09-13", caughtAt: "09:00" }, now)).toBe(false);
  });

  // #310 : le début et la fin ont des secondes (sortie en direct), l'heure de capture non.
  describe("compare les captures à la minute", () => {
    const liveTrip = {
      date: new Date(2026, 8, 12),
      startedAt: "09:18:34",
      endDate: "2026-09-12",
      finishedAt: "17:30:12",
    };

    it("accepte une capture à la minute de début ou de fin de la sortie", () => {
      expect(TripDates.isCatchWithinTrip(liveTrip, { caughtAt: "09:18" })).toBe(true);
      expect(TripDates.isCatchWithinTrip(liveTrip, { caughtAt: "17:30" })).toBe(true);
      expect(TripDates.isCatchWithinTrip(liveTrip, { caughtOn: "2026-09-12", caughtAt: "09:18" })).toBe(true);
    });

    it("refuse une capture la minute d'avant le début ou d'après la fin", () => {
      expect(TripDates.isCatchWithinTrip(liveTrip, { caughtAt: "09:17" })).toBe(false);
      expect(TripDates.isCatchWithinTrip(liveTrip, { caughtAt: "17:31" })).toBe(false);
    });

    it("accepte une capture saisie pendant la première minute d'une sortie en direct", () => {
      const running = { date: new Date(2026, 8, 12), startedAt: "09:18:34" };
      const now = moment("2026-09-12T09:18:50");
      expect(TripDates.isCatchWithinTrip(running, { caughtAt: "09:18" }, now)).toBe(true);
      expect(TripDates.isCatchWithinTrip(running, { caughtAt: "09:19" }, now)).toBe(false);
      expect(now.format("HH:mm:ss")).toBe("09:18:50");
    });

    it("applique la même règle aux bornes d'une sortie de plusieurs jours", () => {
      const night = { date: new Date(2026, 8, 12), startedAt: "22:10:45", endDate: "2026-09-13",
        finishedAt: "06:05:30" };
      expect(TripDates.catchMoment(night, { caughtAt: "22:10" })!.format("YYYY-MM-DD HH:mm")).toBe("2026-09-12 22:10");
      expect(TripDates.catchMoment(night, { caughtAt: "06:05" })!.format("YYYY-MM-DD HH:mm")).toBe("2026-09-13 06:05");
      expect(TripDates.isCatchWithinTrip(night, { caughtOn: "2026-09-13", caughtAt: "06:06" })).toBe(false);
      expect(TripDates.isCatchWithinTrip(night, { caughtOn: "2026-09-12", caughtAt: "22:09" })).toBe(false);
    });

    it("ne compte pas comme hors sortie les captures aux minutes de début et de fin", () => {
      const catchs = [{ caughtAt: "09:18" }, { caughtAt: "17:30" }, { caughtAt: "17:31" }];
      expect(TripDates.countCatchsOutsideTrip(liveTrip, catchs)).toBe(1);
      expect(TripDates.validatePeriod({ ...liveTrip, finishedAt: "17:30" }, catchs.slice(0, 2),
        moment("2026-09-20")).finishedAtError).toBe("");
    });
  });

  it("valide les dates saisies", () => {
    const today = moment("2026-09-20");
    expect(TripDates.validatePeriod(carpSession, [], today)).toEqual(
      { dateError: "", startedAtError: "", endDateError: "", finishedAtError: "" });
    expect(TripDates.validatePeriod({ ...carpSession, endDate: "2026-09-12" }, [], today).finishedAtError)
      .toBe("La fin doit être après le début");
    expect(TripDates.validatePeriod({ ...carpSession, endDate: "2026-09-21" }, [], today).endDateError)
      .toBe("La date ne peut être dans le futur");
    expect(TripDates.validatePeriod({ ...carpSession, endDate: undefined }, [], today).endDateError)
      .toBe("Vous devez renseigner la date de fin");
  });

  it("refuse une heure de début ou de fin impossible (#309)", () => {
    const today = moment("2026-09-20");
    expect(TripDates.validatePeriod({ ...carpSession, startedAt: "25:00" }, [], today).startedAtError)
      .toBe("Heure invalide (format 24h HH:mm, ex. 13:45)");
    expect(TripDates.validatePeriod({ ...carpSession, finishedAt: "09:75" }, [], today).finishedAtError)
      .toBe("Heure invalide (format 24h HH:mm, ex. 13:45)");
  });

  it("accepte les heures HH:mm et HH:mm:ss, pas une heure incomplète ou impossible (#309)", () => {
    expect(TripDates.isValidTime("09:18")).toBe(true);
    expect(TripDates.isValidTime("09:18:34")).toBe(true);
    expect(TripDates.isValidTime("13:0")).toBe(false);
    expect(TripDates.isValidTime("25:00")).toBe(false);
    expect(TripDates.isValidTime("09:75")).toBe(false);
  });

  it("signale les captures que de nouvelles dates de sortie laissent dehors", () => {
    const shortened = { ...carpSession, endDate: "2026-09-13", finishedAt: "02:00" };
    expect(TripDates.validatePeriod(shortened, [{ caughtOn: "2026-09-13", caughtAt: "03:00" }],
      moment("2026-09-20")).finishedAtError)
      .toBe("Une capture est en dehors de la sortie : corrigez sa date et son heure ou celles de la sortie");
  });

  it("signale une durée au-delà de la durée plausible", () => {
    expect(TripDates.exceedsPlausibleDuration(carpSession, 7)).toBe(false);
    expect(TripDates.exceedsPlausibleDuration({ ...carpSession, endDate: "2026-09-20" }, 7)).toBe(true);
  });

  it("lit les dates du backend en date locale", () => {
    expect(TripDates.fromBackendDate([2026, 9, 14])).toBe("2026-09-14");
    expect(TripDates.fromBackendDate(undefined)).toBeUndefined();
  });

  // #278 : sortie a posteriori pré-remplie avec la date et l'heure de l'appareil.
  it("pré-remplit une sortie a posteriori : aujourd'hui, fin maintenant, début vide", () => {
    const defaults = TripDates.afterwardsDefaults(moment("2026-10-21 16:42:37", "YYYY-MM-DD HH:mm:ss"));
    expect(TripDates.toIsoDate(defaults.date!)).toBe("2026-10-21");
    expect(defaults.endDate).toBe("2026-10-21");
    expect(defaults.finishedAt).toBe("16:42");
    expect(defaults).not.toHaveProperty("startedAt");
  });

  it("garde la date locale du jour entre minuit et 2 h, jamais la veille UTC", () => {
    const defaults = TripDates.afterwardsDefaults(moment("2026-07-15 00:30", "YYYY-MM-DD HH:mm"));
    expect(TripDates.toIsoDate(defaults.date!)).toBe("2026-07-15");
    expect(defaults.endDate).toBe("2026-07-15");
    expect(defaults.finishedAt).toBe("00:30");
  });
});
