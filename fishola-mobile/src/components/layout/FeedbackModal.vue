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
  <div class="feedback-overlay" v-if="display" v-on:click.self="closeFeedback">
    <div class="feedback-dialog" role="dialog" aria-modal="true" aria-labelledby="feedback-title">
      <header class="feedback-header">
        <h2 id="feedback-title">Des retours ?</h2>
        <button class="feedback-close" aria-label="Fermer" v-on:click="closeFeedback">
          <i class="icon-error" />
        </button>
      </header>

      <div class="feedback-body">
        <p class="feedback-intro">
          Un bug, une remarque, une idée ? Remplissez ce formulaire pour nous
          faire parvenir votre avis.
        </p>

        <fieldset class="feedback-choices">
          <legend>Catégorie</legend>
          <label v-for="option in categories" :key="option" class="choice">
            <input type="radio" name="feedback-category" :value="option" v-model="model.category" />
            <span>{{ option }}</span>
          </label>
        </fieldset>

        <fieldset class="feedback-choices">
          <legend>Module concerné</legend>
          <label v-for="option in modules" :key="option" class="choice">
            <input type="radio" name="feedback-module" :value="option" v-model="model.module" />
            <span>{{ option }}</span>
          </label>
        </fieldset>

        <FormTextarea
          name="feedback-description"
          label="Description"
          placeholder="Décrivez ce que vous avez constaté ou ce que vous proposez"
          v-model="model.description"
        />

        <div class="feedback-screenshot">
          <span class="feedback-label">Capture d'écran (optionnel)</span>
          <div v-if="screenshot" class="screenshot-preview">
            <img :src="screenshot" alt="Capture d'écran jointe" />
            <button class="screenshot-remove" v-on:click="removeScreenshot">Retirer</button>
          </div>
          <label v-else class="screenshot-add">
            <input type="file" accept="image/*" v-on:change="screenshotChosen" />
            <i class="icon-photo" />
            {{ screenshotLoading ? "Chargement…" : "Ajouter une capture d'écran" }}
          </label>
          <span v-if="screenshotError" class="screenshot-error">{{ screenshotError }}</span>
        </div>

        <FormInput
          name="feedback-email"
          label="E-mail (optionnel)"
          placeholder="Indiquez votre e-mail pour rester informé"
          v-model="model.email"
        />
        <p class="feedback-version">Version de l'application : {{ frontendVersion }}</p>
      </div>

      <footer class="feedback-actions">
        <div class="button button-secondary">
          <button v-on:click="closeFeedback">Annuler</button>
        </div>
        <div class="button button-primary">
          <button v-on:click="sendClicked" :disabled="!canSend">
            <i class="icon-send" /> Envoyer
          </button>
        </div>
      </footer>
    </div>
  </div>
</template>

<script lang="ts">
import FormInput from "@/components/common/FormInput.vue";
import FormTextarea from "@/components/common/FormTextarea.vue";

import { Feedback } from "@/pojos/BackendPojos";
import UserProfile from "@/pojos/UserProfile";

import EventBus from "@/services/EventBus";
import FeedbackService from "@/services/FeedbackService";
import ProfileService from "@/services/ProfileService";
import { resizeScreenshot } from "@/services/ScreenshotResizer";

import { Device } from "@capacitor/device";
import { Component, Vue } from "vue-facing-decorator";

const CATEGORIES = [
  "Bug",
  "Ergonomie",
  "Contenu",
  "Fonctionnalité manquante",
  "Performance",
  "Suggestion d'amélioration",
  "Autre",
];

const MODULES = ["Mon compte", "Carte", "Saisie des captures", "Badges et récompenses", "Autre"];

/**
 * Fenêtre « Des retours ? » : remontée des bugs et des retours des pêcheurs,
 * classés par catégorie et par module, avec une capture d'écran choisie par
 * l'utilisateur. Les boutons restent visibles, seul le contenu défile.
 */
@Component({
  components: {
    FormInput,
    FormTextarea,
  },
})
export default class FeedbackModal extends Vue {
  display = false;
  mvnVersion: string = import.meta.env.VITE__MVN_VERSION;
  packageJSONVersion: string = import.meta.env.VITE__PACKAGE_JSON_VERSION;
  gitRevision: string = import.meta.env.VITE__GIT_REVISION;
  frontendVersion: string = `${this.mvnVersion} ${this.packageJSONVersion} ${this.gitRevision}`;

