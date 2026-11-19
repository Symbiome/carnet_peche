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
  <div class="why">
    <div class="why-content">
      <header class="why-header">
        <button class="back" aria-label="Retour" v-on:click="goBack">
          <img src="img/carnet/header-back.svg" alt="" />
        </button>
        <h1>Pourquoi carnet de pêche ?</h1>
      </header>

      <div class="why-blocks">
        <section class="why-banner">
          <h2>Sentinelles des cours d’eau</h2>
          <p>Avec Carnet de pêche, chaque sortie aide à préserver les rivières.</p>
        </section>

        <section class="why-card presentation-card">
          <div class="eyebrow">Carnet de pêche</div>
          <h2>Un projet de science citoyenne</h2>
          <p>
            Les données anonymisées de vos prises et de vos parties de pêche servent à prendre
            des mesures de préservation des cours d’eau, pour une pêche en rivière durable et
            abondante.
          </p>
          <p>Le pêcheur qui utilise l’application est une véritable sentinelle des cours d’eau.</p>
        </section>

        <section class="key-figures" v-if="keyFiguresLoaded">
          <div class="kf-item kf-trips">
            <div class="kf-number"><Counter :n="tripsCount" /></div>
            <div class="kf-label">sorties</div>
          </div>
          <div class="kf-item kf-catches">
            <div class="kf-number"><Counter :n="catchsCount" /></div>
            <div class="kf-label">Captures</div>
          </div>
          <div class="kf-item kf-pictures">
            <div class="kf-number"><Counter :n="picturesCount" /></div>
            <div class="kf-label">Photos</div>
          </div>
        </section>

        <section class="communications">
          <h2>Communications</h2>
          <CommunicationsOnAboutPage v-if="news.length > 0" :news="news" />
          <template v-else>
            <p>Pas de communications autour de Carnet de pêche pour le moment.</p>
            <p>Revenez consulter cette page prochainement !</p>
          </template>
        </section>

        <form class="why-card contact-card" novalidate v-on:submit.prevent="sendContact">
          <h2>Nous contacter</h2>
          <p>
            Une suggestion, une question sur l’application ?
            <em>Écrivez-nous via le formulaire ci-dessous.</em>
          </p>
          <FormInput name="why-contact-email" type="email" label="Email"
            placeholder="contact@email.com" autocomplete="email" v-model="contactEmail" />
          <FormTextarea name="why-contact-message" label="Message"
            placeholder="Écrivez votre message ici" v-model="contactMessage" />
          <div class="button button-primary send-button">
            <button type="submit">Envoyer</button>
          </div>
        </form>

        <section class="credits">
          <div class="credit">
            <h3>Porteur de projet</h3>
            <p>Carnet de pêche est produit par l’Union des Bassins Rhône-Méditerranée-Corse (UFBRMC).</p>
            <a :href="ufbrmcUrl" target="_blank" rel="noopener">
              <img class="logo-ufbrmc" src="img/logo/ufbrmc.jpg"
                alt="Union des Bassins Rhône-Méditerranée-Corse" />
            </a>
          </div>
          <div class="credit">
            <h3>Développement</h3>
            <p>Application développée par Symbiome.</p>
            <a href="https://symbiome.eu" target="_blank" rel="noopener">
              <img class="logo-symbiome" src="img/logo/symbiome.png" alt="Symbiome" />
            </a>
          </div>
          <div class="credit">
            <h3>Design graphique</h3>
            <p>
              L’identité visuelle de l’application a été réalisée
              par Arthur Doré, graphiste freelance.
            </p>
            <a class="designer-wordmark" href="https://arthurdore.fr/" target="_blank" rel="noopener">
              Arthur<em>Doré</em><span>.fr</span>
            </a>
          </div>
        </section>
      </div>
    </div>
    <BottomNav />
  </div>
</template>

<script lang="ts">
import BottomNav from "@/components/layout/BottomNav.vue";
import Counter from "@/components/common/Counter.vue";
import FormInput from "@/components/common/FormInput.vue";
import FormTextarea from "@/components/common/FormTextarea.vue";
import CommunicationsOnAboutPage from "@/views/CommunicationsOnAboutPage.vue";

