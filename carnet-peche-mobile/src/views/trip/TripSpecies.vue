<!--
  #%L
  Carnet de pêche :: Mobile
  %%
  Copyright (C) 2019 - 2021 INRAE - UMR CARRTEL
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
  <div class="edit-trip-species page-with-header-and-footer shifted-background">
    <CarnetHeader />
    <div class="edit-trip-species-page page">
      <SomeTripHeader v-bind:trip="trip" class="hide-on-desktop" />
      <div class="pane">
        <div class="pane-content rounded">
          <h1 class="no-margin-pane">
            <BackButton class="hide-on-mobile" />
            Espèce recherchée
          </h1>
          <div class="trip-species-form">
            <button type="button" class="all-species-button" v-bind:class="{ selected: trip.allSpecies }"
              v-bind:aria-pressed="trip.allSpecies ? 'true' : 'false'" v-on:click="toggleAllSpecies">
              <i class="icon-check" v-if="trip.allSpecies" />
              Toutes espèces
            </button>
            <SpeciesSearchSelect name="species" label="Ajouter une espèce" v-bind:options="speciesOptions()"
              v-model="speciesChoice" v-bind:clearSearchOnChoice="true" />
            <ul class="selected-species" v-if="trip.speciesIds.length > 0">
              <li v-for="s in selectedSpecies()" v-bind:key="s.id">
                <span>
                  {{ displayName(s) }}
                  <span v-if="s.scientificName" class="scientific-name">{{ s.scientificName }}</span>
                </span>
                <button type="button" class="remove-species" v-bind:aria-label="'Retirer ' + displayName(s)"
                  v-on:click="removeSpecies(s.id)">×</button>
              </li>
            </ul>
            <div class="other-species" v-if="hasOtherSpecies">
              <FormInput name="other" label="Autre espèce" placeholder="Renseignez l'espèce"
                v-model="trip.otherSpecies" />
              <button type="button" class="remove-species" aria-label="Retirer l'espèce autre"
                v-on:click="removeOtherSpecies">×</button>
            </div>
            <div class="info" v-if="hasOtherSpecies">
              Séparez les espèces par une virgule (&nbsp;,&nbsp;) pour en
              renseigner plusieurs
            </div>
          </div>

          <div class="buttons-bar hide-on-mobile">
            <div class="button button-primary">
              <button v-on:click="next">Suivant</button>
            </div>
            <div class="button button-secondary">
              <button v-on:click="giveup">Abandon</button>
            </div>
          </div>

          <div class="bottom-page-spacer keyboardSensitive"></div>
        </div>
      </div>
    </div>
    <CarnetFooter v-bind:button-text="buttonLabel" v-on:buttonClicked="next"
      :isWaitingForPositionBeforeGoingToNextPage="isWaitingForPositionBeforeGoingToNextPage
        " shortcuts="back,step-2-4,giveup" />
  </div>
</template>

<script lang="ts">
import TripSpecies from "@/pojos/TripSpecies";
import { SpeciesWithAlias } from "@/pojos/BackendPojos";
import Constants from "@/services/Constants";
import Helpers from "@/services/Helpers";
import TripsService from "@/services/TripsService";
import ReferentialService from "@/services/ReferentialService";

import BackButton from "@/components/common/BackButton.vue";
import FormInput from "@/components/common/FormInput.vue";
import SpeciesSearchSelect from "@/components/common/SpeciesSearchSelect.vue";
import CarnetHeader from "@/components/layout/CarnetHeader.vue";
import SomeTripHeader from "@/components/trip/SomeTripHeader.vue";
import CarnetFooter from "@/components/layout/CarnetFooter.vue";

import { Component, Prop, Vue, Watch } from "vue-facing-decorator";
import orderBy from "lodash/orderBy";
import EventBus from "@/services/EventBus";
import router from "../../router";
import { RouterUtils } from "@/router/RouterUtils";

