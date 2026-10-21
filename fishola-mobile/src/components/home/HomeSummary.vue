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
  <div class="home-summary">
    <div class="summary-header">
      <h2 class="summary-title">Résumé</h2>
      <div class="period-toggle" role="group" aria-label="Période du résumé">
        <button
          v-for="option in periodOptions"
          :key="option.value"
          :class="{ active: period === option.value }"
          :aria-pressed="period === option.value"
          v-on:click="period = option.value"
        >
          {{ option.label }}
        </button>
      </div>
    </div>

    <div class="trips-card" v-on:click="$emit('open')">
      <div class="trips-total">
        <span class="trips-count">{{ summary.tripsCount }}</span>
        <span class="trips-caption">{{ tripsCaption }}</span>
      </div>
      <div class="trips-bars" :class="'period-' + period">
        <div
          v-for="(bucket, index) in summary.buckets"
          :key="bucket.key"
          class="trips-bar"
          :class="{ current: index === summary.buckets.length - 1 }"
          :style="{ height: barHeight(bucket.trips) }"
          :title="bucket.shortLabel + ' : ' + bucket.trips"
        ></div>
      </div>
      <div class="trips-axis">
        <span>{{ firstBucket.shortLabel }}</span>
        <span>{{ lastBucket.shortLabel }}</span>
      </div>
    </div>

    <div class="summary-cards">
      <div class="catches-card" v-on:click="$emit('open')">
        <div class="card-title">Prises</div>
        <div class="card-value">{{ summary.catchesCount }}</div>
        <svg class="catches-sparkline" viewBox="0 0 100 40" preserveAspectRatio="none" aria-hidden="true">
          <polygon class="sparkline-area" :points="sparklineArea" />
          <polyline class="sparkline-line" :points="sparklinePoints" />
        </svg>
        <div class="card-caption">{{ peakCaption }}</div>
      </div>
      <div class="species-card" v-on:click="$emit('open')">
        <div class="card-title">Espèces</div>
        <div class="card-value">{{ speciesCountLabel }}</div>
        <div class="species-bar" aria-hidden="true">
          <div
            v-for="(segment, index) in speciesSegments"
            :key="segment.name"
            class="species-segment"
            :class="'shade-' + index"
            :style="{ flexGrow: segment.count }"
          ></div>
        </div>
        <div class="card-caption">{{ leaderCaption }}</div>
      </div>
    </div>

    <button class="summary-more" v-on:click="$emit('open')">Voir le résumé complet&nbsp;&nbsp;›</button>
  </div>
</template>

<script lang="ts">
import { CatchMarker, TripLight } from "@/pojos/BackendPojos";
import {
  buildHomeSummary,
  HomeSummary as HomeSummaryData,
  SpeciesShare,
  SummaryBucket,
  SummaryPeriod,
} from "@/services/HomeSummaryStats";
import { Component, Prop, Vue } from "vue-facing-decorator";

const MAX_BAR_HEIGHT_PX = 63;
const EMPTY_BAR_HEIGHT_PX = 4;
const MAX_SPECIES_SEGMENTS = 6;

/**
 * Bloc « Résumé » de l'accueil : sorties, prises et espèces sur les 30
 * derniers jours ou les 12 derniers mois.
 */
@Component({ emits: ["open"] })
export default class HomeSummaryBlock extends Vue {
  @Prop({ default: () => [] }) trips!: TripLight[];
  @Prop({ default: () => [] }) markers!: CatchMarker[];
  @Prop({ default: true }) speciesKnown!: boolean;

  period: SummaryPeriod = "12m";
  periodOptions = [
    { value: "30d", label: "30 jours" },
    { value: "12m", label: "12 mois" },
  ];

  get summary(): HomeSummaryData {
    return buildHomeSummary(this.trips, this.markers, this.period);
  }

  get firstBucket(): SummaryBucket {
    return this.summary.buckets[0];
  }

  get lastBucket(): SummaryBucket {
    return this.summary.buckets[this.summary.buckets.length - 1];
  }

  get tripsCaption(): string {
    const noun = this.summary.tripsCount > 1 ? "sorties" : "sortie";
    const span = this.period === "30d" ? "30 jours" : "12 mois";
    return noun + " sur " + span;
  }

  get peakCaption(): string {
    const peak = this.summary.peakBucket;
    return peak ? "Pic " + peak.longLabel : "Aucune prise";
  }

  get speciesCountLabel(): string | number {
    return this.speciesKnown ? this.summary.species.length : "–";
  }

  get leaderCaption(): string {
    const leader = this.summary.species[0];
    if (!this.speciesKnown) {
      return "Hors ligne";
    }
    return leader ? leader.name + " en tête" : "Aucune espèce";
  }

  /** Espèces de la barre empilée, les moins pêchées regroupées à la fin. */
  get speciesSegments(): SpeciesShare[] {
    const species = this.summary.species;
    if (species.length <= MAX_SPECIES_SEGMENTS) {
      return species;
    }
    const head = species.slice(0, MAX_SPECIES_SEGMENTS - 1);
    const rest = species.slice(MAX_SPECIES_SEGMENTS - 1);
    const others = rest.reduce((sum, share) => sum + share.count, 0);
    return [...head, { name: "Autres", count: others }];
  }

