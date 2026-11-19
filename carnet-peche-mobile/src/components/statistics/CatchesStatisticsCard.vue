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
  <section class="catches-card">
    <h2 class="card-title">Prises</h2>
    <div class="card-total">
      <span class="card-count">{{ view.catchesCount }}</span>
      <span class="card-caption">{{ caption }}</span>
    </div>
    <svg
      class="catches-chart"
      :viewBox="'0 0 ' + CHART_WIDTH + ' ' + CHART_HEIGHT"
      preserveAspectRatio="none"
      aria-hidden="true"
    >
      <polygon class="chart-area" :points="areaPoints" />
      <polyline class="chart-line" :points="linePoints" />
    </svg>
    <div class="card-axis">
      <span>{{ view.bars[0].shortLabel }}</span>
      <span>{{ peakLabel }}</span>
      <span>{{ view.bars[view.bars.length - 1].shortLabel }}</span>
    </div>
  </section>
</template>

<script lang="ts">
import { StatisticsView } from "@/services/PersonalStatisticsView";
import { Component, Prop, Vue } from "vue-facing-decorator";

const CHART_WIDTH = 329;
const CHART_HEIGHT = 84;
const CHART_TOP_MARGIN = 3;

/** Carte « Prises » : nombre de poissons et courbe des prises par mois (ou par jour). */
@Component
export default class CatchesStatisticsCard extends Vue {
  @Prop() view!: StatisticsView;

  readonly CHART_WIDTH = CHART_WIDTH;
  readonly CHART_HEIGHT = CHART_HEIGHT;

  get caption(): string {
    const noun = this.view.catchesCount > 1 ? "prises" : "prise";
    return noun + (this.view.period === "season" ? " sur la saison" : " sur 30 jours");
  }

  get peakLabel(): string {
    const peak = this.view.peakBar;
    return peak ? "Pic " + peak.longLabel + " : " + peak.catches : "Aucune prise";
  }

  get linePoints(): string {
    const bars = this.view.bars;
    const max = Math.max(1, ...bars.map((bar) => bar.catches));
    const step = CHART_WIDTH / Math.max(1, bars.length - 1);
    const usableHeight = CHART_HEIGHT - CHART_TOP_MARGIN - 2;
    return bars
      .map((bar, index) => {
        const y = CHART_HEIGHT - 2 - (usableHeight * bar.catches) / max;
        return (index * step).toFixed(2) + "," + y.toFixed(2);
      })
      .join(" ");
  }

  get areaPoints(): string {
    return "0," + CHART_HEIGHT + " " + this.linePoints + " " + CHART_WIDTH + "," + CHART_HEIGHT;
  }
}
</script>

<style scoped lang="less">
@import (reference) "../../less/_statistics-card";

.catches-card {
  .statistics-card();
  padding: 17px 17px 20px 20px;
  background-color: @green-box;
  color: @green-text;
}

.card-title {
  .statistics-card-title();
  margin-left: 2px;
}

.card-total {
  .statistics-total();
  margin-top: 13px;
}

.card-count {
  .statistics-count(34px);
}

.card-caption {
  .statistics-caption();
}

.catches-chart {
  display: block;
  width: 100%;
  height: 84px;
  margin-top: 22px;
  overflow: visible;

  .chart-area {
    fill: @carnet-green;
  }

  .chart-line {
    fill: none;
    stroke: @green-text;
    stroke-width: 4.5px;
    stroke-linejoin: round;
    vector-effect: non-scaling-stroke;
  }
}

.card-axis {
  .statistics-axis();
  margin: 10px 0 0 -2px;
}
</style>
