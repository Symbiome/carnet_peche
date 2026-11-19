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
  <section class="water-card">
    <h2 class="card-title">Cours d’eau</h2>
    <div class="card-total">
      <span class="card-count">{{ waterEntities.length }}</span>
      <span class="card-caption">{{ caption }}</span>
    </div>
    <ul class="water-chips" v-if="waterEntities.length">
      <li class="water-chip" v-for="waterEntity in shownWaterEntities" :key="waterEntity.id">
        <span class="chip-name">{{ waterEntity.name }}</span>
        <template v-if="waterEntity.sparselyDocumented">
          <img class="chip-dot" src="img/carnet/dot-yellow.svg" alt="" />
          <span class="chip-flag">Peu documenté</span>
        </template>
      </li>
      <li class="water-chip more" v-if="hiddenCount > 0">+ {{ hiddenLabel }}</li>
    </ul>
    <p class="card-hint">
      Vos sorties sur un cours d’eau peu documenté comptent doublement pour la connaissance du bassin.
    </p>
  </section>
</template>

<script lang="ts">
import { FrequentedWaterEntity } from "@/pojos/BackendPojos";
import { plural } from "@/services/PersonalStatisticsView";
import { Component, Prop, Vue } from "vue-facing-decorator";

const MAX_CHIPS = 10;

/**
 * Carte « Cours d'eau » : milieux des sorties, les peu documentés signalés ;
 * au-delà de dix, seuls les plus fréquentés sont nommés.
 */
@Component
export default class WaterEntitiesStatisticsCard extends Vue {
  @Prop({ default: () => [] }) waterEntities!: FrequentedWaterEntity[];

  get shownWaterEntities(): FrequentedWaterEntity[] {
    return this.waterEntities.slice(0, MAX_CHIPS);
  }

  get hiddenCount(): number {
    return Math.max(0, this.waterEntities.length - MAX_CHIPS);
  }

  get hiddenLabel(): string {
    return plural(this.hiddenCount, "autre");
  }

  get caption(): string {
    return "cours d’eau " + (this.waterEntities.length > 1 ? "enregistrés" : "enregistré");
  }
}
</script>

<style scoped lang="less">
@import (reference) "../../less/_statistics-card";

.water-card {
  .statistics-card();
  padding-bottom: 23px;
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

.water-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 13px 11px;
  margin: 15px 0 0 3px;
  padding: 0;
  list-style: none;
}

.water-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  max-width: 100%;
  padding: 10px;
  border: 1px solid @sky-text;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;

  .chip-name {
    min-width: 0;
    overflow-wrap: break-word;
    word-break: break-word;
  }

  .chip-dot {
    flex-shrink: 0;
    width: 9px;
    height: 9px;
  }

  .chip-flag {
    flex-shrink: 0;
    font-size: 11px;
    font-weight: 400;
    white-space: nowrap;
  }

  &.more {
    border-style: dashed;
  }
}

.card-hint {
  .statistics-hint();
  margin: 23px 0 0 1px;
  color: @sky-text;
  opacity: 0.7;
}
</style>