  device: string = "";
  categories = CATEGORIES;
  modules = MODULES;

  model: Feedback = this.newModel("Bug");
  screenshot: string = "";
  screenshotLoading = false;
  screenshotError = "";

  get canSend(): boolean {
    return !!(this.model.description && this.model.description.trim()) && !this.screenshotLoading;
  }

  created() {
    Device.getInfo().then((info) => {
      this.device = JSON.stringify(info);
    });
  }

  mounted() {
    EventBus.on("open-feedback", (event) => this.openFeedback(event));
    EventBus.on("close-feedback", this.closeFeedback);
  }

  beforeUnmount() {
    EventBus.off("open-feedback");
    EventBus.off("close-feedback");
  }

  newModel(category: string): Feedback {
    return {
      category: category,
      id: "" + new Date().getTime(),
      frontendVersion: this.frontendVersion,
    };
  }

  openFeedback(feedbackType: string) {
    this.model = this.newModel(feedbackType ? "Autre" : "Bug");
    this.removeScreenshot();
    if (feedbackType == "scale") {
      this.model.description =
        "Bonjour,\nJe souhaiterais devenir collecteur d'écailles salmonidés.\nVoici mon adresse : ...\n";
    } else if (feedbackType == "ambassador") {
      this.model.description =
        "Bonjour,\nJe souhaiterais devenir ambassadeur Carnet de pêche.\n";
    }
    ProfileService.getProfile().then(this.profileLoaded, () => (this.display = true));
  }

  profileLoaded(profile: UserProfile) {
    this.model.email = profile.email;
    this.model.userId = profile.email;
    this.display = true;
  }

  closeFeedback() {
    this.display = false;
  }

  screenshotChosen(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files && input.files[0];
    input.value = "";
    if (!file) {
      return;
    }
    this.screenshotError = "";
    this.screenshotLoading = true;
    resizeScreenshot(file).then(
      (dataUrl) => {
        this.screenshot = dataUrl;
        this.screenshotLoading = false;
      },
      () => {
        this.screenshotError = "Cette image ne peut pas être lue. Essayez une capture au format PNG ou JPEG.";
        this.screenshotLoading = false;
      }
    );
  }

  removeScreenshot() {
    this.screenshot = "";
    this.screenshotError = "";
    this.screenshotLoading = false;
  }

  sendClicked() {
    if (!this.canSend) {
      return;
    }
    if (this.screenshot) {
      this.model.screenshot = this.screenshot;
    }
    this.closeFeedback();
    this.sendFeedback();
  }

  addEnvInfo() {
    this.model.browser = this.getBrowserNameAndVersion();
    this.model.os = this.getOperatingSystemNameAndVersion();
    this.model.platform = navigator.platform;
    this.model.screenResolution = screen.width + "x" + screen.height;
    this.model.displaySize = window.innerWidth + "x" + window.innerHeight;
    this.model.devicePixelRatio = "" + window.devicePixelRatio;
    this.model.locale = navigator.language;
  }

  getOperatingSystemNameAndVersion() {
    const userAgent = navigator.userAgent;
    let os = userAgent;
    if (userAgent.indexOf("Windows NT 10.0") != -1) os = "Windows 10";
    if (userAgent.indexOf("Windows NT 6.3") != -1) os = "Windows 8.1";
    if (userAgent.indexOf("Windows NT 6.2") != -1) os = "Windows 8";
    if (userAgent.indexOf("Windows NT 6.1") != -1) os = "Windows 7";
    if (userAgent.indexOf("Windows NT 6.0") != -1) os = "Windows Vista";
    if (userAgent.indexOf("Windows NT 5.1") != -1) os = "Windows XP";
    if (userAgent.indexOf("Windows NT 5.0") != -1) os = "Windows 2000";
    if (userAgent.indexOf("Mac") != -1) os = "Mac/iOS";
    if (userAgent.indexOf("X11") != -1) os = "UNIX";
    if (userAgent.indexOf("Linux") != -1) os = "Linux";
    if (userAgent.indexOf("Android") != -1) os = "Android";
    return os;
  }

