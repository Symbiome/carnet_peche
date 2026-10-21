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

import { ONBOARDING_SLIDES, ONBOARDING_TOUR_STEPS } from "@/services/OnboardingContent";
import OnboardingService from "@/services/OnboardingService";

function memoryStorage(): Storage {
  const values = new Map<string, string>();
  return {
    get length() {
      return values.size;
    },
    clear: () => values.clear(),
    getItem: (key) => values.get(key) ?? null,
    key: (index) => Array.from(values.keys())[index] ?? null,
    removeItem: (key) => {
      values.delete(key);
    },
    setItem: (key, value) => {
      values.set(key, value);
    },
  };
}

// #194 : onboarding mémorisé par appareil, relançable.
describe("OnboardingService", () => {
  it("s'affiche tant qu'il n'a été ni terminé ni passé, puis plus jamais", () => {
    const storage = memoryStorage();
    expect(OnboardingService.isDone(storage)).toBe(false);
    OnboardingService.markDone(storage);
    expect(OnboardingService.isDone(storage)).toBe(true);
  });

  it("peut être relancé", () => {
    const storage = memoryStorage();
    OnboardingService.markDone(storage);
    OnboardingService.replay(storage);
    expect(OnboardingService.isDone(storage)).toBe(false);
  });

  it("ne s'impose pas à chaque écran si le stockage est indisponible", () => {
    const broken = memoryStorage();
    broken.getItem = () => {
      throw new Error("SecurityError");
    };
    expect(OnboardingService.isDone(broken)).toBe(true);
  });
});

describe("Contenu de l'onboarding", () => {
  it("présente le but, l'anonymisation et remercie la sentinelle", () => {
    const texts = ONBOARDING_SLIDES.map((slide) => slide.title + " " + slide.text).join(" ");
    expect(texts).toMatch(/protéger/i);
    expect(texts).toMatch(/anonym/i);
    expect(texts).toMatch(/sentinelle/i);
  });

  it("guide vers la saisie d'une sortie et d'une capture, les statistiques et les badges", () => {
    expect(ONBOARDING_TOUR_STEPS.map((step) => step.title)).toEqual([
      "Saisir une sortie",
      "Saisir une capture",
      "Vos statistiques",
      "Vos badges",
    ]);
    ONBOARDING_TOUR_STEPS.forEach((step) => expect(step.selector).not.toBe(""));
  });
});
