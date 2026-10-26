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
  <div class="dashboard-cards">
    <section class="dashboard-card tone-yellow" v-if="picturesPerTrip">
      <h2 class="card-title pointer" @click="openGalery">
        <span>Mes photos</span>
        <span class="card-year">{{ year }}</span>
      </h2>
      <div class="dashboard-top-catchs catch-preview-list-scrollable">
        <GaleryPreviewList
          v-bind:picturesPerTrip="picturesPerTrip"
          :year="year"
          :selectedLakeId="selectedLakeId"
          bottomMode="index"
        />
      </div>
    </section>

    <section class="dashboard-card tone-sky">
      <h2 class="card-title">
        <span>Mes poissons</span>
        <span class="card-year">{{ year }}</span>
      </h2>
      <DistributionChart
        :distribution="caughtSpeciesDistribution"
        legend="Capturés"
        greenLegend="conservés"
      ></DistributionChart>
    </section>

    <section class="dashboard-card tone-green">
      <h2 class="card-title">
        <span>Nombre de prises par mois</span>
        <span class="card-year">{{ year }}</span>
      </h2>
      <div class="not-enough-data" v-if="monthlySizesOptions.length == 0">
        <span>Pas assez de données</span>
      </div>
      <OptionsList
        :items="monthlyCountOptions"
        v-if="monthlyCountOptions.length > 0"
        v-on:item-selected="onMonthlyCountSelected"
      >
      </OptionsList>
      <HistogramChart
        v-if="monthlyCount"
        :values="monthlyCount"
        :orderedMonths="orderedMonths"
        :is-active-month-condition="isActiveMonth"
        :average="averageCatchsPerTripRounded"
        averageLabel="Nombre moyen de prises par sortie de pêche (Toutes espèces confondues)"
      ></HistogramChart>
    </section>

    <AnglerEffortStatistics
      :year="year"
      :selectedLakeId="selectedLakeId"
      :species="dashboardData.species"
    />

    <section class="dashboard-card tone-sky">
      <div class="avg-size">
        <h2 class="card-title">
        <span>Taille moyenne par espèce (cm)</span>
        <span class="card-year">{{ year }}</span>
      </h2>
        <MaillageLegend
          :selectedLakeId="selectedLakeId"
          :maillages="maillages"
        />
      </div>
      <div class="not-enough-data" v-if="monthlySizesOptions.length == 0">
        <span>Pas assez de données</span>
      </div>
      <OptionsList
        :items="monthlySizesOptions"
        v-if="monthlySizesOptions.length > 0"
        v-on:item-selected="onMonthlySizeSelected"
      >
      </OptionsList>
      <HistogramChart
        v-if="monthlySizes"
        :values="monthlySizes"
        :orderedMonths="orderedMonths"
        :is-active-month-condition="isActiveMonth"
      ></HistogramChart>
    </section>

    <section class="dashboard-card tone-yellow">
      <h2 class="card-title">
        <span>Top 5 tailles</span>
        <span class="card-year">{{ year }}</span>
      </h2>
      <div class="not-enough-data" v-if="topBySizeOptions.length == 0">
        <span>Pas assez de données</span>
      </div>
      <OptionsList
        :items="topBySizeOptions"
        v-if="topBySizeOptions.length > 0"
        v-on:item-selected="onTopSizeSelected"
      ></OptionsList>
      <div
        class="dashboard-top-catchs catch-preview-list-scrollable"
        v-if="topBySizeCatchs"
      >
        <CatchPreviewList
          v-bind:modifiable="false"
          v-bind:reverse="false"
          lakeId=""
          v-bind:catchs="topBySizeCatchs"
          v-on:openCatchFromId="openCatch($event)"
          bottomMode="index"
        />
      </div>
    </section>

    <section class="dashboard-card tone-green">
      <h2 class="card-title">
        <span>Top 5 poids</span>
        <span class="card-year">{{ year }}</span>
      </h2>
      <div class="not-enough-data" v-if="topByWeightOptions.length == 0">
        <span>Pas assez de données</span>
      </div>
      <OptionsList
        :items="topByWeightOptions"
        v-if="topByWeightOptions.length > 0"
        v-on:item-selected="onTopWeightSelected"
      ></OptionsList>
      <div
        class="dashboard-top-catchs catch-preview-list-scrollable"
        v-if="topByWeightCatchs"
      >
        <CatchPreviewList
          v-bind:modifiable="false"
          v-bind:reverse="false"
          lakeId=""
          v-bind:catchs="topByWeightCatchs"
          v-on:openCatchFromId="openCatch($event)"
          metaMode="weight"
          bottomMode="index"
        />
      </div>
    </section>
  </div>