const OTHER_SPECIES_ID = "__other__";

@Component({
  components: {
    BackButton,
    FormInput,
    SpeciesSearchSelect,
    CarnetHeader,
    SomeTripHeader,
    CarnetFooter,
  },
})
export default class TripSpeciesView extends Vue {
  @Prop() id!: string;

  trip: TripSpecies = {
    id: "",
    speciesIds: [],
    lakeId: "",
    mode: "Live",
    startedAt: "",
    otherSpecies: "",
    allSpecies: false,
  };

  hasOtherSpecies: boolean = false;
  speciesChoice: string = "";

  species: SpeciesWithAlias[] = [];
  allSpecies: SpeciesWithAlias[] = [];

  buttonLabel: string;

  isWaitingForPositionBeforeGoingToNextPage = false;

  created() {
    this.buttonLabel =
      this.id == Constants.NEW_TRIP_ID ? "Commencer" : "Enregistrer";
    // #130 : la liste ne dépend plus du plan d'eau (le référentiel
    // species-per-waterEntity échouait à l'échelle du bassin RM&C) — on
    // charge simplement toutes les espèces de la table species.
    ReferentialService.getAllSpecies().then(this.speciesLoaded);
  }

  sortedSpecies(): SpeciesWithAlias[] {
    const manualSpecies = localStorage.getItem("manual-species") ?? "";
    return orderBy(this.species, "name").filter(s => s.present || manualSpecies.indexOf(s.id) > -1);
  }

  speciesLoaded(species: SpeciesWithAlias[]) {
    this.allSpecies = species;
    TripsService.getTrip(this.id, this.tripLoaded);
  }

  tripLoaded(someTrip: TripSpecies) {
    console.debug("Trip chargé", someTrip);
    this.trip = someTrip;
    this.trip.allSpecies = !!someTrip.allSpecies;
    this.species = [];
    this.allSpecies.forEach((s) => {
      const selected = this.trip.speciesIds.indexOf(s.id) != -1;
      // Espèce archivée (#202) : plus proposée, sauf si déjà choisie pour la sortie.
      if ((s.builtIn && !s.archived) || selected) {
        this.species.push(s);
      }
    });
    if (this.trip.otherSpecies) {
      this.hasOtherSpecies = true;
    }
  }

  /** Nom affiché d'une espèce : alias éventuel, puis nom usuel. */
  displayName(s: SpeciesWithAlias): string {
    return s.alias ? `${s.alias} (${s.name})` : s.name;
  }

  /** Options de la liste (#305) : espèces non encore choisies, puis « Autre ». */
  speciesOptions(): SpeciesWithAlias[] {
    const options: SpeciesWithAlias[] = this.sortedSpecies()
      .filter((s) => this.trip.speciesIds.indexOf(s.id) == -1)
      .map((s) => ({ ...s, name: this.displayName(s) }));
    if (!this.hasOtherSpecies) {
      options.push({
        id: OTHER_SPECIES_ID,
        name: "Autre ...",
        builtIn: false,
        mandatorySize: false,
        mandatoryReport: false,
        archived: false,
        authorizedSample: false,
        present: true,
        minSize: 0,
        maxSize: 1000,
      });
    }
    return options;
  }

  /** Espèces choisies, dans l'ordre de la liste. */
  selectedSpecies(): SpeciesWithAlias[] {
    return this.sortedSpecies().filter((s) => this.trip.speciesIds.indexOf(s.id) != -1);
  }

  /** Une espèce choisie dans la liste s'ajoute aux espèces recherchées et désactive « Toutes espèces ». */
  @Watch("speciesChoice")
  speciesChosen(speciesId: string) {
    if (!speciesId) {
      return;
    }
    if (speciesId == OTHER_SPECIES_ID) {
      this.hasOtherSpecies = true;
    } else if (this.trip.speciesIds.indexOf(speciesId) == -1) {
      this.trip.speciesIds.push(speciesId);
    }
    this.trip.allSpecies = false;
    this.$nextTick(() => (this.speciesChoice = ""));
  }

