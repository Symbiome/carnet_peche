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
  <section class="quality-card">
    <h2 class="card-title">Qualité de vos données</h2>
    <p class="card-hint">Plus une fiche est complète, plus elle est utile aux scientifiques du bassin.</p>
    <div class="quality-rows">
      <StatisticsProgressRow v-for="row in view.quality" :key="row.label" :row="row" :emphasizeFirst="true" />
    </div>
  </section>
</template>

<script lang="ts">
import StatisticsProgressRow from "@/components/statistics/StatisticsProgressRow.vue";
import { StatisticsView } from "@/services/PersonalStatisticsView";
import { Component, Prop, Vue } from "vue-facing-decorator";

/** Carte « Qualité de vos données » : fiches complètes, photos, saisies dans les 24 h. */
@Component({ components: { StatisticsProgressRow } })
export default class QualityStatisticsCard extends Vue {
  @Prop() view!: StatisticsView;
}
</script>

<style scoped lang="less">
@import (reference) "../../less/_statistics-card";

.quality-card {
  .statistics-card();
  padding: 25px 20px 40px 18px;
  background-color: @surface;
  color: @heading;
}

.card-title {
  .statistics-card-title();
}

.card-hint {
  .statistics-hint();
  margin: 13px 0 0 1px;
}

.quality-rows {
  display: flex;
  flex-direction: column;
  gap: 21px;
  margin: 22px 0 0 2px;
}
</style>