</template>

<script lang="ts">
import { RouterUtils } from "@/router/RouterUtils";
import CatchPreviewList from "@/components/trip/CatchPreviewList.vue";
import GaleryPreviewList from "@/components/galery/GaleryPreviewList.vue";

import OptionsList from "@/components/common/OptionsList.vue";

import DistributionChart from "@/components/charts/DistributionChart.vue";
import HistogramChart from "@/components/charts/HistogramChart.vue";
import AnglerEffortStatistics from "@/components/charts/personal-dashboard/AnglerEffortStatistics.vue";

import Constants from "@/services/Constants";
import TripsService from "@/services/TripsService";
import {
  SpeciesWithAlias,
  CatchBean,
  Month,
  PicturePerTripBean,
  Maillage,
} from "@/pojos/BackendPojos";
import DistributionEntry from "@/pojos/DistributionEntry";
import OptionItem from "@/pojos/OptionItem";
import { DashboardAndSpecies } from "@/services/DashboardService";

import { Component, Prop, Vue, Watch } from "vue-facing-decorator";
import orderBy from "lodash/orderBy";

import moment from "moment";
import MaillageLegend from "./MaillageLegend.vue";

export class TopEntry {
  constructor(public species: SpeciesWithAlias, public catchs: CatchBean[]) {}
}

@Component({
  components: {
    DistributionChart,
    OptionsList,
    HistogramChart,
    CatchPreviewList,
    GaleryPreviewList,
    MaillageLegend,
    AnglerEffortStatistics,
  },
})
export default class PersonalDashboard extends Vue {
  @Prop() dashboardData: DashboardAndSpecies;
  @Prop() year: number;
  @Prop() selectedLakeId: string;

  speciesIndex: { [index: string]: SpeciesWithAlias } = {};

  averageCatchsPerTripRounded: number = 0;
  caughtSpeciesDistribution: DistributionEntry[] = [];
  topBySizeOptions: OptionItem[] = [];
  topBySizeCatchs: CatchBean[] | null = null;
  topByWeightOptions: OptionItem[] = [];
  topByWeightCatchs: CatchBean[] | null = null;
  orderedMonths: Month[] | null = null;
  monthlySizesOptions: OptionItem[] = [];
  monthlyCountOptions: OptionItem[] = [];
  monthlySizes: { [P in Month]?: number } | null = null;
  monthlyCount: { [P in Month]?: number } | null = null;
  picturesPerTrip: PicturePerTripBean[] = [];

  // On a besoin de maintenir un index de capture -> sortie
  catchToTripId: { [index: string]: string } = {};
  maillages: Maillage[] = [];

  constructor() {
    super();
  }

