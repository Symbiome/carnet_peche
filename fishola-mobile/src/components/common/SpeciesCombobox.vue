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
  <div>
    <FormMultiValues v-if="readonly" v-bind:name="name" v-bind:label="label" v-bind:values="readonlyValues"
      v-bind:readonly="true" />
    <div v-else class="species-combobox">
      <label v-bind:for="'field-' + name">
        {{ label }}
      </label>
      <Combobox v-bind:modelValue="modelValue" v-on:update:modelValue="choose">
        <div class="combobox-root">
          <div class="combobox-anchor" v-bind:class="error ? 'field-error' : ''">
            <ComboboxInput v-bind:id="'field-' + name" v-bind:display-value="nameOf"
              v-bind:placeholder="placeholder" inputmode="search" autocomplete="off"
              v-on:change="search = $event.target.value" v-on:focus="showAll($event.target)"
              v-on:click="open($event.target)" v-on:keydown.esc="search = ''" />
            <ComboboxButton ref="trigger" class="combobox-trigger" aria-label="Voir la liste des espèces"
              v-on:click="search = ''">
              <i class="icon-chevron" />
            </ComboboxButton>
          </div>
          <ComboboxOptions class="combobox-options">
            <li v-if="filteredOptions().length == 0" class="combobox-empty">Aucune espèce ne correspond</li>
            <ComboboxOption v-for="option in filteredOptions()" v-bind:key="option.id" v-bind:value="option.id"
              v-slot="{ active, selected }" as="template">
              <li class="combobox-option" v-bind:class="{ active: active, selected: selected }">
                {{ option.name }}
              </li>
            </ComboboxOption>
          </ComboboxOptions>
        </div>
      </Combobox>
      <div class="combobox-message" v-bind:class="error ? 'field-error' : ''">
        <span v-if="error">
          {{ error }}
        </span>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { SpeciesWithAlias } from "@/pojos/BackendPojos";
import Helpers from "@/services/Helpers";

import FormMultiValues from "@/components/common/FormMultiValues.vue";

import { Combobox, ComboboxButton, ComboboxInput, ComboboxOption, ComboboxOptions } from "@headlessui/vue";
import { Component, Prop, Vue } from "vue-facing-decorator";

/**
 * Champ « Espèce » à autocomplétion (#311) : une seule zone de saisie qui filtre
 * la liste déroulante des espèces sur le nom affiché, l'alias et le nom
 * scientifique, sans tenir compte de la casse ni des accents ; les
 * correspondances exactes et en début de mot passent en premier (#197).
 *
 * Construit sur le Combobox de Headless UI (rôle `combobox`, navigation au
 * clavier et au doigt). Le filtrage reste le nôtre (`Helpers.rankBySearch`).
 */
@Component({
  emits: ["update:modelValue"],
  components: {
    Combobox,
    ComboboxButton,
    ComboboxInput,
    ComboboxOption,
    ComboboxOptions,
    FormMultiValues,
  },
})
export default class SpeciesCombobox extends Vue {
  @Prop() name!: string;
  @Prop() label?: string;
  @Prop() options!: SpeciesWithAlias[];
  @Prop() modelValue?: string;
  @Prop() error?: string;
  @Prop() readonly?: boolean;
  @Prop({ default: "Nom usuel ou nom scientifique" }) placeholder?: string;
  /** Option toujours proposée en fin de liste, même quand la saisie ne la désigne pas (« Autre »). */
  @Prop() pinnedOptionId?: string;

  /**
   * Texte tapé par le pêcheur. Vidé à l'ouverture de la liste, qui est alors complète,
   * au choix d'une espèce et à Échap, quand le champ réaffiche l'espèce choisie.
   */
  search: string = "";

  get readonlyValues(): string[] {
    const name = this.nameOf(this.modelValue);
    return name ? [name] : [];
  }

  nameOf(speciesId?: string): string {
    const option = (this.options || []).find((o) => o.id == speciesId);
    return option ? option.name : "";
  }

  choose(speciesId: string) {
    this.search = "";
    this.$emit("update:modelValue", speciesId);
  }

