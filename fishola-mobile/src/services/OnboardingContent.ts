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

/**
 * Textes de l'onboarding (#194), réunis ici pour être remplacés sans toucher à
 * la logique. PROVISOIRES : le narratif définitif sera fourni par l'UFBRMC.
 */

/** Un écran d'introduction. */
export interface OnboardingSlide {
  emoji: string;
  title: string;
  text: string;
}

/** Une étape de la visite guidée, rattachée à un élément de l'accueil. */
export interface OnboardingTourStep {
  /** Sélecteur CSS de l'élément mis en avant ; étape centrée s'il est absent. */
  selector: string;
  /** Bulle au-dessus de l'élément (barre de navigation, en bas de l'écran). */
  above?: boolean;
  title: string;
  text: string;
}

export const ONBOARDING_SLIDES: OnboardingSlide[] = [
  {
    emoji: "🌊",
    title: "Protégeons nos cours d'eau",
    text: "Chaque sortie que vous enregistrez aide à mieux connaître les poissons et les milieux aquatiques, pour mieux les protéger.",
  },
  {
    emoji: "🔒",
    title: "Vos données restent anonymes",
    text: "Vos captures sont utilisées de façon anonymisée par les fédérations et les scientifiques, pour la connaissance et la gestion des milieux.",
  },
  {
    emoji: "🎣",
    title: "Merci, sentinelle des cours d'eau !",
    text: "En partageant vos prises, vous devenez une sentinelle des cours d'eau. Découvrons ensemble l'application.",
  },
];

export const ONBOARDING_TOUR_STEPS: OnboardingTourStep[] = [
  {
    selector: ".bottom-nav .nav-action",
    above: true,
    title: "Saisir une sortie",
    text: "Démarrez une sortie en direct au bord de l'eau, ou saisissez-la à votre retour.",
  },
  {
    selector: ".bottom-nav .nav-item-catches",
    above: true,
    title: "Saisir une capture",
    text: "Retrouvez vos sorties dans « Mes prises » et ajoutez-y vos captures : espèce, taille, photo.",
  },
  {
    selector: ".home-summary .trips-card",
    title: "Vos statistiques",
    text: "Suivez vos sorties, vos prises et vos espèces sur 30 jours ou 12 mois. Le détail est dans « Compte › Résumé de mon activité ».",
  },
  {
    selector: ".home-badges-header",
    title: "Vos badges",
    text: "Débloquez des badges au fil de vos sorties et consultez toute la collection.",
  },
];

export const ONBOARDING_LABELS = {
  skip: "Passer",
  previous: "Précédent",
  next: "Suivant",
  start: "Découvrir l'application",
  done: "Terminer",
};
