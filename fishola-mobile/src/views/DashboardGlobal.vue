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
  <div class="dashboard page-with-header-and-footer shifted-background">
    <CarnetHeader />
    <div class="page dashboard-page">
      <div class="pane pane-only">
        <div id="scrollable" class="pane-content large rounded">
          <FormToggle class="network-toggle" label="Afficher les statistiques du réseau"
            v-bind:modelValue="showNetworkStats" v-on:update:modelValue="networkStatsToggled" />
          <GlobalDashboardSection v-if="showNetworkStats" />
          <section v-else class="dashboard-section network-hidden">
            <h1 class="no-margin-pane">Statistiques</h1>
            <p>Les statistiques de l'ensemble des pêcheurs sont masquées.</p>
          </section>
        </div>
      </div>
      <RunningOverlay class="hiddenWhenKeyboardShows" v-if="hasRunningTrip" />
    </div>
    <BottomNav />
  </div>
</template>

<script lang="ts">
import CarnetHeader from "@/components/layout/CarnetHeader.vue";
import RunningOverlay from "@/components/layout/RunningOverlay.vue";
import BottomNav from "@/components/layout/BottomNav.vue";
import GlobalDashboardSection from "@/components/charts/GlobalDashboardSection.vue";
import FormToggle from "@/components/common/FormToggle.vue";
import DashboardService from "@/services/DashboardService";
import TripsService from "@/services/TripsService";
import { Component, Vue } from "vue-facing-decorator";

/**
 * « Statistiques » : statistiques du réseau, filtrées par année. Le pêcheur peut
 * les masquer, choix mémorisé sur l'appareil (#307).
 */
@Component({
  components: {
    CarnetHeader,
    RunningOverlay,
    BottomNav,
    FormToggle,
    GlobalDashboardSection,
  },
})
export default class DashboardGlobalView extends Vue {
  hasRunningTrip: boolean = false;
  showNetworkStats: boolean = DashboardService.isNetworkStatsShown();

  networkStatsToggled(shown: boolean) {
    this.showNetworkStats = shown;
    DashboardService.setNetworkStatsShown(shown);
  }

  mounted() {
    TripsService.hasRunningTrip().then(
      (result: boolean) => (this.hasRunningTrip = result)
    );
    const scrolllistener = document.getElementById("scrollable");
    scrolllistener?.addEventListener("scroll", (_event: Event) => {
      const exportButton = document.getElementById("export-button");
      if (exportButton && exportButton.style) {
        if (scrolllistener.scrollTop > 50) {
          exportButton.style.display = "none";
        } else {
          exportButton.style.display = "block";
        }
      }
    });
  }
}
</script>

<style lang="less">
@import "../less/_dashboard-page";

.dashboard-page {
  .network-toggle {
    padding: @vertical-margin-x-small @margin-large 0 @margin-large;
  }

  .network-hidden p {
    padding-left: @margin-xx-large;
    padding-right: @margin-xx-large;
    color: @pale-sky;
  }
}
</style>
