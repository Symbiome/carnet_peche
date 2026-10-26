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
  <div class="credits page-with-header-and-footer shifted-background">
    <CarnetHeader />
    <div class="page credits-page">
      <div class="pane pane-only">
        <div class="pane-content rounded">
          <h1>Qui porte le projet ?</h1>

          <div class="credits-card">
            <a v-bind:href="ufbrmcUrl" target="_blank" rel="noopener" class="credits-logo">
              <img src="/img/logo/ufbrmc.jpg" alt="Union des Bassins Rhône-Méditerranée-Corse (UFBRMC)" />
            </a>
            <p class="credits-description">
              Carnet de pêche est porté par l'UFBRMC, qui réunit les fédérations
              de pêche du bassin Rhône-Méditerranée &amp; Corse et leurs quelque
              400 000 pêcheurs. Avec cet outil commun et open source, vos
              captures aident à mieux connaître les poissons des rivières, lacs
              et étangs, et à mieux les gérer.
            </p>
            <a v-bind:href="ufbrmcUrl" target="_blank" rel="noopener" class="credits-link">
              Découvrir l'UFBRMC
              <i class="icon-arrow" />
            </a>
          </div>

          <div class="bottom-page-spacer"></div>
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

import TripsService from "@/services/TripsService";

import { Component, Vue } from "vue-facing-decorator";

/**
 * « Qui porte le projet ? » : logo, courte présentation et lien vers le site
 * de l'UFBRMC, porteur de Carnet de pêche.
 */
@Component({
  components: {
    CarnetHeader,
    RunningOverlay,
    BottomNav,
  },
})
export default class CreditsView extends Vue {
  readonly ufbrmcUrl =
    "https://www.peche-auvergne-rhonealpes.fr/actualite/3754/1913-union-des-bassins-rhone-mediterranee-corse.htm";

  hasRunningTrip: boolean = false;

  mounted() {
    TripsService.hasRunningTrip().then((result: boolean) => (this.hasRunningTrip = result));
  }
}
</script>

<style scoped lang="less">
.credits-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  margin-top: @vertical-margin-medium;
  padding: 24px 20px;
  border-radius: 13px;
  background-color: @surface;
  letter-spacing: @letter-spacing;
}

.credits-logo img {
  display: block;
  width: 160px;
  border-radius: 8px;
}

.credits-description {
  margin: 0;
  color: @gunmetal;
  font-size: 14px;
  line-height: 1.5;
  text-align: left;
}

.credits-link {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  border-radius: 8px;
  background-color: @carnet-blue;
  color: @white;
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
}

@media screen and (min-width: @desktop-min-width) {
  .credits-card {
    max-width: 700px;
  }

  .credits-description {
    font-size: 16px;
  }
}
</style>
