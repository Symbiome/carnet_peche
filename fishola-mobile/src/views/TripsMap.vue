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
            <MyTripsMapView :visible="true" />
          </div>
          <BottomNav />
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import CarnetHeader from "@/components/layout/CarnetHeader.vue";
import BottomNav from "@/components/layout/BottomNav.vue";
import MyTripsMapView from "@/components/my-trips/MyTripsMap.vue";
import { Component, Vue } from "vue-facing-decorator";

/** Onglet « Carte » (#301) : la carte des sorties, seule. */
@Component({
  components: {
    CarnetHeader,
    BottomNav,
    MyTripsMapView,
  },
})
export default class TripsMapView extends Vue {}
</script>
