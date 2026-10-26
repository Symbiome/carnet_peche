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
  <section class="regularity-card">
    <h2 class="card-title">Régularité</h2>
    <p class="card-hint">{{ hint }}</p>

    <div class="regularity-cells" :class="'period-' + view.period" aria-hidden="true">
      <div v-for="cell in view.cells" :key="cell.key" class="regularity-cell" :class="'level-' + cell.level"></div>
    </div>
    <div class="card-axis">
      <span>{{ view.bars[0].shortLabel }}</span>
      <span class="regularity-legend">
        Moins
        <span class="legend-swatches">
          <span class="legend-swatch level-none"></span>
          <span class="legend-swatch level-low"></span>
          <span class="legend-swatch level-high"></span>
        </span>
        Plus
      </span>
      <span>{{ view.bars[view.bars.length - 1].shortLabel }}</span>
    </div>

    <dl class="regularity-facts">
      <div class="fact">
        <dt>{{ activeLabel }}</dt>
        <dd><strong>{{ view.activeCellsCount }}</strong> sur {{ elapsedLabel }}</dd>
      </div>
      <div class="fact">
        <dt>Rythme habituel</dt>
        <dd>{{ view.rhythmLabel }}</dd>
      </div>
      <div class="fact" v-if="view.period === 'season'">
        <dt>Mois les plus productifs</dt>
        <dd>{{ view.topMonthsLabel }}</dd>
      </div>
    </dl>

    <h3 class="moments-title">Moments de sortie</h3>
    <StatisticsProgressRow v-for="row in view.moments" :key="row.label" :row="row" layout="inline" />
  </section>
</template>

<script lang="ts">
import StatisticsProgressRow from "@/components/statistics/StatisticsProgressRow.vue";
import { StatisticsView } from "@/services/PersonalStatisticsView";
import { Component, Prop, Vue } from "vue-facing-decorator";

/**
 * Carte « Régularité » : frise des semaines (saison) ou des jours (30 jours)
 * avec sortie, rythme habituel, mois les plus productifs et moments de sortie.
 */
@Component({ components: { StatisticsProgressRow } })
export default class RegularityStatisticsCard extends Vue {
  @Prop() view!: StatisticsView;

  get hint(): string {
    return this.view.period === "season"
      ? "Une semaine est active dès qu’une sortie y est enregistrée."
      : "Un jour est actif dès qu’une sortie y est enregistrée.";
  }

  get activeLabel(): string {
    return this.view.period === "season" ? "Semaines actives" : "Jours actifs";
  }

  get elapsedLabel(): string {
    return this.view.elapsedCellsCount + (this.view.period === "season" ? " écoulées" : "");
  }
}
</script>

<style scoped lang="less">
@import (reference) "../../less/_statistics-card";

.regularity-card {
  .statistics-card();
  padding-top: 23px;
  padding-bottom: 29px;
  background-color: @surface;
  color: @heading;
}

.card-title {
  .statistics-card-title();
}

.card-hint {
  .statistics-hint();
  margin-top: 12px;
}

.regularity-cells {
  display: flex;
  justify-content: space-between;
  height: 16px;
  margin-top: 20px;

  .regularity-cell {
    flex: 0 0 4px;
    border-radius: 1px;
  }

  &.period-30d {
    gap: 3px;

    .regularity-cell {
      flex: 1 1 0;
      border-radius: 2px;
    }
  }
}

.level-none { background-color: @sky-box; }
.level-low { background-color: @carnet-sky; }
.level-high { background-color: @accent-text; }
.level-current { background-color: @carnet-yellow; }

.card-axis {
  .statistics-axis();
  margin-top: 10px;
  color: @pale-sky;
  font-size: 13.6px;
}

.regularity-legend {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 14px;
  font-weight: 400;

  .legend-swatches {
    display: flex;
    gap: 3px;
  }

  .legend-swatch {
    width: 11px;
    height: 11px;
    border-radius: 2px;
  }
}

.regularity-facts {
  margin: 21px 2px 0 1px;
  border-bottom: 1px solid @divider;

  .fact {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
    height: 47px;
    border-top: 1px solid @divider;
  }

  dt {
    color: @pale-sky;
    font-size: 14px;
  }

  dd {
    margin: 0;
    font-size: 14px;
    font-weight: 500;
    text-align: right;

    strong {
      color: @accent-text;
      font-weight: 600;
    }
  }
}

.moments-title {
  margin: 21px 0 9px 1px;
  font-size: 14px;
  font-weight: 600;
}
</style>