  get sparklinePoints(): string {
    const buckets = this.summary.buckets;
    const max = Math.max(1, ...buckets.map((bucket) => bucket.catches));
    const step = 100 / Math.max(1, buckets.length - 1);
    return buckets
      .map((bucket, index) => (index * step).toFixed(2) + "," + (38 - (36 * bucket.catches) / max).toFixed(2))
      .join(" ");
  }

  get sparklineArea(): string {
    return "0,40 " + this.sparklinePoints + " 100,40";
  }

  /** Hauteur d'une barre, proportionnelle au mois (ou jour) le plus chargé. */
  barHeight(trips: number): string {
    const max = Math.max(...this.summary.buckets.map((bucket) => bucket.trips));
    if (!trips || !max) {
      return EMPTY_BAR_HEIGHT_PX + "px";
    }
    const height = Math.max(EMPTY_BAR_HEIGHT_PX, (MAX_BAR_HEIGHT_PX * trips) / max);
    return Math.round(height) + "px";
  }
}
</script>

<style scoped lang="less">
.home-summary {
  .summary-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin: 48px 0 17px 0;
  }

  .summary-title {
    margin: 0 0 0 2px;
    color: @heading;
    font-size: 22px;
    font-weight: 600;
    line-height: 1.2;
    letter-spacing: @letter-spacing;
  }

  .period-toggle {
    display: flex;
    gap: 2px;
    padding: 7.5px 8px 7.5px 5px;
    border-radius: 8.873px;
    background-color: @surface;

    button {
      height: 34px;
      min-width: 70px;
      padding: 0 8px;
      border: none;
      border-radius: 8px;
      background: transparent;
      color: @pale-sky;
      font-size: 14px;
      font-weight: 500;
      letter-spacing: @letter-spacing;

      &.active {
        min-width: 80px;
        background-color: @carnet-sky;
        // Blanc sur ce bleu ciel : 3,5:1, trop peu pour du texte de 14 px.
        color: @carnet-ink;
      }

      &:active {
        animation: none;
      }
    }
  }

  .trips-card,
  .catches-card,
  .species-card {
    border-radius: 13px;
    cursor: pointer;
    -webkit-tap-highlight-color: transparent;
  }

  .trips-card {
    padding: 14px 18px 17px 11px;
    background-color: @sky-box;
    color: @sky-text;
  }

  .trips-total {
    display: flex;
    align-items: baseline;
    gap: 5px;
    line-height: 1.2;
    letter-spacing: @letter-spacing;
  }

  .trips-count {
    font-size: 43px;
    font-weight: 700;
  }

  .trips-caption {
    font-size: 18px;
    font-weight: 500;
  }

  .trips-bars {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    height: 63px;
    margin: 13px 0 0 10px;

    .trips-bar {
      flex: 0 0 19px;
      border-radius: 5px;
      background-color: @carnet-sky;

      &.current {
        background-color: @carnet-yellow;
      }
    }

    &.period-30d .trips-bar {
      flex: 1 1 0;
      margin: 0 1.5px;
      border-radius: 3px;
    }
  }

  .trips-axis {
    display: flex;
    justify-content: space-between;
    margin: 9px 0 0 10px;
    font-size: 14px;
    font-weight: 500;
    line-height: 1.2;
    letter-spacing: @letter-spacing;
  }

  .summary-cards {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 12px;
    margin-top: 20px;
  }

  .catches-card,
  .species-card {
    display: flex;
    flex-direction: column;
    min-height: 172px;
    padding: 17px 13px 18px 11px;
    line-height: 1.2;
    letter-spacing: @letter-spacing;
    overflow: hidden;
  }

  .catches-card {
    background-color: @green-box;
    color: @green-text;
  }

  .species-card {
    background-color: @yellow-box;
    color: @yellow-text;
  }

  .card-title {
    font-size: 18px;
    font-weight: 500;
  }

  .card-value {
    font-size: 34px;
    font-weight: 700;
  }

  .card-caption {
    margin-top: auto;
    font-size: 14px;
    font-weight: 500;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .catches-sparkline {
    width: 100%;
    height: 36px;
    margin: 8px 0 10px 0;
    overflow: visible;

    .sparkline-area {
      fill: @carnet-green;
    }

    .sparkline-line {
      fill: none;
      stroke: @green-text;
      stroke-width: 1.5px;
      vector-effect: non-scaling-stroke;
    }
  }

  .species-bar {
    display: flex;
    gap: 2px;
    height: 14px;
    margin: 30px 0 10px 0;

    .species-segment {
      flex-basis: 0;
      border-radius: 2px;

      &.shade-0 { background-color: #7F5F00; }
      &.shade-1 { background-color: #B28600; }
      &.shade-2 { background-color: #E5AB00; }
      &.shade-3 { background-color: @carnet-yellow; }
      &.shade-4 { background-color: fade(@carnet-yellow, 80%); }
      &.shade-5 { background-color: fade(@carnet-yellow, 60%); }
    }
  }

  .summary-more {
    display: block;
    margin: 26px 4px 0 auto;
    padding: 0;
    border: none;
    background: none;
    color: @heading;
    font-size: 18px;
    font-weight: 500;
    letter-spacing: @letter-spacing;

    &:active {
      animation: none;
    }
  }
}
</style>