  @Watch("dashboardData")
  dashboardDataChanged(): void {
    this.speciesIndex = {};
    this.caughtSpeciesDistribution = [];
    this.topBySizeOptions = [];
    this.topBySizeCatchs = [];
    this.topByWeightOptions = [];
    this.topBySizeCatchs = [];
    this.orderedMonths = [];
    this.monthlySizesOptions = [];
    this.monthlyCountOptions = [];
    this.monthlySizes = null;
    this.monthlyCount = null;
    this.catchToTripId = {};
    this.picturesPerTrip = [];

    const speciesAliases = this.dashboardData.dashboard.speciesAliases;
    this.dashboardData.species.forEach((species) => {
      this.speciesIndex[species.id] = species;

      const aliases = speciesAliases[species.id];
      if (aliases) {
        species.alias = aliases.join(" ou ");
      }
    });

    const speciesCount = this.dashboardData.dashboard.caughtSpeciesCount;
    const distribution = this.dashboardData.dashboard.caughtSpeciesDistribution;
    const releasedDistribution =
      this.dashboardData.dashboard.caughtAndReleasedSpeciesDistribution;
    Object.keys(distribution).forEach((speciesId) => {
      const species: SpeciesWithAlias = this.speciesIndex[speciesId];
      const percent: number = Math.round(distribution[speciesId]);
      const releasedPercent: number =
        Math.round(releasedDistribution[speciesId]) | 0;
      const keptPercent: number = percent - releasedPercent;
      const count: number = speciesCount[speciesId];
      const keptCount: number =
        percent == 0 ? 0 : Math.round((count * keptPercent) / percent);
      const keptLabel: string = "conservé" + (keptCount > 1 ? "s" : "");
      const entry: DistributionEntry = new DistributionEntry(
        speciesId,
        species.name,
        percent,
        keptPercent,
        count,
        species.alias,
        keptCount,
        keptLabel
      );
      this.caughtSpeciesDistribution.push(entry);
    });

    const averageCatchsPerTrip =
      this.dashboardData.dashboard.averageCatchsPerTrip || 0;
    this.averageCatchsPerTripRounded =
      Math.round(10 * averageCatchsPerTrip) / 10;

    const topBySize: TopEntry[] = this.parseTop(
      this.dashboardData.dashboard.topBySize
    );
    topBySize.forEach((top) => {
      this.topBySizeOptions.push({
        id: top.species.id,
        name: top.species.name,
        alias: top.species.alias,
        whatever: top.catchs,
      });
    });
    this.topBySizeOptions = orderBy(this.topBySizeOptions, "name");

    const topByWeight: TopEntry[] = this.parseTop(
      this.dashboardData.dashboard.topByWeight
    );
    topByWeight.forEach((top) => {
      this.topByWeightOptions.push({
        id: top.species.id,
        name: top.species.name,
        alias: top.species.alias,
        whatever: top.catchs,
      });
    });
    this.topByWeightOptions = orderBy(
      this.topByWeightOptions,
      "name"
    );

    this.orderedMonths = this.dashboardData.dashboard.orderedMonths;
    const monthlySizesSpecies: string[] = Object.keys(
      this.dashboardData.dashboard.monthlySizesPerMaillage
    );
    monthlySizesSpecies.forEach((speciesId) => {
      const species: SpeciesWithAlias = this.speciesIndex[speciesId];
      this.monthlySizesOptions.push({
        id: species.id,
        name: species.name,
        alias: species.alias,
        whatever:
          this.dashboardData.dashboard.monthlySizesPerMaillage[speciesId],
      });
      this.monthlyCountOptions.push({
        id: species.id,
        name: species.name,
        alias: species.alias,
        whatever:
          this.dashboardData.dashboard.monthlySizesPerMaillage[speciesId],
      })
    });
    this.monthlySizesOptions = orderBy(
      this.monthlySizesOptions,
      "name"
    );
    this.monthlyCountOptions = orderBy(
      this.monthlySizesOptions,
      "name"
    );
    this.monthlyCountOptions.unshift({
        id: "all",
        name: "Toutes les espèces",
        alias: "",
        whatever:
          this.computeTotalCountPerMonthAllSpeciesCombined(),
    });
    this.picturesPerTrip = this.dashboardData.dashboard.picturesPerTrip;
  }

  computeTotalCountPerMonthAllSpeciesCombined() {
    const monthlyCount = {};
    const dashboardPerSpecie = this.dashboardData.dashboard.monthlySizesPerMaillage;
    Object.keys(dashboardPerSpecie).forEach(specie => {     
      Object.keys(dashboardPerSpecie[specie]).forEach((month) => {
        // @ts-ignore
        monthlyCount[month] = monthlyCount[month] ?? {};
        // @ts-ignore
         const dashboardForSpeciePerMonth = dashboardPerSpecie[specie][month];
        
        // @ts-ignore
        Object.keys(dashboardForSpeciePerMonth).forEach((maillage) => {
          // @ts-ignore
          const countAndAverage = dashboardForSpeciePerMonth[maillage];
          // @ts-ignore
          const previousCount = monthlyCount[month]["NON_DEFINI"] ? parseInt(Object.keys(monthlyCount[month]["NON_DEFINI"])[0]) : 0;
          const newCount = previousCount + parseInt(Object.keys(countAndAverage)[0]);
          // @ts-ignore
          monthlyCount[month]["NON_DEFINI"] = {};
          // @ts-ignore
          monthlyCount[month]["NON_DEFINI"][newCount] = 0;
        });
      });
    });
    return monthlyCount;
  }

