<!--
  #%L
  Carnet de pêche :: Mobile
  %%
  Copyright (C) 2019 - 2026 INRAE - UMR CARRTEL
  %%
  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU Affero General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU Affero General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
  #L%
  -->
<template>
  <div class="statistics">
    <div class="statistics-content">
      <header class="statistics-header">
        <button class="back" aria-label="Retour" v-on:click="goBack">
          <img src="img/carnet/header-back.svg" alt="" />
        </button>
        <h1>Mes statistiques</h1>
      </header>

      <div class="period-toggle" role="group" aria-label="Période des statistiques">
        <button
          v-for="option in periodOptions"
          :key="option.value"
          :class="{ active: period === option.value }"
          :aria-pressed="period === option.value"
          v-on:click="selectPeriod(option.value)"
        >
          {{ option.label }}
        </button>
      </div>

      <div class="statistics-state" v-if="!view && !offline">
        <div class="loader" />
      </div>
      <p class="statistics-state" v-if="offline">
        Les statistiques ne sont pas disponibles sans connexion internet.
      </p>

      <div class="statistics-cards" v-if="view && stats">
        <TripsStatisticsCard :view="view" />
        <RegularityStatisticsCard :view="view" />
        <CatchesStatisticsCard :view="view" />
        <SpeciesStatisticsCard :view="view" />
        <QualityStatisticsCard :view="view" />
        <WaterEntitiesStatisticsCard :waterEntities="stats.waterEntities" />
      </div>
    </div>
    <RunningOverlay class="hiddenWhenKeyboardShows" v-if="hasRunningTrip" />
    <BottomNav />
  </div>
</template>

<script lang="ts">
import BottomNav from "@/components/layout/BottomNav.vue";
import RunningOverlay from "@/components/layout/RunningOverlay.vue";
import CatchesStatisticsCard from "@/components/statistics/CatchesStatisticsCard.vue";
import QualityStatisticsCard from "@/components/statistics/QualityStatisticsCard.vue";
import RegularityStatisticsCard from "@/components/statistics/RegularityStatisticsCard.vue";
import SpeciesStatisticsCard from "@/components/statistics/SpeciesStatisticsCard.vue";
import TripsStatisticsCard from "@/components/statistics/TripsStatisticsCard.vue";
import WaterEntitiesStatisticsCard from "@/components/statistics/WaterEntitiesStatisticsCard.vue";
import { PersonalStatistics, SpeciesWithAlias } from "@/pojos/BackendPojos";
import { RouterUtils } from "@/router/RouterUtils";
import DashboardService from "@/services/DashboardService";
import EventBus from "@/services/EventBus";
import {
  buildStatisticsView,
  StatisticsPeriod,
  statisticsRange,
  StatisticsView,
  toIsoDay,
} from "@/services/PersonalStatisticsView";
import ReferentialService from "@/services/ReferentialService";
import TripsService from "@/services/TripsService";
import { Component, Vue } from "vue-facing-decorator";

/**
 * « Mes statistiques » (maquette Figma « Statistiques ») : sorties,
 * régularité, prises, espèces, qualité des données et cours d'eau du
 * pêcheur, sur la saison en cours ou les 30 derniers jours. Remplace
 * l'ancien « Résumé de mon activité » (#301).
 */
@Component({
  components: {
    BottomNav,
    RunningOverlay,
    TripsStatisticsCard,
    RegularityStatisticsCard,
    CatchesStatisticsCard,
    SpeciesStatisticsCard,
    QualityStatisticsCard,
    WaterEntitiesStatisticsCard,
  },
})
export default class ActivitySummaryView extends Vue {
  period: StatisticsPeriod = "season";
  periodOptions = [
    { value: "30d" as StatisticsPeriod, label: "30 jours" },
    { value: "season" as StatisticsPeriod, label: "Saison " + new Date().getFullYear() },
  ];
  stats: PersonalStatistics | null = null;
  speciesNames: Map<string, string> = new Map();
  offline: boolean = false;
  hasRunningTrip: boolean = false;
  loadedPeriods: Map<StatisticsPeriod, PersonalStatistics> = new Map();

