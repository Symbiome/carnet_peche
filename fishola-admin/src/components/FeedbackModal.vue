<!--
  #%L
  Carnet de pêche :: Admin
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
  <b-modal v-model="active" has-modal-card trap-focus aria-role="dialog" aria-modal>
    <div class="modal-card feedback-card">
      <header class="modal-card-head">
        <p class="modal-card-title">Signaler un problème ou une idée</p>
        <button type="button" class="delete" aria-label="Fermer" @click="active = false" />
      </header>

      <section class="modal-card-body">
        <p class="feedback-intro">
          Un bug, une remarque, une idée ? Ce formulaire nous parvient directement
          et nous aide à classer et traiter votre retour.
        </p>

        <b-field label="Catégorie">
          <div class="choices">
            <b-radio v-for="option in CATEGORIES" :key="option" v-model="category" name="feedback-category" :native-value="option">
              {{ option }}
            </b-radio>
          </div>
        </b-field>

        <b-field label="Module concerné">
          <div class="choices">
            <b-radio v-for="option in MODULES" :key="option" v-model="module" name="feedback-module" :native-value="option">
              {{ option }}
            </b-radio>
          </div>
        </b-field>

        <b-field label="Description">
          <b-input v-model="description" type="textarea" placeholder="Décrivez ce que vous avez constaté ou ce que vous proposez" />
        </b-field>

        <b-field label="Capture d'écran (optionnel)" :type="screenshotError ? 'is-danger' : ''" :message="screenshotError">
          <div v-if="screenshot" class="screenshot-preview">
            <img :src="screenshot" alt="Capture d'écran jointe" />
            <b-button size="is-small" type="is-danger" outlined @click="removeScreenshot">Retirer</b-button>
          </div>
          <b-upload v-else v-model="screenshotFile" accept="image/*" drag-drop expanded>
            <div class="screenshot-drop">
              <b-icon icon="image-plus" size="is-medium" />
              <span>{{ screenshotLoading ? "Chargement…" : "Glissez une capture ici ou cliquez pour la choisir" }}</span>
            </div>
          </b-upload>
        </b-field>

        <b-field label="E-mail">
          <b-input v-model="email" type="email" />
        </b-field>
        <p class="feedback-version">Version de l'administration : {{ frontendVersion }}</p>
      </section>

      <footer class="modal-card-foot feedback-actions">
        <b-button @click="active = false">Annuler</b-button>
        <b-button type="is-primary" icon-left="send" :disabled="!canSend" :loading="sending" @click="send">
          Envoyer
        </b-button>
      </footer>
    </div>
  </b-modal>
</template>

<script setup lang="ts">
import BackendService from "@/services/BackendService";
import { resizeScreenshot } from "@/utils/screenshot";
import { BButton, BField, BIcon, BInput, BModal, BRadio, BUpload, useToast } from "buefy";
import { computed, ref, watch } from "vue";

/**
 * Formulaire de retour des testeurs de l'administration : même envoi que
 * « Des retours ? » de l'app pêcheur (e-mail à l'équipe), classé par
 * catégorie et par module, avec une capture d'écran facultative.
 */

const CATEGORIES = [
  "Bug",
  "Ergonomie",
  "Contenu",
  "Fonctionnalité manquante",
  "Performance",
  "Suggestion d'amélioration",
  "Autre",
];

const MODULES = ["Administration", "Imports / exports de données", "Saisie manuelle", "Autre"];

const props = defineProps<{ email: string }>();
const active = defineModel<boolean>("active", { default: false });

const frontendVersion = [
  import.meta.env.VITE__MVN_VERSION,
  import.meta.env.VITE__PACKAGE_JSON_VERSION,
  import.meta.env.VITE__GIT_REVISION,
].join(" ");
const Toast = useToast();

const category = ref("Bug");
const module = ref("");
const description = ref("");
const email = ref("");
const screenshot = ref("");
const screenshotFile = ref<File | null>(null);
const screenshotLoading = ref(false);
const screenshotError = ref("");
const sending = ref(false);

const canSend = computed(() => description.value.trim().length > 0 && !screenshotLoading.value);

watch(active, (isActive) => {
  if (isActive) {
    category.value = "Bug";
    module.value = "";
    description.value = "";
    email.value = props.email;
    removeScreenshot();
  }
});

watch(screenshotFile, async (file) => {
  if (!file) {
    return;
  }
  screenshotError.value = "";
  screenshotLoading.value = true;
  try {
    screenshot.value = await resizeScreenshot(file);
  } catch {
    screenshotError.value = "Cette image ne peut pas être lue. Essayez une capture au format PNG ou JPEG.";
  } finally {
    screenshotLoading.value = false;
    screenshotFile.value = null;
  }
});

/** Retire la capture jointe et son éventuel message d'erreur. */
function removeScreenshot() {
  screenshot.value = "";
  screenshotFile.value = null;
  screenshotError.value = "";
  screenshotLoading.value = false;
}

/** Envoie le retour au backend, qui le transmet par e-mail à l'équipe. */
async function send() {
  sending.value = true;
  try {
    await BackendService.backendPut("/v1/feedback", {
      category: category.value,
      module: module.value || undefined,
      description: description.value,
      email: email.value || undefined,
      userId: props.email || undefined,
      screenshot: screenshot.value || undefined,
      frontendVersion,
      date: new Date(),
      location: window.location.href,
      locationTitle: document.title,
      browser: navigator.userAgent,
      platform: navigator.platform,
      screenResolution: screen.width + "x" + screen.height,
      displaySize: window.innerWidth + "x" + window.innerHeight,
      devicePixelRatio: "" + window.devicePixelRatio,
      locale: navigator.language,
    });
    Toast.open({ message: "Votre retour a été envoyé, merci", type: "is-success" });
    active.value = false;
  } catch (error: any) {
    Toast.open({ message: "Votre retour n'a pas pu être envoyé : " + (error?.message ?? error), type: "is-danger" });
  } finally {
    sending.value = false;
  }
}
</script>

<style scoped lang="less">
.feedback-card {
  width: min(640px, calc(100vw - 32px));
  max-height: calc(100vh - 40px);
  border-radius: 8px;
}

.feedback-intro {
  margin-bottom: 1rem;
}

.choices {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem 1rem;
}

.screenshot-drop {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 1rem;
  text-align: center;
}

.screenshot-preview {
  display: flex;
  align-items: center;
  gap: 1rem;

  img {
    max-width: 200px;
    max-height: 140px;
    border-radius: 4px;
    object-fit: contain;
  }
}

.feedback-version {
  font-size: 0.8rem;
  opacity: 0.7;
}

.feedback-actions {
  justify-content: flex-end;
  gap: 0.5rem;
}
</style>
