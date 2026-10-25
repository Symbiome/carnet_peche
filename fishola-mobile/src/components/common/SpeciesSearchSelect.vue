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
  <div class="species-search-select">
    <FormInput v-if="!readonly" v-bind:name="name + 'Search'" label="Rechercher une espèce" type="text"
      placeholder="Nom usuel ou nom scientifique" v-model="search" />
    <FormSelect v-bind:name="name" v-bind:label="label" v-bind:options="filteredOptions()"
      v-bind:modelValue="modelValue" v-on:update:modelValue="choose"
      v-bind:error="error" v-bind:readonly="readonly" />
  </div>
</template>

<script lang="ts">
import { SpeciesWithAlias } from "@/pojos/BackendPojos";
import Helpers from "@/services/Helpers";

import FormInput from "@/components/common/FormInput.vue";
import FormSelect from "@/components/common/FormSelect.vue";

import { Component, Prop, Vue } from "vue-facing-decorator";

/**
 * Liste déroulante d'espèces avec recherche textuelle (#92, #305) : saisie d'une
 * capture et espèces recherchées d'une sortie. La recherche porte sur le nom
 * affiché, l'alias et le nom scientifique ; les correspondances exactes et en
 * début de mot passent en premier (#197).
 */
@Component({
  emits: ["update:modelValue"],
  components: {
    FormInput,
    FormSelect,
  },
})
export default class SpeciesSearchSelect extends Vue {
  @Prop() name!: string;
  @Prop() label?: string;
  @Prop() options!: SpeciesWithAlias[];
  @Prop() modelValue?: string;
  @Prop() error?: string;
  @Prop() readonly?: boolean;
  /** Choix multiple : la recherche est vidée après chaque espèce choisie. */
  @Prop({ default: false }) clearSearchOnChoice!: boolean;

  search: string = "";

  choose(speciesId: string) {
    if (this.clearSearchOnChoice) {
      this.search = "";
    }
    this.$emit("update:modelValue", speciesId);
  }

  /** Options correspondant à la recherche, toutes si elle est vide. */
  filteredOptions(): SpeciesWithAlias[] {
    if (!this.search.trim()) {
      return this.options;
    }
    return Helpers.rankBySearch(this.options, this.search, (s) => [
      s.name,
      s.alias,
      s.scientificName,
    ]);
  }
}
</script>
