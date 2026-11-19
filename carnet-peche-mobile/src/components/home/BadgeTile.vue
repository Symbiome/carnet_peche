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
  <div class="badge-tile" :class="{ locked: locked }" v-on:click="$emit('clicked')">
    <span class="badge-emoji">{{ emoji }}</span>
    <span class="badge-label">{{ label }}</span>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from "vue-facing-decorator";

/** Tuile de badge de l'accueil : emoji et nom, grisés tant que verrouillé. */
@Component({ emits: ["clicked"] })
export default class BadgeTile extends Vue {
  @Prop() emoji!: string;
  @Prop() label!: string;
  @Prop({ default: false }) locked!: boolean;
}
</script>

<style scoped lang="less">
.badge-tile {
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 123px;
  padding: 10px 6px 0 6px;
  border-radius: 11px;
  background-color: @surface;
  color: @heading;
  text-align: center;
  cursor: pointer;
  overflow: hidden;
  -webkit-tap-highlight-color: transparent;

  .badge-emoji {
    font-size: 44px;
    line-height: 57px;
  }

  .badge-label {
    margin-top: 2px;
    font-size: 18px;
    font-weight: 600;
    line-height: 1.1;
    letter-spacing: @letter-spacing;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  &.locked {
    .badge-emoji {
      filter: grayscale(100%);
      opacity: 0.4;
    }

    .badge-label {
      color: @pale-sky;
    }
  }
}
</style>
