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

import introJs from "intro.js";
import "intro.js/introjs.css";
import { ONBOARDING_LABELS, ONBOARDING_TOUR_STEPS } from "@/services/OnboardingContent";

/** Élément visible à l'écran : la barre de navigation est masquée sur ordinateur. */
function visibleElement(selector: string): HTMLElement | undefined {
  const element = document.querySelector(selector);
  return element instanceof HTMLElement && element.getClientRects().length > 0 ? element : undefined;
}

/**
 * Visite guidée des fonctions clés de l'accueil (#194) : saisie d'une sortie et
 * d'une capture, statistiques, badges. Une étape dont l'élément n'est pas
 * affiché est présentée au centre de l'écran.
 *
 * @param onEnd appelé une seule fois, à la fin de la visite ou si elle est passée
 */
export function startOnboardingTour(onEnd: () => void): void {
  let ended = false;
  const end = () => {
    if (!ended) {
      ended = true;
      onEnd();
    }
  };
  const steps = ONBOARDING_TOUR_STEPS.map((step) => ({
    element: visibleElement(step.selector),
    title: step.title,
    intro: step.text,
    position: step.above ? ("top" as const) : undefined,
  }));
  introJs
    .tour()
    .setOptions({
      steps,
      nextLabel: ONBOARDING_LABELS.next,
      prevLabel: ONBOARDING_LABELS.previous,
      doneLabel: ONBOARDING_LABELS.done,
      skipLabel: ONBOARDING_LABELS.skip,
      showBullets: true,
      showProgress: false,
      exitOnOverlayClick: false,
      disableInteraction: true,
      // L'accueil défile dans un conteneur interne, qu'intro.js ne fait pas
      // défiler : chaque élément y est centré avant d'afficher son étape.
      scrollToElement: false,
      tooltipClass: "carnet-tour",
    })
    .onBeforeChange((targetElement) => {
      targetElement.scrollIntoView({ block: "center" });
      return new Promise<boolean>((resolve) => requestAnimationFrame(() => resolve(true)));
    })
    .onComplete(end)
    .onExit(end)
    .start();
}
