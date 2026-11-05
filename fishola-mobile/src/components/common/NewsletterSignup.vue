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
  <div class="newsletter-signup" v-if="registrationOpen !== null">
    <div class="newsletter-card" v-if="registrationOpen">
      <h2 class="newsletter-title">Les inscriptions sont ouvertes</h2>
      <p class="newsletter-text">Créez votre compte pour saisir vos sorties et vos captures.</p>
      <div class="button button-primary">
        <button type="button" v-on:click="goToRegister">Créer un compte</button>
      </div>
    </div>

    <div class="newsletter-card" v-else-if="subscribedEmail">
      <h2 class="newsletter-title">Merci !</h2>
      <p class="newsletter-text newsletter-success">
        Un e-mail de confirmation vient d'être envoyé à {{ subscribedEmail }}. Cliquez sur le lien
        qu'il contient pour valider votre inscription.
      </p>
    </div>

    <form class="newsletter-card" v-else novalidate v-on:submit.prevent="subscribe">
      <h2 class="newsletter-title">Être prévenu·e de l'ouverture</h2>
      <p class="newsletter-text">
        Carnet de pêche est en test. Laissez votre e-mail pour être prévenu·e de l'ouverture des
        inscriptions et recevoir la newsletter.
      </p>
      <FormInput name="newsletter-email" type="email" label="E-mail" placeholder="Renseignez votre e-mail"
        v-model="email" v-bind:error="validationErrors['email']" />
      <FormSelect name="newsletter-department" label="Département (optionnel)" v-model="departmentCode"
        v-bind:options="departmentOptions" v-bind:error="validationErrors['departmentCode']" />
      <div class="form-checkbox newsletter-consent">
        <input type="checkbox" id="newsletter-consent" class="pelorous-checkbox" v-model="consent" />
        <label for="newsletter-consent"></label>
        <label for="newsletter-consent" class="newsletter-consent-label">
          J'accepte que mon e-mail soit utilisé pour être prévenu·e de l'ouverture de Carnet de pêche
          et recevoir sa newsletter. Je peux me désinscrire à tout moment par le lien présent dans chaque
          e-mail.
          <a :href="privacyUrl" target="_blank" rel="noopener">Politique de confidentialité</a>
        </label>
      </div>
      <div class="field-error newsletter-consent-error" v-if="validationErrors['consent']">
        {{ validationErrors["consent"] }}
      </div>
      <div class="button button-primary newsletter-submit">
        <button type="submit" :disabled="sending">S'inscrire</button>
      </div>
    </form>
  </div>
</template>

<script lang="ts">
import FormInput from "@/components/common/FormInput.vue";
import FormSelect from "@/components/common/FormSelect.vue";
import DocumentationService from "@/services/DocumentationService";
import EventBus from "@/services/EventBus";
import NewsletterService, { DepartmentName } from "@/services/NewsletterService";
import ProfileService from "@/services/ProfileService";
import { RouterUtils } from "@/router/RouterUtils";

import { Component, Vue } from "vue-facing-decorator";

/**
 * Encart de la page d'accueil publique (#313) : tant que l'inscription à l'app
 * est fermée, le visiteur laisse son e-mail (double opt-in) pour être prévenu de
 * l'ouverture ; une fois ouverte, l'encart devient un lien « Créer un compte ».
 */
@Component({
  components: {
    FormInput,
    FormSelect,
  },
})
export default class NewsletterSignup extends Vue {
  // null tant que l'état de l'inscription n'est pas connu : rien n'est affiché.
  registrationOpen: boolean | null = null;
  email = "";
  departmentCode = "";
  consent = false;
  validationErrors: { [field: string]: string } = {};
  sending = false;
  subscribedEmail = "";
  departmentOptions: { id: string; name: string }[] = [];
  privacyUrl = DocumentationService.getCGUUrl();

  mounted() {
    ProfileService.isRegistrationEnabled().then((enabled) => {
      this.registrationOpen = enabled;
      if (!enabled) {
        this.loadDepartments();
      }
    });
  }

  loadDepartments() {
    NewsletterService.getDepartments().then(
      (departments: DepartmentName[]) => {
        this.departmentOptions = departments.map((department) => ({
          id: department.code,
          name: `${department.code} - ${department.name}`,
        }));
      },
      () => (this.departmentOptions = [])
    );
  }

  subscribe() {
    if (!this.consent) {
      this.validationErrors = { consent: "Votre consentement est nécessaire pour vous inscrire" };
      return;
    }
    this.validationErrors = {};
    this.sending = true;
    NewsletterService.subscribe({ email: this.email, departmentCode: this.departmentCode, consent: this.consent })
      .then(
        () => (this.subscribedEmail = this.email.trim()),
        (error) => this.subscriptionFailed(error)
      )
      .finally(() => (this.sending = false));
  }

  subscriptionFailed(error: { status: number; content?: { [field: string]: string } }) {
    if ((error.status == 400 || error.status == 429) && error.content) {
      this.validationErrors = error.content;
    } else {
      EventBus.emit("toaster-error", "L'inscription n'a pas pu être enregistrée, réessayez plus tard");
    }
  }

  goToRegister() {
    RouterUtils.pushRouteNoDuplicate(this.$router, "/register");
  }
}
</script>

<style scoped lang="less">
.newsletter-signup {
  background-color: @white-smoke;
  padding: 40px 20px;
}

.newsletter-card {
  max-width: 560px;
  margin: 0 auto;
  padding: @margin-large;
  border-radius: 16px;
  background-color: @surface;
  color: @gunmetal;
  font-family: @font-family;
  letter-spacing: @letter-spacing;
  text-align: left;
}

.newsletter-title {
  margin: 0 0 @vertical-margin-small 0;
  color: @accent-text;
  font-size: 22px;
  font-weight: 700;
  line-height: 1.2;
}

.newsletter-text {
  margin: 0 0 @vertical-margin-small 0;
  font-size: 15px;
  line-height: 1.5;
}

.newsletter-success {
  color: @success;
  font-weight: 600;
}

.newsletter-consent {
  display: flex;
  align-items: flex-start;
  padding-top: @vertical-margin-small;

  label:first-of-type {
    flex-shrink: 0;
    margin-top: 2px;
  }
}

.newsletter-consent-label {
  margin-left: @margin-small;
  font-size: @fontsize-small-paragraph;
  line-height: calc(@fontsize-small-paragraph + @line-height-padding-medium);

  a {
    color: @accent-text;
    font-weight: 600;
  }
}

.newsletter-consent-error {
  margin-top: @vertical-margin-xx-small;
  color: @error;
  font-size: @fontsize-small-paragraph;
}

.newsletter-submit {
  margin-top: @vertical-margin-medium;

  button {
    width: 100%;
  }

  @media screen and (min-width: @desktop-min-width) {
    button {
      width: @desktop-button-width;
    }
  }
}
</style>