  removeSpecies(speciesId: string) {
    this.trip.speciesIds = this.trip.speciesIds.filter((id) => id != speciesId);
  }

  removeOtherSpecies() {
    this.hasOtherSpecies = false;
    this.trip.otherSpecies = "";
  }

  /** « Toutes espèces » (#305) : aucune espèce ciblée, les espèces choisies sont retirées. */
  toggleAllSpecies() {
    this.trip.allSpecies = !this.trip.allSpecies;
    if (this.trip.allSpecies) {
      this.trip.speciesIds = [];
      this.removeOtherSpecies();
    }
  }

  checkOtherSpecies() {
    // Check for existing species withing other
    const unexistingSpecies: Array<string> = [];
    if (this.trip.otherSpecies) {
      const allCustomSpecies = this.trip.otherSpecies.split(",");
      allCustomSpecies.forEach((customOtherSpecie) => {
        // Search through existing species to see if the entered custom specie would match one of them
        let foundMatchingSpecie = false;
        const customSpecie = this.lowerCaseAndRemovePlural(customOtherSpecie);
        this.species.forEach((existingSpecie) => {
          const existingSpecieWithAlias = existingSpecie as SpeciesWithAlias;
          if (
            this.lowerCaseAndRemovePlural(existingSpecieWithAlias.name) ==
            customSpecie ||
            this.lowerCaseAndRemovePlural(existingSpecieWithAlias.alias) ==
            customSpecie
          ) {
            foundMatchingSpecie = true;
            if (
              this.trip.speciesIds.indexOf(existingSpecieWithAlias.id) == -1
            ) {
              this.trip.speciesIds.push(existingSpecieWithAlias.id);
            }
            let manualSpecies = localStorage.getItem("manual-species") ?? "";
            if (manualSpecies.indexOf(existingSpecieWithAlias.id) == -1) {
             manualSpecies += "," + existingSpecieWithAlias.id;
            }
            localStorage.setItem("manual-species", manualSpecies);
          }
        });

        // If we found an existing specie matching custom name, let's use the existing specie
        if (!foundMatchingSpecie) {
          unexistingSpecies.push(customOtherSpecie.trim());
        }
      });
      this.trip.otherSpecies = unexistingSpecies.join(",");
      if (!this.trip.otherSpecies) {
        this.hasOtherSpecies = false;
      }
    }
  }

  lowerCaseAndRemovePlural(specieName: string | undefined): string {
    let lowerCaseAndPluralRemoved = specieName
      ? specieName
        .toLowerCase()
        .replace("s", "")
        .replace(" ", "")
        .replace("(", "")
        .replace(")", "")
      : "";
    lowerCaseAndPluralRemoved = lowerCaseAndPluralRemoved.replace(
      "chevaine",
      "chevesne"
    );
    return lowerCaseAndPluralRemoved;
  }
  next() {
    this.checkOtherSpecies();
    let hasError = false;

    if (this.hasOtherSpecies) {
      if (!this.trip.otherSpecies) {
        hasError = true;
        EventBus.emit(
          "toaster-error",
          "Vous devez saisir le nom de l'espèce"
        );
      }
    } else {
      this.trip.otherSpecies = "";
    }
    if (this.trip.speciesIds.length == 0 && !this.hasOtherSpecies && !this.trip.allSpecies) {
      hasError = true;
      EventBus.emit(
        "toaster-error",
        "Vous devez sélectionner au moins une espèce ou « Toutes espèces »"
      );
    }

    // if (this.name) {
    //   this.nameError = '';
    // } else {
    //   hasError = true;
    //   this.nameError = 'Information obligatoire';
    // }
    // if (this.type) {
    //   this.typeError = '';
    // } else {
    //   hasError = true;
    //   this.typeError = 'Information obligatoire';
    // }
    if (hasError) {
      //
    } else {
      this.isWaitingForPositionBeforeGoingToNextPage = true;
      TripsService.finishTripBootstrap(this.trip, this.tripSaved);
    }
  }