  parseTop(rawTop: { [index: string]: CatchBean[] }): TopEntry[] {
    const result: TopEntry[] = [];
    const topSpecies: string[] = Object.keys(rawTop);
    topSpecies.forEach((speciesId) => {
      const species: SpeciesWithAlias = this.speciesIndex[speciesId];
      const catchs: CatchBean[] = [];
      const rawCatchs: any[] = rawTop[speciesId];
      rawCatchs.forEach((rawCatch: any) => {
        this.catchToTripId[rawCatch.id] = rawCatch.tripId;
        const aCatch: CatchBean = TripsService.backendCatchToCatchBean(
          moment(),
          rawCatch
        );
        catchs.push(aCatch);
      });
      const entry: TopEntry = new TopEntry(species, catchs);
      result.push(entry);
    });
    return result;
  }

  onTopSizeSelected(item: OptionItem) {
    this.topBySizeCatchs = item.whatever;
  }

  onTopWeightSelected(item: OptionItem) {
    this.topByWeightCatchs = item.whatever;
  }

  onMonthlySizeSelected(item: OptionItem) {
    // Convert Pairs of Avg size / count into average per maillage
    const monthlySizes = {};
    this.maillages = [];
    // @ts-ignore
    Object.keys(item.whatever).forEach((month) => {
      // @ts-ignore
      monthlySizes[month] = {};
      // @ts-ignore
      Object.keys(item.whatever[month]).forEach((maillage) => {
        if (this.maillages.indexOf(maillage as Maillage) == -1) {
          this.maillages.push(maillage as Maillage);
        }
        // @ts-ignore
        const countAndAverage = item.whatever[month][maillage];
         // @ts-ignore
        monthlySizes[month][maillage] = Object.values(countAndAverage)[0];
      });
    });
    this.monthlySizes = monthlySizes;
    this.maillages = this.maillages.sort();
  }

  onMonthlyCountSelected(item: OptionItem) {
    // Convert Pairs of Avg size / count into total count
    const monthlyCount = {};
    // @ts-ignore
    Object.keys(item.whatever).forEach((month) => {
      // @ts-ignore
      monthlyCount[month] = {};
      // @ts-ignore
      Object.keys(item.whatever[month]).forEach((maillage) => {
        // @ts-ignore
        const countAndAverage = item.whatever[month][maillage];
         // @ts-ignore
        const previousCount = monthlyCount[month]["NON_DEFINI"] ?? 0;
        const newCount = previousCount + parseInt(Object.keys(countAndAverage)[0]);
         // @ts-ignore
        monthlyCount[month]["NON_DEFINI"] = newCount;
      });
    });
    this.monthlyCount = monthlyCount;
  }

  openCatch(catchId: string) {
    RouterUtils.pushRouteNoDuplicate(this.$router, {
      name: "catch",
      params: { tripId: this.catchToTripId[catchId], catchId: catchId },
    });
  }

  openGalery() {
    RouterUtils.pushRouteNoDuplicate(this.$router, "/galery");
  }

  getDay(date: number) {
    return new Date(date).getDate();
  }

  getMonth(date: number) {
    const month = new Date(date).getMonth();
    const result = Constants.MONTHS[month];
    return result;
  }

  getYear(date: number) {
    return new Date(date).getFullYear();
  }

  isActiveMonth(month: Month) {
    const today = new Date();
    const monthNames: Month[] = [
      "JANUARY", "FEBRUARY", "MARCH", "APRIL",
      "MAY", "JUNE", "JULY", "AUGUST",
      "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER"
    ];
    const currentMonthName = monthNames[today.getMonth()];
    return this.year == today.getFullYear() && currentMonthName == month;

  }
}
</script>

<style scoped lang="less">
@import (reference) "../../less/_statistics-card";

