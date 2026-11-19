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
  <section class="trips-card">
    <h2 class="card-title">Sorties</h2>
    <div class="card-total">
      <span class="card-count">{{ view.tripsCount }}</span>
      <span class="card-caption">{{ caption }}</span>
    </div>
    <div class="trips-bars" :class="'period-' + view.period">
      <div
        v-for="bar in view.bars"
        :key="bar.key"
        class="trips-bar"
        :class="{ current: bar.current }"
        :style="{ height: barHeight(bar.trips) }"
        :title="bar.shortLabel + ' : ' + bar.trips"
      ></div>
    </div>
    <div class="card-axis">
      <span>{{ view.bars[0].shortLabel }}</span>
      <span>{{ view.bars[view.bars.length - 1].shortLabel }}</span>
    </div>
    <p class="card-range">{{ rangeLabel }}</p>
  </section>
</template>

<script lang="ts">
import { frenchDate, StatisticsView } from "@/services/PersonalStatisticsView";
import { Component, Prop, Vue } from "vue-facing-decorator";

const MAX_BAR_HEIGHT_PX = 64;
const EMPTY_BAR_HEIGHT_PX = 4;

/** Jour et mois en toutes lettres : « 1er janvier », « 25 octobre ». */
function longDay(date: Date): string {
  const month = frenchDate(date, { month: "long" });
  return (date.getDate() === 1 ? "1er" : date.getDate()) + " " + month;
}

/** Carte « Sorties » : nombre de sorties et barres par mois (saison) ou par jour (30 jours). */
@Component
export default class TripsStatisticsCard extends Vue {
  @Prop() view!: StatisticsView;

  get caption(): string {
    const noun = this.view.tripsCount > 1 ? "sorties" : "sortie";
    return noun + (this.view.period === "season" ? " cette saison" : " sur 30 jours");
  }

  get rangeLabel(): string {
    const prefix = this.view.period === "season"
      ? "Saison " + this.view.range.from.getFullYear()
      : "30 derniers jours";
    return prefix + " · du " + longDay(this.view.range.from) + " au " + longDay(this.view.range.to);
  }

  /** Hauteur d'une barre, proportionnelle au mois (ou jour) le plus chargé. */
  barHeight(trips: number): string {
    const max = Math.max(...this.view.bars.map((bar) => bar.trips));
    if (!trips || !max) {
      return EMPTY_BAR_HEIGHT_PX + "px";
    }
    return Math.round(Math.max(EMPTY_BAR_HEIGHT_PX, (MAX_BAR_HEIGHT_PX * trips) / max)) + "px";
  }
}
</script>

<style scoped lang="less">
@import (reference) "../../less/_statistics-card";

.trips-card {
  .statistics-card();
  background-color: @sky-box;
  color: @sky-text;
}

.card-title {
  .statistics-card-title();
}

.card-total {
  .statistics-total();
}

.card-count {
  .statistics-count(43px);
}

.card-caption {
  .statistics-caption();
}

.trips-bars {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  height: 64px;
  margin: 14px 9px 0 1px;

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

.card-axis {
  .statistics-axis();
  margin: 11px 4px 0 1px;
}

.card-range {
  margin: 18px 0 0 0;
  font-size: 14px;
  opacity: 0.7;
}
</style>
