<!--
  #%L
  Carnet de pêche :: Mobile
  %%
  Copyright (C) 2019 - 2022 INRAE - UMR CARRTEL
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
  <div class="my-trips shifted-background page-with-header-and-footer">
    <CarnetHeader />
    <div class="page my-trips-page">
      <div class="pane pane-only">
        <div class="pane-content large rounded no-scroll">
          <div class="padding-content">
            <MyTrips :hasRunningTrip="hasRunningTrip" />
          </div>
          <div class="bottom">
            <RunningOverlay class="hiddenWhenKeyboardShows" v-if="hasRunningTrip" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import CarnetHeader from "@/components/layout/CarnetHeader.vue";
import RunningOverlay from "@/components/layout/RunningOverlay.vue";
import MyTrips from "@/views/MyTrips.vue";
import TripsService from "@/services/TripsService";
import { Component, Vue } from "vue-facing-decorator";

/**
 * Onglet « Mes prises » (#301) : liste des sorties, avec recherche, sélection et
 * suppression. La barre de navigation est portée par MyTrips (mode suppression).
 */
@Component({
  components: {
    CarnetHeader,
    MyTrips,
    RunningOverlay,
  },
})
export default class TripsListView extends Vue {
  hasRunningTrip = false;

  mounted() {
    TripsService.hasRunningTrip().then(
      (result: boolean) => (this.hasRunningTrip = result)
    );
  }
}
</script>