  get view(): StatisticsView | null {
    return this.stats ? buildStatisticsView(this.stats, this.period, this.speciesNames) : null;
  }

  mounted() {
    TripsService.hasRunningTrip().then((result: boolean) => (this.hasRunningTrip = result));
    ReferentialService.getAllSpecies().then(this.speciesLoaded, () => undefined);
    this.loadStatistics();
  }

  speciesLoaded(species: SpeciesWithAlias[]) {
    this.speciesNames = new Map(species.map((aSpecies) => [aSpecies.id, aSpecies.name]));
  }

  selectPeriod(period: StatisticsPeriod) {
    if (period !== this.period) {
      this.period = period;
      this.loadStatistics();
    }
  }

  loadStatistics() {
    const period = this.period;
    const cached = this.loadedPeriods.get(period);
    this.stats = cached || null;
    this.offline = false;
    if (cached) {
      return;
    }
    const range = statisticsRange(period);
    DashboardService.loadPersonalStatisticsOrTimeout(toIsoDay(range.from), toIsoDay(range.to)).then(
      (stats: PersonalStatistics) => {
        this.loadedPeriods.set(period, stats);
        if (this.period === period) {
          this.stats = stats;
        }
      },
      this.cannotLoad
    );
  }

  cannotLoad(error: any) {
    if (error && error.status == 401) {
      EventBus.emit("toaster-warning", "Vous n'êtes plus connecté·e");
      RouterUtils.pushRouteNoDuplicate(this.$router, "/login");
    } else {
      this.offline = true;
    }
  }

  goBack() {
    this.$router.back();
  }
}
</script>

<style scoped lang="less">
.statistics {
  position: relative;
  height: 100%;
  background-color: @white-smoke;

  @media screen and (min-width: @desktop-min-width) {
    height: 100vh;
  }
}

.statistics-content {
  height: calc(100% - @footer-height);
  overflow-y: auto;
  overflow-x: hidden;
  padding: env(safe-area-inset-top) 12px @margin-large 12px;
  letter-spacing: @letter-spacing;

  @media screen and (min-width: @desktop-min-width) {
    height: 100%;
    max-width: 700px;
    padding: @margin-large @margin-large-desktop;
  }
}

.statistics-header {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 30px 0 0 5px;

  .back {
    display: flex;
    padding: 0;
    border: none;
    background: none;
    -webkit-tap-highlight-color: transparent;

    &:active {
      animation: none;
    }
  }

  h1 {
    margin: 0;
    color: @heading;
    font-size: 22px;
    font-weight: 700;
    line-height: 1.2;
  }
}

// Le chevron de la maquette est bleu nuit : passé en blanc sur fond sombre.
:root[data-theme="dark"] .statistics-header .back img {
  filter: brightness(0) invert(1);
}

.period-toggle {
  display: flex;
  gap: 4px;
  margin-top: 27px;
  padding: 7px 8px;
  border-radius: 8.873px;
  background-color: @surface;

  button {
    flex: 1 1 0;
    height: 43px;
    border: none;
    border-radius: 8px;
    background: transparent;
    color: @pale-sky;
    font-size: 14px;
    font-weight: 500;
    letter-spacing: @letter-spacing;

    &.active {
      background-color: @carnet-sky;
      // Comme le résumé de l'accueil : blanc sur ce bleu ciel n'atteint que
      // 3,5:1, trop peu pour du texte de 14 px.
      color: @carnet-ink;
    }

    &:active {
      animation: none;
    }
  }
}

.statistics-state {
  display: flex;
  justify-content: center;
  margin-top: @margin-x-large;
  color: @pale-sky;
  font-size: 14px;
  text-align: center;
}

.statistics-cards {
  display: flex;
  flex-direction: column;
  gap: 25px;
  margin-top: 23px;
}
</style>
