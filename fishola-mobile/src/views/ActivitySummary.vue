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
          <PersonalDashboardSection />
          <GlobalDashboardSection class="network-section" title="Statistiques du réseau"
            yearQueryKey="networkYear" v-bind:showExport="false" />
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
import PersonalDashboardSection from "@/components/charts/PersonalDashboardSection.vue";
import TripsService from "@/services/TripsService";
import { Component, Vue } from "vue-facing-decorator";

/**
 * « Résumé de mon activité » (#301) : tableau de bord personnel (« Mes données »,
 * filtre par année et export) puis statistiques du réseau, chacun avec sa propre
 * année dans l'URL.
 */
@Component({
  components: {
    CarnetHeader,
    RunningOverlay,
    BottomNav,
    PersonalDashboardSection,
    GlobalDashboardSection,
  },
})
export default class ActivitySummaryView extends Vue {
  hasRunningTrip: boolean = false;

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

.dashboard-page .network-section {
  display: block;
  margin-top: @vertical-margin-large;
}
</style>
