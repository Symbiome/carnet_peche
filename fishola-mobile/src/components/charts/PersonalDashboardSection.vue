<!--
  #%L
  Carnet de pêche :: Mobile
  %%
  Copyright (C) 2019 - 2021 INRAE - UMR CARRTEL
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
  <section class="dashboard-section">
    <div class="year-filter">
      <div class="year-filter-header">
        <p class="year-filter-label">
          <i class="icon-calendar" />
          Année affichée dans les cartes ci-dessous
        </p>
        <a v-bind:href="exportUrl" v-if="!asyncExport" id="export-button"
          class="export" title="Exporter mes données" target="_blank">
          <i class="icon-download" />
          <span>Exporter</span>
        </a>
      </div>
      <div class="year-toggle" role="group" aria-label="Année des cartes ci-dessous">
        <button
          v-for="aYear in years"
          :key="aYear"
          :class="{ active: year === aYear }"
          :aria-pressed="year === aYear"
          v-on:click="selectYear(aYear)"
        >
          {{ aYear }}
        </button>
      </div>
    </div>

    <div class="spinner-wrapper" v-if="!ready">
      <div class="spinner"></div>
    </div>

    <div class="offline" v-if="ready && offline">
      <span>Le tableau de bord n'est pas disponible sans connexion
        internet</span>
    </div>

    <PersonalDashboard
      v-if="personalDashboard" :year="year"
      :dashboardData="personalDashboard"
      :selectedLakeId="selectedLakeId"
    />
  </section>
</template>

<script lang="ts">
import PersonalDashboard from "@/components/charts/PersonalDashboard.vue";
import DashboardService, { DashboardAndSpecies } from "@/services/DashboardService";
import EventBus from "@/services/EventBus";
import Helpers from "@/services/Helpers";
import { RouterUtils } from "@/router/RouterUtils";
import { Component, Prop, Vue } from "vue-facing-decorator";

const FIRST_DASHBOARD_YEAR = 2020;

/**
 * Tableau de bord personnel : année, export et graphiques du pêcheur, tous
 * plans d'eau confondus (#301), affichés en cartes sous celles de « Mes
 * statistiques ». L'année choisie est rappelée dans le titre de chaque carte
 * et gardée dans l'URL (`yearQueryKey`) pour le retour depuis une prise.
 */
@Component({
  components: {
    PersonalDashboard,
  },
})
export default class PersonalDashboardSection extends Vue {
  @Prop({ default: "currentYear" }) yearQueryKey: string;

  exportUrl: string = "";
  ready: boolean = false;
  offline: boolean = false;
  asyncExport: boolean = false;
  personalDashboard: DashboardAndSpecies | null = null;
  year: number = new Date().getFullYear();
  // Données tous plans d'eau confondus : plus de filtre par plan d'eau, ni
  // d'onglet « Évolution » (calculée par plan d'eau).
  readonly selectedLakeId = "";
  isFirstLoad = true;

  get years(): number[] {
    const currentYear = new Date().getFullYear();
    return Array.from(
      { length: currentYear - FIRST_DASHBOARD_YEAR + 1 },
      (_, index) => currentYear - index
    );
  }

  mounted() {
    const yearFromURL = parseInt(this.$route.query[this.yearQueryKey] as string);
    if (this.years.includes(yearFromURL)) {
      this.year = yearFromURL;
      this.isFirstLoad = false;
    }
    this.reloadDashboard();
    this.exportUrl = DashboardService.getExportUrl();
    Helpers.ifApplication(() => (this.asyncExport = true));
  }

  selectYear(year: number) {
    if (year === this.year) {
      return;
    }
    this.year = year;
    this.isFirstLoad = false;
    this.$router.replace({
      query: { ...this.$route.query, [this.yearQueryKey]: "" + year },
    });
    this.reloadDashboard();
  }

  reloadDashboard(): void {
    DashboardService.loadDashboardOrTimeout(
      this.year,
      this.selectedLakeId
    ).then(this.personalDashboardLoaded, this.cannotLoad);
  }

  personalDashboardLoaded(data: DashboardAndSpecies) {
    // Sans prise sur l'année en cours au premier chargement, on montre l'année
    // précédente : le sélecteur suit, puisqu'il lit `year`.
    if (
      this.isFirstLoad &&
      data.dashboard &&
      data.dashboard.latestTripsCatchs.length == 0
    ) {
      this.year = this.year - 1;
      this.reloadDashboard();
    } else {
      this.personalDashboard = data;
      this.ready = true;
    }
    this.isFirstLoad = false;
  }

  cannotLoad(error: any) {
    if (error && error.timeoutReached) {
      this.offline = true;
    } else if (error && error.status == 401) {
      EventBus.emit("toaster-warning", "Vous n'êtes plus connecté\u00B7e");
      RouterUtils.pushRouteNoDuplicate(this.$router, "/login");
    }
    this.ready = true;
  }
}
</script>

<style scoped lang="less">
.year-filter {
  margin-bottom: 25px;
  text-align: left;
  letter-spacing: @letter-spacing;
}

.year-filter-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin: 0 2px 10px 2px;
}

.year-filter-label {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  color: @heading;
  font-size: 14px;
  font-weight: 600;

  i {
    color: @accent-text;
  }
}

.export {
  display: flex;
  flex: none;
  align-items: center;
  gap: 6px;
  color: @accent-text;
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
}

// Même bascule que « 30 jours / Saison » en haut de la page.
.year-toggle {
  display: flex;
  gap: 4px;
  padding: 7px 8px;
  border-radius: 8.873px;
  background-color: @surface;
  overflow-x: auto;
  scrollbar-width: none;

  &::-webkit-scrollbar {
    display: none;
  }

  button {
    flex: 1 0 auto;
    min-width: 56px;
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
      color: @carnet-ink;
    }

    &:active {
      animation: none;
    }
  }
}
</style>