  giveup() {
    Helpers.confirm(
      "Voulez-vous vraiment abandonner cette sortie ?"
    ).then(this.giveupConfirmed);
  }

  giveupConfirmed() {
    TripsService.cancelCreations();
    RouterUtils.pushRouteNoDuplicate(this.$router, "/trips");
  }

  summaryNotYetSaved(tripAsAny: any) {
    // Si on a pas encore la liste des techniques c'est qu'on est pas
    // encore passé par la sauvegarde sur l'écran de résumé #JoeLaBidouille
    return !tripAsAny.techniqueIds;
  }

  tripSaved(savedId: string) {
    this.isWaitingForPositionBeforeGoingToNextPage = false;
    if (
      this.id == Constants.NEW_TRIP_ID ||
      this.summaryNotYetSaved(this.trip)
    ) {
      RouterUtils.pushRouteNoDuplicate(this.$router, {
        name: "trip-catchs",
        params: { id: savedId },
      });
    } else if (this.id == "RUNNING") {
      RouterUtils.pushRouteNoDuplicate(this.$router, {
        name: "trip-summary",
        params: { id: savedId },
      });
    } else {
      RouterUtils.pushRouteNoDuplicate(this.$router, {
        name: "trip",
        params: { id: savedId },
      });
    }
  }
}
</script>

<style lang="less">


.edit-trip-species-page {
  .pane .pane-content {
    padding-left: 0px;
    padding-right: 0px;
  }

  .trip-species-form {
    padding-left: @margin-x-large;
    padding-right: @margin-x-large;
    text-align: left;
  }

  .all-species-button {
    margin-top: @vertical-margin-x-small;
    margin-bottom: @vertical-margin-x-small;
    height: 38px;
    padding-left: @margin-medium;
    padding-right: @margin-medium;
    border-radius: 50px;
    border: 1px solid @accent-text;
    background-color: transparent;
    color: @accent-text;
    font-size: @fontsize-small-paragraph;
    font-weight: 600;

    &.selected {
      border-color: @carnet-blue;
      background-color: @carnet-blue;
      color: @white;
    }
  }

  .selected-species {
    margin: 0px;
    padding: 0px;
    list-style: none;

    display: flex;
    flex-wrap: wrap;
    gap: @margin-x-small;

    li {
      display: flex;
      align-items: center;
      gap: @margin-x-small;

      padding-left: @margin-small;
      border-radius: 50px;
      background-color: @blue-box;
      color: @blue-text;
      font-size: @fontsize-small-paragraph;
      line-height: calc(@fontsize-small-paragraph + @line-height-padding-medium);

      .scientific-name {
        font-style: italic;
        color: @pale-sky;
      }
    }
  }

  .remove-species {
    min-width: 32px;
    height: 32px;
    border: 0px;
    border-radius: 50px;
    background-color: transparent;
    color: @blue-text;
    font-size: @fontsize-paragraph;
  }

  .other-species {
    display: flex;
    align-items: center;
    gap: @margin-x-small;

    > div {
      flex: 1;
    }
  }

  .info {
    font-style: italic;
    font-weight: 300;
    font-size: @fontsize-info;
    line-height: calc(@fontsize-info + @line-height-padding-medium);
    color: @pale-sky;
    text-align: center;
  }

  @media screen and (min-width: @desktop-min-width) {
    .trip-species-form {
      padding-left: @margin-large-desktop;
      padding-right: @margin-large-desktop;
    }

    .info {
      margin-top: 20px;
      font-size: @fontsize-paragraph;
      line-height: calc(@fontsize-paragraph + @line-height-padding-medium);
    }
  }
}
</style>
