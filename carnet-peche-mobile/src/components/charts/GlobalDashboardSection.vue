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
    <h1 class="no-margin-pane h1-with-selects">
      <span>{{ title }}</span>
      <LakeAndYearSelection
        :years="getDashboardYears()"
        :showYears="true"
        :showLake="false"
        :yearQueryKey="yearQueryKey"
        @lake-and-year="yearAndLakeChangedChanged"
        @year="yearChanged"
      />
      <a v-bind:href="exportUrl" v-if="showExport && !asyncExport" id="export-button"
        class="export" title="Exporter" target="_blank">
        <span>Exporter</span>
        <i class="icon-download" />
      </a>
    </h1>

    <div class="spinner-wrapper" v-if="!ready">
      <div class="spinner"></div>
    </div>

    <div class="offline" v-if="ready && offline">
      <span>Le tableau de bord n'est pas disponible sans connexion
        internet</span>
    </div>

    <GlobalDashboardComponent v-if="globalDashboard"
      :showUpdateHour="year == new Date().getFullYear()" :dashboardData="globalDashboard"
      :selectedLakeId="selectedLakeId"></GlobalDashboardComponent>
  </section>
</template>

<script lang="ts">
import GlobalDashboardComponent from "@/components/charts/GlobalDashboardComponent.vue";
import LakeAndYearSelection from "@/components/common/LakeAndYearSelection.vue";
import DashboardService, { GlobalDashboardAndSpecies } from "@/services/DashboardService";
import EventBus from "@/services/EventBus";
import Helpers from "@/services/Helpers";
import { RouterUtils } from "@/router/RouterUtils";
import { Component, Prop, Vue } from "vue-facing-decorator";

/**
 * Statistiques du réseau, filtrées par année (#277), affichées seules
 * (« Statistiques ») ou dans « Résumé de mon activité » (#301).
 */
@Component({
  components: {
    GlobalDashboardComponent,
    LakeAndYearSelection
  },
})
export default class GlobalDashboardSection extends Vue {
  @Prop({ default: "Statistiques" }) title: string;
  @Prop({ default: "currentYear" }) yearQueryKey: string;
  /** Lien d'export : le même que celui de « Mes données », inutile en double. */
  @Prop({ default: true }) showExport: boolean;

  exportUrl: string = "";
  ready: boolean = false;
  offline: boolean = false;
  asyncExport: boolean = false;
  globalDashboard: GlobalDashboardAndSpecies | null = null;
  year: number = new Date().getFullYear();
  // « Statistiques » porte sur tous les milieux confondus, filtrées par année
  // uniquement : plus de filtre par plan d'eau, ni d'onglet « Évolution » (#277).
  readonly selectedLakeId = "";
  isFirstLoad = true;

  mounted() {
    this.exportUrl = DashboardService.getExportUrl();
    Helpers.ifApplication(() => (this.asyncExport = true));
  }

  yearChanged(newYear: number) {
    this.year = newYear;
    this.reloadDashboard();
  }

  yearAndLakeChangedChanged(event: any) {
    this.year = event.year;
    this.reloadDashboard();
  }

  reloadDashboard(): void {
    DashboardService.loadGlobalDashboardOrTimeout(
      this.year,
      this.selectedLakeId
    ).then(this.globalDashboardLoaded, this.cannotLoad);
  }

  getDashboardYears(): number[] {
    const rangeDesc = (from: number, to: number) =>
      [...Array(Math.floor(from - to) + 1)].map((_, i) => from + i * -1);
    return rangeDesc(new Date().getFullYear(), 2020);
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

  globalDashboardLoaded(data: GlobalDashboardAndSpecies) {
    // Comme « Mes données » : sans capture sur l'année en cours au premier
    // chargement, on affiche l'année précédente. Le sélecteur suit l'URL.
    const currentYear = new Date().getFullYear();
    const isEmpty = Object.keys(data.dashboard.caughtSpeciesCount).length == 0;
    if (this.isFirstLoad && isEmpty && this.year == currentYear) {
      this.isFirstLoad = false;
      this.$router.replace({
        query: { ...this.$route.query, [this.yearQueryKey]: "" + (currentYear - 1) },
      });
      return;
    }
    this.isFirstLoad = false;
    this.globalDashboard = data;
    this.ready = true;
  }
}
</script>
