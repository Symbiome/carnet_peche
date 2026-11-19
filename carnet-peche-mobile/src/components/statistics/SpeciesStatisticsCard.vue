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
  <section class="species-card">
    <h2 class="card-title">Espèces</h2>
    <div class="card-total">
      <span class="card-count">{{ view.speciesCount }}</span>
      <span class="card-caption">{{ caption }}</span>
    </div>
    <div class="species-rows">
      <StatisticsProgressRow
        v-for="(row, index) in view.species"
        :key="row.label"
        :row="row"
        :tone="'shade-' + index"
      />
    </div>
  </section>
</template>

<script lang="ts">
import StatisticsProgressRow from "@/components/statistics/StatisticsProgressRow.vue";
import { StatisticsView } from "@/services/PersonalStatisticsView";
import { Component, Prop, Vue } from "vue-facing-decorator";

/** Carte « Espèces » : nombre d'espèces et prises des plus pêchées. */
@Component({ components: { StatisticsProgressRow } })
export default class SpeciesStatisticsCard extends Vue {
  @Prop() view!: StatisticsView;

  get caption(): string {
    return this.view.speciesCount > 1 ? "espèces différentes" : "espèce";
  }
}
</script>

<style scoped lang="less">
@import (reference) "../../less/_statistics-card";

.species-card {
  .statistics-card();
  padding: 21px 20px 35px 20px;
  background-color: @yellow-box;
  color: @yellow-text;
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

.species-rows {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 23px;
}
</style>
