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
      <a v-bind:href="exportUrl" v-if="!asyncExport" id="export-button"
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

    <PersonalDashboard
      v-if="personalDashboard" :year="year"
      :dashboardData="personalDashboard"
      :selectedLakeId="selectedLakeId"
    />
  </section>
</template>

<script lang="ts">
import PersonalDashboard from "@/components/charts/PersonalDashboard.vue";
import LakeAndYearSelection from "@/components/common/LakeAndYearSelection.vue";
import DashboardService, { DashboardAndSpecies } from "@/services/DashboardService";
import EventBus from "@/services/EventBus";
import Helpers from "@/services/Helpers";
import { RouterUtils } from "@/router/RouterUtils";
import { Component, Prop, Vue } from "vue-facing-decorator";

/**
 * Tableau de bord personnel « Mes données » : filtre par année, export et
 * graphiques du pêcheur, tous plans d'eau confondus (#301).
 */
@Component({
  components: {
    PersonalDashboard,
    LakeAndYearSelection
  },
})
export default class PersonalDashboardSection extends Vue {
  @Prop({ default: "Mes données" }) title: string;
  @Prop({ default: "currentYear" }) yearQueryKey: string;

  exportUrl: string = "";
  ready: boolean = false;
  offline: boolean = false;
  asyncExport: boolean = false;
  personalDashboard: DashboardAndSpecies | null = null;
  year: number = new Date().getFullYear();
  // « Mes données » montre les données tous plans d'eau confondus : plus de
  // filtre par plan d'eau, ni d'onglet « Évolution » (calculée par plan d'eau).
  readonly selectedLakeId = "";
  isFirstLoad = true;

  mounted() {
    DashboardService.loadDashboardOrTimeout(this.year, "").then(
      this.personalDashboardLoaded,
      this.cannotLoad
    );
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
    DashboardService.loadDashboardOrTimeout(
      this.year,
      this.selectedLakeId
    ).then(this.personalDashboardLoaded, this.cannotLoad);
  }

  getDashboardYears(): number[] {
    const rangeDesc = (from: number, to: number) =>
      [...Array(Math.floor(from - to) + 1)].map((_, i) => from + i * -1);
    return rangeDesc(new Date().getFullYear(), 2020);
  }

  personalDashboardLoaded(data: DashboardAndSpecies) {
    // If no data for current year and this is first load, select year - 1 by default
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
