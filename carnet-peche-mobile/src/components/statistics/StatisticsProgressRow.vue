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
  <div class="progress-row" :class="'layout-' + layout">
    <span class="row-label">{{ row.label }}</span>
    <div class="row-track" aria-hidden="true">
      <div class="row-fill" :class="'tone-' + tone" :style="{ width: fillWidth }"></div>
    </div>
    <span class="row-value">
      <strong v-if="emphasis">{{ emphasis }}</strong>{{ rest }}
    </span>
    <span v-if="row.hint" class="row-hint">{{ row.hint }}</span>
  </div>
</template>

<script lang="ts">
import { ProgressRow } from "@/services/PersonalStatisticsView";
import { Component, Prop, Vue } from "vue-facing-decorator";

/**
 * Ligne libellé / valeur avec barre de progression des statistiques.
 * `inline` : libellé, barre et valeur sur une ligne (moments de sortie) ;
 * `stacked` : libellé et valeur, barre dessous, puis l'aide éventuelle.
 * Avec `emphasizeFirst`, le premier mot de la valeur (un pourcentage) est
 * mis en avant.
 */
@Component
export default class StatisticsProgressRow extends Vue {
  @Prop() row!: ProgressRow;
  @Prop({ default: "stacked" }) layout!: "inline" | "stacked";
  @Prop({ default: "sky" }) tone!: string;
  @Prop({ default: false }) emphasizeFirst!: boolean;

  get fillWidth(): string {
    return Math.round(Math.min(1, Math.max(0, this.row.ratio)) * 1000) / 10 + "%";
  }

  get emphasis(): string {
    return this.emphasizeFirst ? this.row.value.split(" ")[0] : "";
  }

  get rest(): string {
    return this.emphasizeFirst ? this.row.value.substring(this.emphasis.length) : this.row.value;
  }
}
</script>

<style scoped lang="less">
.progress-row {
  display: grid;
  align-items: center;
  line-height: 1.2;
  letter-spacing: @letter-spacing;
  font-size: 14px;
}

.row-track {
  height: 10px;
  border-radius: 100px;
  background-color: @white-smoke;
  overflow: hidden;
}

.row-fill {
  height: 100%;
  border-radius: 100px;

  &.tone-sky { background-color: fade(@carnet-sky, 75%); }
  &.tone-shade-0 { background-color: #7F5F00; }
  &.tone-shade-1 { background-color: #B28500; }
  &.tone-shade-2 { background-color: #E5AB00; }
  &.tone-shade-3 { background-color: #FFBE00; }
  &.tone-shade-4 { background-color: #FFC929; }
  &.tone-shade-5 { background-color: #FFD452; }
}

.layout-inline {
  grid-template-columns: 91px 1fr 35px;
  height: 30px;

  .row-label {
    color: @pale-sky;
  }

  .row-value {
    color: @heading;
    font-weight: 500;
    text-align: right;
  }
}

.layout-stacked {
  grid-template-columns: 1fr auto;
  grid-template-areas:
    "label value"
    "track track"
    "hint hint";
  column-gap: 10px;

  .row-label {
    grid-area: label;
    font-weight: 600;
  }

  .row-value {
    grid-area: value;
    text-align: right;

    strong {
      font-size: 16px;
      font-weight: 600;
    }
  }

  .row-track {
    grid-area: track;
    margin-top: 8px;
  }

  .row-hint {
    grid-area: hint;
    margin-top: 7px;
    color: @pale-sky;
    font-size: 11px;
    line-height: 1.4;
  }
}
</style>