  /** Liste complète à l'ouverture ; le nom affiché est sélectionné pour être remplacé à la frappe. */
  showAll(input: HTMLInputElement) {
    this.search = "";
    input.select();
  }

  /** Un appui sur le champ ouvre la liste, comme le bouton (Headless UI ne l'ouvre qu'à la frappe). */
  open(input: HTMLInputElement) {
    if (input.getAttribute("aria-expanded") != "true") {
      (this.$refs.trigger as { $el: HTMLElement }).$el.click();
    }
  }

  /** Options correspondant à la saisie, toutes si elle est vide ; l'option épinglée toujours en dernier. */
  filteredOptions(): SpeciesWithAlias[] {
    const options = this.options || [];
    if (!this.search.trim()) {
      return options;
    }
    const ranked = Helpers.rankBySearch(options, this.search, (s) => [s.name, s.alias, s.scientificName])
      .filter((s) => s.id != this.pinnedOptionId);
    const pinned = options.filter((s) => s.id == this.pinnedOptionId);
    return ranked.concat(pinned);
  }
}
</script>

<style scoped lang="less">
.species-combobox {
  margin-top: @vertical-margin-x-small;

  font-size: @fontsize-form-input;
  line-height: calc(@fontsize-form-input + @line-height-padding-medium);

  display: flex;
  flex-direction: column;
  align-items: flex-start;

  label {
    font-weight: 300;
    color: @text-strong;
  }

  .combobox-root {
    position: relative;
    width: 100%;
  }

  .combobox-anchor {
    display: flex;
    align-items: center;
    margin-top: @vertical-margin-xx-small;
    width: 100%;
    height: 38px;
    border-radius: 4px;
    border: 1px solid @pale-sky;

    &.field-error {
      border: 1px solid @cardinal !important;
    }

    input {
      flex: 1;
      min-width: 0;
      height: 100%;
      padding-left: @margin-small;
      padding-right: @margin-small;
      border: none;
      background: transparent;

      color: @gunmetal;
      font-size: @fontsize-form-input;
      font-family: @font-family;

      &::placeholder {
        font-style: italic;
        font-weight: normal;
        font-size: @fontsize-form-input;
        color: @pale-sky;
      }
    }
  }

  .combobox-trigger {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 44px;
    height: 100%;
    border: none;
    border-left: 1px solid @pale-sky;
    border-radius: 0 4px 4px 0;
    background: @solitude;
    color: @gunmetal;
    cursor: pointer;

    i {
      font-size: 12px;
    }

    &:hover {
      color: @accent-text;
    }
  }

  .combobox-options {
    list-style: none;
    padding: 0;
    position: absolute;
    top: 100%;
    left: 0;
    width: 100%;
    max-height: 250px;
    overflow-y: auto;
    margin: 2px 0 0;
    background-color: @surface;
    border-radius: 4px;
    box-shadow: 0 0 5px #0004;
    z-index: 100;
  }

  .combobox-option,
  .combobox-empty {
    padding: 8px @margin-small;
    color: @gunmetal;
  }

  .combobox-option {
    cursor: pointer;

    &.active {
      background-color: @solitude;
    }

    &.selected {
      color: @accent-text;
      font-weight: 600;
    }
  }

  .combobox-empty {
    font-style: italic;
    color: @pale-sky;
  }

  .combobox-message {
    height: calc(@fontsize-form-error + @line-height-padding-medium);
  }

  .combobox-message.field-error {
    background-color: transparent;
    color: @cardinal;
    font-size: @fontsize-form-error;
    line-height: calc(@fontsize-form-error + @line-height-padding-medium);
  }

  @media screen and (min-width: @desktop-min-width) {
    font-size: @fontsize-form-input-desktop;
    line-height: calc(@fontsize-form-input-desktop + @line-height-padding-medium);

    .combobox-anchor {
      height: 42px;

      input {
        font-size: @fontsize-form-input-desktop;

        &::placeholder {
          font-size: @fontsize-form-input-desktop;
        }
      }
    }
  }
}
</style>