import AboutService from "@/services/AboutService";
import DocumentationService from "@/services/DocumentationService";
import FeedbackService from "@/services/FeedbackService";
import EventBus from "@/services/EventBus";
import { Feedback, NewsBean } from "@/pojos/BackendPojos";

import { Component, Vue } from "vue-facing-decorator";

/**
 * « Pourquoi carnet de pêche ? » : présentation du projet de science
 * citoyenne au pêcheur connecté, chiffres clés, communications, formulaire de
 * contact et crédits (porteur, développement, design).
 */
@Component({
  components: { BottomNav, Counter, FormInput, FormTextarea, CommunicationsOnAboutPage },
})
export default class WhyCarnetView extends Vue {
  readonly ufbrmcUrl =
    "https://www.peche-auvergne-rhonealpes.fr/actualite/3754/1913-union-des-bassins-rhone-mediterranee-corse.htm";

  keyFiguresLoaded: boolean = false;
  tripsCount: number = 0;
  catchsCount: number = 0;
  picturesCount: number = 0;
  news: NewsBean[] = [];

  contactEmail: string = "";
  contactMessage: string = "";

  created() {
    this.loadKeyFigures();
    DocumentationService.getNews().then(
      (news) => (this.news = news),
      () => undefined
    );
  }

  /**
   * Charge les chiffres clés ; le bloc reste masqué s'ils sont indisponibles.
   * Les valeurs sont posées après l'affichage du bloc : `Counter` ne s'anime
   * que sur un changement de `n` survenu après son montage.
   */
  loadKeyFigures() {
    AboutService.getKeyFigures().then(
      (keyFigures) => {
        this.keyFiguresLoaded = true;
        this.$nextTick(() => {
          this.tripsCount = keyFigures.tripsCount;
          this.catchsCount = keyFigures.catchsCount;
          this.picturesCount = keyFigures.picturesCount;
        });
      },
      () => undefined
    );
  }

  /** Envoie le message du formulaire à l'équipe projet (catégorie CONTACT). */
  sendContact() {
    if (!this.contactEmail) {
      EventBus.emit("toaster-error", "L'email est obligatoire");
      return;
    }
    if (!this.contactMessage) {
      EventBus.emit("toaster-error", "Le message est obligatoire");
      return;
    }
    FeedbackService.sendFeedbackNoAsync(this.buildContactFeedback()).then(
      () => {
        this.contactMessage = "";
        EventBus.emit("toaster-success", "Votre message a bien été envoyé à l'équipe projet, merci", 10000);
      },
      () => EventBus.emit("toaster-error", "Une erreur est survenue, merci de réessayer ultérieurement")
    );
  }

  buildContactFeedback(): Feedback {
    const version = `${import.meta.env.VITE__PACKAGE_JSON_VERSION} (${import.meta.env.VITE__GIT_REVISION})`;
    return {
      id: "" + new Date().getTime(),
      category: "CONTACT",
      date: new Date(),
      frontendVersion: version,
      email: this.contactEmail,
      description: this.contactMessage,
    };
  }

  goBack() {
    this.$router.back();
  }
}
</script>

<style scoped lang="less">
.why {
  position: relative;
  height: 100%;
  background-color: @white-smoke;

  @media screen and (min-width: @desktop-min-width) {
    height: 100vh;
  }
}

.why-content {
  height: calc(100% - @footer-height);
  overflow-y: auto;
  overflow-x: hidden;
  padding: env(safe-area-inset-top) 12px 40px 12px;
  letter-spacing: @letter-spacing;

  @media screen and (min-width: @desktop-min-width) {
    height: 100%;
    max-width: 700px;
    padding: @margin-large @margin-large-desktop;
  }
}

.why-header {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 30px 0 0 5px;

  .back {
    display: flex;
    padding: 0;
    border: none;
    background: none;
    -webkit-tap-highlight-color: transparent;

    &:active {
      animation: none;
    }
  }

  h1 {
    margin: 0;
    color: @heading;
    font-size: 22px;
    font-weight: 700;
    line-height: 1.2;
  }
}