  getBrowserNameAndVersion() {
    const ua = navigator.userAgent;
    let tem,
      M =
        ua.match(
          /(opera|chrome|safari|firefox|msie|trident(?=\/))\/?\s*([\d]+)/i
        ) || [];
    if (/trident/i.test(M[1])) {
      tem = /\brv[ :]+(\d+(\.\d+)?)/g.exec(ua) || [];
      return "IE " + (tem[1] || "");
    }
    M = M[2] ? [M[1], M[2]] : [navigator.appName, navigator.appVersion, "-?"];
    if ((tem = ua.match(/version\/([\d]+)/i)) != null) M[2] = tem[1];
    return M.join(" ");
  }

  sendFeedback() {
    this.addEnvInfo();
    this.model.date = new Date();
    this.model.location = window.location.href;
    this.model.device = this.device;
    FeedbackService.sendFeedback(this.model).then(
      () => EventBus.emit("toaster-success", "Votre retour a été enregistré, merci"),
      () => EventBus.emit("toaster-error", "Votre retour n'a pas pu être enregistré")
    );
  }
}
</script>

<style scoped lang="less">
.feedback-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000; // Au-dessus du menu latéral (998) et de la barre du bas
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: @black-alpha-60;
}

.feedback-dialog {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  background-color: @surface;
  color: @gunmetal;
  letter-spacing: @letter-spacing;

  @media screen and (min-width: @desktop-min-width) {
    width: 640px;
    height: auto;
    max-height: 90vh;
    border-radius: 13px;
    overflow: hidden;
  }
}

.feedback-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex: none;
  padding: calc(14px + env(safe-area-inset-top)) 20px 14px 20px;
  border-bottom: 1px solid @divider;

  @media screen and (min-width: @desktop-min-width) {
    padding-top: 14px;
  }

  h2 {
    margin: 0;
    color: @heading;
    font-size: 20px;
    font-weight: 700;
  }
}

.feedback-close {
  padding: 4px;
  border: none;
  background: none;
  color: @pale-sky;
  font-size: 20px;
  cursor: pointer;
}

.feedback-body {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  padding: 16px 20px;
}

.feedback-intro {
  margin: 0 0 16px 0;
  font-size: 14px;
  line-height: 1.4;
}

.feedback-choices {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 0 0 16px 0;
  padding: 0;
  border: none;

  legend {
    width: 100%;
    margin-bottom: 8px;
    padding: 0;
    color: @heading;
    font-size: 14px;
    font-weight: 600;
  }

  .choice {
    cursor: pointer;

    input {
      position: absolute;
      opacity: 0;
      pointer-events: none;
    }

    span {
      display: inline-block;
      padding: 7px 12px;
      border: 1px solid @divider;
      border-radius: 20px;
      background-color: @white-smoke;
      font-size: 13px;
      font-weight: 500;
    }

    input:checked + span {
      border-color: @carnet-sky;
      background-color: @carnet-sky;
      color: @carnet-ink;
      font-weight: 600;
    }

    input:focus-visible + span {
      outline: 2px solid @accent-text;
      outline-offset: 2px;
    }
  }
}

.feedback-screenshot {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin: 8px 0 16px 0;

  .feedback-label {
    color: @heading;
    font-size: 14px;
    font-weight: 600;
  }
}

.screenshot-add {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 14px;
  border: 1px dashed @accent-text;
  border-radius: 8px;
  color: @accent-text;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;

  input {
    display: none;
  }
}

.screenshot-preview {
  display: flex;
  align-items: center;
  gap: 12px;

  img {
    max-width: 160px;
    max-height: 120px;
    border: 1px solid @divider;
    border-radius: 8px;
    object-fit: contain;
  }
}

.screenshot-remove {
  padding: 6px 12px;
  border: 1px solid @error;
  border-radius: 20px;
  background: none;
  color: @error;
  font-size: 13px;
  cursor: pointer;
}

.screenshot-error {
  color: @error;
  font-size: 13px;
}

.feedback-version {
  margin: 8px 0 0 0;
  color: @pale-sky;
  font-size: 12px;
}

.feedback-actions {
  display: flex;
  flex: none;
  gap: 12px;
  padding: 12px 20px calc(12px + env(safe-area-inset-bottom)) 20px;
  border-top: 1px solid @divider;

  @media screen and (min-width: @desktop-min-width) {
    justify-content: flex-end;
    padding-bottom: 12px;
  }

  .button {
    flex: 1 1 0;
    margin: 0;

    @media screen and (min-width: @desktop-min-width) {
      flex: none;
      width: 160px;
    }

    button {
      width: 100%;
    }

    button:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
  }
}
</style>