.average {
  position: relative;
  margin-top: @vertical-margin-small;
  width: 100%;
  display: flex;
  flex-direction: row;
  justify-content: space-between;

  .average-threshold {
    position: absolute;
    width: calc(100% + 2 * @margin-large);
    left: calc(-1 * @margin-large);
    height: 0px;
    border: 1px solid @accent-text;

    @media screen and (min-width: @desktop-min-width) and (max-width: 1073px) {
      width: calc(100% + 2 * @margin-large-desktop);
      left: calc(-1 * @margin-large-desktop);
    }
  }

  .average-column {
    display: flex;
    flex-direction: column;
    align-items: center;

    .count {
      font-weight: bold;
      font-size: @fontsize-small-paragraph;
      color: @accent-text;
    }

    .average-row-bar {
      position: relative;
      margin-top: @vertical-margin-xx-small;
      margin-bottom: @vertical-margin-xx-small;
      height: 150px;
      width: 14px;
      border-radius: 7px;
      background: @solitude;

      .average-row-bar-filled {
        position: absolute;
        bottom: 0px;
        width: 14px;
        border-radius: 7px;

        &.even {
          background: @pelorous;
        }

        &.odd {
          background: @summer-sky;
        }
      }
    }

    .date {
      height: 60px;
      font-size: @fontsize-small-paragraph;
      color: @gunmetal;
      .day {
        font-weight: bold;
      }
    }
  }
}

.dashboard-top-catchs {
  display: flex;
  flex-direction: row;
  align-items: center;
  overflow-x: auto;
  overflow-y: hidden;
  height: 200px;
  margin-bottom: @vertical-margin-medium;
  .galery-preview-list {
    .galery-preview {
      width: 15vw;
      min-width: 150px;
    }
  }
}

.avg-size {
  display: flex;
  gap: 8px;
  justify-content: start;
  align-items: center;
  @media screen and (max-width: 1400px) {
    flex-direction: column;
    align-items: flex-start;
  }

  .card-title {
    align-self: stretch;
  }

  :deep(.maillage-legend) {
    margin: 0 0 15px 2px;
    padding: 0;
  }
}

.card-year {
  flex: none;
  padding: 3px 10px;
  border-radius: 20px;
  background-color: @surface;
  font-size: 13px;
  font-weight: 600;
}

.dashboard-cards {
  display: flex;
  flex-direction: column;
  gap: 25px;
}

// Fonds colorés en alternance, comme les cartes de « Mes statistiques ».
.dashboard-card {
  .statistics-card();
  text-align: left;
}

.tone-sky {
  background-color: @sky-box;
  color: @sky-text;
}

.tone-green {
  background-color: @green-box;
  color: @green-text;
}

.tone-yellow {
  background-color: @yellow-box;
  color: @yellow-text;
}

.card-title {
  .statistics-card-title();
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  margin: 0 0 15px 2px;
}

// Graphiques partagés avec « Statistiques » : recolorés ici seulement, aux
// teintes des cartes de « Mes statistiques ». Les sélecteurs reprennent ceux
// des composants pour l'emporter sur eux.
.dashboard-card {
  :deep(.options-list) {
    padding-left: 0;
    padding-right: 0;

    div.item.selected {
      color: inherit;
      border-bottom-color: @carnet-sky;
    }
  }

  :deep(.distribution .distribution-row) {
    .distribution-row-data .species {
      color: inherit;
    }

    .distribution-row-data .percent .green-label {
      color: @green-text;
    }

    .distribution-row-bar {
      background: @white-smoke;

      .distribution-row-bar-filled.even,
      .distribution-row-bar-filled.odd {
        background: @carnet-sky;
      }

      .distribution-row-bar-filled.green {
        background: @carnet-green;
      }
    }
  }

  :deep(.histogram .bars .bar) {
    background-color: @white-smoke;

    .bar-filled.maillee.even,
    .bar-filled.maillee.odd {
      background: @carnet-sky;
    }

    .bar-filled.non-maillee.even,
    .bar-filled.non-maillee.odd {
      background: @carnet-orange;
    }

    .bar-filled.is-active-month {
      background: @carnet-yellow !important;
    }
  }

  :deep(.histogram .labels) {
    .label-short:not(.is-active-month),
    .label-mid:not(.is-active-month) {
      color: inherit;
    }
  }

  :deep(.maillage-legend .maillage) {
    .square.square-maillee {
      background-color: @carnet-sky;
    }

    .square.square-non-maillee {
      background-color: @carnet-orange;
    }
  }
}
</style>