// Le chevron de la maquette est bleu nuit : passé en blanc sur fond sombre.
:root[data-theme="dark"] .why-header .back img {
  filter: brightness(0) invert(1);
}

.why-blocks {
  display: flex;
  flex-direction: column;
  gap: 34px;
  margin-top: 35px;

  h2 {
    margin: 0;
    font-size: 18px;
    font-weight: 700;
    line-height: 1.2;
  }

  p {
    margin: 0;
    font-size: 14px;
    line-height: 1.4;
  }
}

.why-banner {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 13px;
  height: 195px;
  padding: 0 @margin-medium;
  border-radius: 13px;
  background:
    linear-gradient(fade(@carnet-ink, 70%), fade(@carnet-ink, 70%)),
    url(img/carnet/why-banner.jpg) center / cover no-repeat;
  color: @white;
  text-align: center;

  p {
    max-width: 256px;
  }
}

.why-card {
  padding: 26px 20px 25px 20px;
  border-radius: 13px;
  background-color: @surface;
}

.presentation-card {
  .eyebrow {
    color: @accent-text;
    font-size: 14px;
    font-weight: 600;
    line-height: 1.2;
  }

  h2 {
    margin: 6px 0 14px 0;
    color: @heading;
  }

  p {
    color: @body-text;

    & + p {
      margin-top: 19.6px;
    }
  }
}

.key-figures {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 11px;

  .kf-item {
    display: flex;
    flex-direction: column;
    gap: 1px;
    height: 57px;
    padding: 10px 11px;
    border-radius: 10px;
    line-height: 1.2;
  }

  .kf-trips {
    background-color: @sky-box;
    color: @sky-text;
  }

  .kf-catches {
    background-color: @green-box;
    color: @green-text;
  }

  .kf-pictures {
    background-color: @yellow-box;
    color: @yellow-text;
  }

  .kf-number {
    font-size: 18px;
    font-weight: 700;
  }

  .kf-label {
    font-size: 11px;
    font-weight: 500;
  }
}

.communications {
  padding: 32px 20px 33px 20px;
  border-top: 2px solid @divider;
  border-bottom: 2px solid @divider;

  h2 {
    margin-bottom: 13px;
    color: @heading;
  }

  p {
    color: @body-text;

    & + p {
      margin-top: 19.6px;
    }
  }
}

.contact-card {
  h2 {
    color: @accent-text;
  }

  > p {
    margin: 11px 0 1px 0;
    color: @heading;
  }

  // Champs aux mesures de la maquette (libellé 14 px, cadre fin de 30 px).
  :deep(.form-input),
  :deep(.form-textarea) {
    margin-top: 20px;

    label {
      color: @heading;
      font-size: 14px;
      font-weight: 600;
      line-height: 1.4;
    }

    input,
    textarea {
      margin-top: 8px;
      border: 0.5px solid @heading;
      border-radius: 5px;
      font-size: 14px;

      &::placeholder {
        font-style: normal;
        font-size: 14px;
      }
    }

    input {
      height: 30px;
    }

    textarea {
      height: 97px;
    }

    > div {
      height: auto;
    }
  }

  .send-button {
    width: 124px;
    height: 30px;
    margin: 22px 0 0 0;

    button {
      border-radius: 5px;
      font-size: 14px;
      line-height: 1.4;
    }
  }
}

.credits {
  display: flex;
  flex-direction: column;
  gap: 37px;
  padding-left: 20px;
  max-width: 326px;

  .credit {
    display: flex;
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }

  h3 {
    margin: 0;
    color: @accent-text;
    font-size: 14px;
    font-weight: 600;
    line-height: 1.2;
  }

  p {
    color: @heading;
  }

  a {
    display: block;
  }

  .logo-ufbrmc {
    display: block;
    width: 64px;
    height: 70px;
    object-fit: cover;
  }

  .logo-symbiome {
    display: block;
    width: 125px;
    height: 30px;
    object-fit: cover;
  }
}

.designer-wordmark {
  color: @text-strong;
  text-decoration: none;
  font-size: 17.944px;
  font-weight: 600;
  line-height: 1.4;

  em {
    font-style: italic;
  }

  span {
    font-weight: 400;
  }
}
</style>
