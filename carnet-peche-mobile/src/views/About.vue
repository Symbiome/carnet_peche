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
  <div class="about-page force-light-theme" v-on:scroll="scrolled" id="about-scroll-container">
    <!-- Page vitrine : reste en thème clair (#208). -->
    <div class="about-frame" id="top">
      <header class="about-header" v-if="!wrappedInTab">
        <a class="about-logo-link logo-smartphone" href="#/about" @click.prevent="scrollToSection('top')">
          <CarnetLogo class="about-logo" />
        </a>

        <button class="back-to-app" v-if="connected && closed" v-on:click="goToLogin">
          <i class="icon-arrow icon-back" />
          Retour à l'application
        </button>

        <button class="burger" v-if="closed" aria-label="Ouvrir le menu" @click="closed = !closed">
          <i class="icon-menu"></i>
        </button>

        <div class="menu" :class="{ closed: closed }">
          <a class="about-logo-link logo-desktop" href="#/about"
            @click.prevent="scrollToSection('top'); closed = true">
            <CarnetLogo class="about-logo" />
          </a>

          <nav class="about-nav">
            <button class="close-menu hide-on-desktop" aria-label="Fermer le menu" v-on:click="closeMenu">+</button>
            <a href="#/about" :class="{ active: activeSection == 'presentation' }"
              @click.prevent="scrollToSection('presentation'); closed = !closed">Présentation</a>
            <a href="#/about" :class="{ active: activeSection == 'communications' }"
              @click.prevent="scrollToSection('communications'); closed = !closed">Communications</a>
            <a href="#/about" :class="{ active: activeSection == 'contact' }"
              @click.prevent="scrollToSection('contact'); closed = !closed">Contact</a>
          </nav>
          <div class="button button-primary authentication">
            <button v-on:click="goToLogin">{{ connected ? "Retour à l'application" : "Connexion" }}</button>
          </div>
        </div>
      </header>

      <div class="title-banner" id="title" v-if="!wrappedInTab">
        <div class="title-overlay">
          <h1 class="main-title">Sentinelles des cours d'eau</h1>
          <p class="sub-title">Avec Carnet de pêche, chaque sortie aide à préserver les rivières</p>
        </div>
      </div>

      <!-- Inscription à la newsletter tant que l'app est fermée (#313), aussi dans
           l'onglet « Présentation » de l'accueil hors connexion -->
      <NewsletterSignup v-if="profileChecked && !connected" />

      <section class="about-section" id="presentation">
        <div class="about-card presentation-card">
          <div class="eyebrow">Carnet de pêche</div>
          <h2 class="section-title">Un projet de science citoyenne</h2>
          <p>
            Les données anonymisées de vos prises et de vos parties de pêche servent à prendre des
            mesures de préservation des cours d'eau, pour une pêche en rivière durable et abondante.
          </p>
          <p>
            Le pêcheur qui utilise l'application est une véritable sentinelle des cours d'eau.
          </p>
        </div>
      </section>

      <section class="about-section" id="about">
        <div class="key-figures" ref="keyFigures">
          <div class="kf-item kf-trips">
            <div class="kf-number"><Counter :n="tripsCount"></Counter></div>
            <div class="kf-label">sorties</div>
          </div>
          <div class="kf-item kf-catches">
            <div class="kf-number"><Counter :n="catchsCount"></Counter></div>
            <div class="kf-label">captures</div>
          </div>
          <div class="kf-item kf-pictures">
            <div class="kf-number"><Counter :n="picturesCount"></Counter></div>
            <div class="kf-label">photos</div>
          </div>
        </div>
      </section>

      <section class="communications-section" id="communications" v-if="!wrappedInTab">
        <h2 class="section-title">Communications</h2>
        <CommunicationsOnAboutPage :news="news" />
      </section>

      <section class="about-section" id="contact">
        <form class="about-card contact-card" novalidate v-on:submit.prevent="sendContact">
          <h2 class="section-title">Nous contacter</h2>
          <p>
            Une suggestion, une question sur l'application ? Écrivez-nous
            par l'intermédiaire du formulaire ci-dessous.
          </p>
          <FormInput name="contact-email" type="email" label="E-mail" placeholder="Votre e-mail"
            autocomplete="email" v-model="contactEmail" />
          <FormTextarea name="contact-message" label="Message" placeholder="Votre message"
            v-model="contactMessage" />
          <div class="button button-primary send-button">
            <button type="submit">Envoyer</button>
          </div>
        </form>
      </section>

      <section class="about-section" id="producted_by">
        <div class="about-card producers-card">
          <div class="producer">
            <h3 class="producer-title">Porteur du projet</h3>
            <p>Carnet de pêche est produit par l'Union des Bassins Rhône-Méditerranée-Corse (UFBRMC).</p>
            <a href="https://www.peche-auvergne-rhonealpes.fr/actualite/3754/1913-union-des-bassins-rhone-mediterranee-corse.htm"
              target="_blank" rel="noopener"><img class="logo-ufbrmc" src="/img/logo/ufbrmc.jpg"
                alt="Union des Bassins Rhône-Méditerranée-Corse" /></a>
          </div>
          <div class="producer">
            <h3 class="producer-title">Développement</h3>
            <p>Application développée par Symbiome.</p>
            <a href="https://symbiome.eu" target="_blank" rel="noopener"><img class="logo-symbiome"
                src="/img/logo/symbiome.png" alt="Symbiome" /></a>
          </div>
        </div>
      </section>

      <footer class="about-footer">
        <p>© Union des Bassins Rhône-Méditerranée-Corse · Application développée par Symbiome</p>
      </footer>
    </div>
  </div>
</template>

<script lang="ts">
import Counter from "@/components/common/Counter.vue";
import ProfileService from "@/services/ProfileService";
import AboutService from "@/services/AboutService";
import FeedbackService from "@/services/FeedbackService";
import { RouterUtils } from "@/router/RouterUtils";

import { Component, Prop, Vue } from "vue-facing-decorator";
import debounce from "lodash/debounce";
import EventBus from "@/services/EventBus";
import { Feedback, NewsBean } from "@/pojos/BackendPojos";
import DocumentationService from "../services/DocumentationService";
import CommunicationsOnAboutPage from "./CommunicationsOnAboutPage.vue";
import CarnetLogo from "@/components/layout/CarnetLogo.vue";
import NewsletterSignup from "@/components/common/NewsletterSignup.vue";
import FormInput from "@/components/common/FormInput.vue";
import FormTextarea from "@/components/common/FormTextarea.vue";

@Component({
  components: {
    Counter,
    CommunicationsOnAboutPage,
    CarnetLogo,
    NewsletterSignup,
    FormInput,
    FormTextarea,
  },
})
export default class AboutView extends Vue {
  @Prop({ default: false }) wrappedInTab: boolean;

  tripsCount: number = 0;
  realTripsCount: number = 442;
  catchsCount: number = 0;
  realCatchsCount: number = 1726;
  picturesCount: number = 0;
  realPicturesCount: number = 212;
  news: NewsBean[] = [];

  contactEmail: string = "";
  contactMessage: string = "";

  projectVersion: string = import.meta.env.VITE__PACKAGE_JSON_VERSION;
  gitRevision: string = import.meta.env.VITE__GIT_REVISION;
  frontendVersion: string = `${this.projectVersion} (${this.gitRevision})`;

  closed: boolean = true;
  connected: boolean = false;
  profileChecked: boolean = false;
  keyFiguresObserver?: IntersectionObserver;

  constructor() {
    super();
  }

  created() {
    ProfileService.fetchProfile()
      .then(
        () => (this.connected = true),
        () => (this.connected = false)
      )
      .finally(() => (this.profileChecked = true));
    AboutService.getKeyFigures().then(
      (kf) => {
        console.debug("Chiffres clés", kf);
        this.realTripsCount = kf.tripsCount;
        this.realCatchsCount = kf.catchsCount;
        this.realPicturesCount = kf.picturesCount;
      },
      () => undefined
    );
    DocumentationService.getNews().then((zeNews) => {
      this.news = zeNews;
    });
  }

  sendContact() {
    if (!this.contactEmail) {
      EventBus.emit("toaster-error", "L'email est obligatoire");
      return;
    }
    if (!this.contactMessage) {
      EventBus.emit("toaster-error", "Le message est obligatoire");
      return;
    }

    let feedback: Feedback = {
      id: "" + new Date().getTime(),
      category: "CONTACT",
      date: new Date(),
      frontendVersion: this.frontendVersion,
      email: this.contactEmail,
      description: this.contactMessage,
    };

    FeedbackService.sendFeedbackNoAsync(feedback).then(
      () => {
        EventBus.emit(
          "toaster-success",
          "Votre message a bien été envoyé à l'équipe projet, merci",
          10000
        );
      },
      (_error) => {
        EventBus.emit(
          "toaster-error",
          "Une erreur est survenue, merci de réessayer ultérieurement"
        );
      }
    );
  }

  sectionIds: string[] = [
    "presentation",
    "communications",
    "contact",
  ];
  activeSection: string = "";
  refreshSelectedSectionTimer: any = undefined;

  scrolled() {
    if (this.refreshSelectedSectionTimer) {
      this.refreshSelectedSectionTimer.cancel();
    }
    this.refreshSelectedSectionTimer = debounce(
      this.refreshSelectedSection,
      100
    );
    this.refreshSelectedSectionTimer();
  }

  refreshSelectedSection() {
    const elem = document.getElementById("about-scroll-container");
    if (elem) {
      let foundSection: string = "";
      this.sectionIds.forEach((sectionId) => {
        const sectionElem = document.getElementById(sectionId);
        if (sectionElem && elem.scrollTop >= sectionElem.offsetTop - 73) {
          // 73 étant la hauteur du header
          foundSection = sectionId;
        }
      });
      this.activeSection = foundSection;
    }
  }

  mounted() {
    const keyFigures = this.$refs.keyFigures as HTMLElement | undefined;
    if (keyFigures) {
      this.keyFiguresObserver = new IntersectionObserver((entries) =>
        this.keyFiguresItemsVisibilityChanged(entries.some((entry) => entry.isIntersecting))
      );
      this.keyFiguresObserver.observe(keyFigures);
    }
  }

  beforeUnmount() {
    this.keyFiguresObserver?.disconnect();
  }

  scrollToSection(sectionId: string) {
    document.getElementById(sectionId)?.scrollIntoView({ behavior: "smooth" });
  }

  keyFiguresItemsVisibilityChanged(isVisible: boolean) {
    if (isVisible) {
      this.tripsCount = this.realTripsCount;
      this.catchsCount = this.realCatchsCount;
      this.picturesCount = this.realPicturesCount;
    }
  }

  goToLogin() {
    this.closed = true;
    ProfileService.fetchProfile().then(
      (_profile) => {
        RouterUtils.pushRouteNoDuplicate(this.$router, RouterUtils.homeRoute());
      },
      (_status) => {
        RouterUtils.pushRouteNoDuplicate(this.$router, "/login");
      }
    );
  }

  closeMenu() {
    this.closed = true;
  }
}
</script>


<style scoped lang="less">
@about-header-height: 73px;
@about-content-width: 760px;

.about-page {
  height: 100%;
  overflow: auto;
  overflow-x: hidden;
  background-color: @white-smoke;
  color: @gunmetal;
  font-family: @font-family;
  letter-spacing: @letter-spacing;
}

.about-header {
  position: sticky;
  top: 0;
  z-index: 998; // Juste en dessous du toaster
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: @about-header-height;
  padding: 0 @margin-medium;
  background-color: @surface;
  border-bottom: 1px solid @divider;

  .about-logo {
    width: 150px;
  }

  .logo-smartphone {
    @media screen and (min-width: 900px) {
      display: none;
    }
  }

  // Retour visible sans ouvrir le menu, pour le pêcheur connecté.
  .back-to-app {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-left: auto;
    margin-right: @margin-small;
    padding: 8px 14px;
    border: none;
    border-radius: 10px;
    background-color: @carnet-blue;
    color: @white;
    font-family: inherit;
    font-size: 14px;
    font-weight: 600;
    letter-spacing: @letter-spacing;
    white-space: nowrap;
    cursor: pointer;

    @media screen and (min-width: 900px) {
      display: none;
    }
  }

  .burger {
    padding: 0;
    border: none;
    background: none;
    color: @heading;
    font-size: 22px;
    cursor: pointer;

    @media screen and (min-width: 900px) {
      display: none;
    }
  }

  .menu {
    display: flex;
    flex: 1;
    align-items: center;
    justify-content: space-between;
    height: 100%;

    .logo-desktop {
      @media screen and (max-width: 899px) {
        display: none;
      }
    }

    @media screen and (max-width: 899px) {
      position: fixed;
      top: 0;
      right: 0;
      flex-direction: column;
      align-items: stretch;
      justify-content: space-between;
      width: 260px;
      height: 100vh;
      height: 100dvh; /* Good height for mobile screens, with navigation bar, if supported */
      padding: @margin-medium;
      background-color: @surface;
      box-shadow: -4px 0 16px @black-alpha-50;

      &.closed {
        display: none;
      }
    }
  }

  .about-nav {
    display: flex;
    gap: 4px;

    a {
      padding: 10px 16px;
      border-radius: 10px;
      color: @gunmetal;
      font-size: 15px;
      font-weight: 600;
      text-decoration: none;

      &:hover,
      &.active {
        background-color: @blue-box;
        color: @accent-text;
      }
    }

    @media screen and (max-width: 899px) {
      flex-direction: column;
      text-align: right;
    }
  }

  .close-menu {
    align-self: flex-end;
    padding: 0 @margin-small;
    border: none;
    background: none;
    color: @heading;
    font-size: 36px;
    line-height: 1;
    transform: rotate(45deg);
    cursor: pointer;
  }

  .authentication {
    width: auto;
    height: 41px;
    margin: 0;

    button {
      padding: 0 @margin-medium;
      font-size: 15px;
      font-weight: 600;
    }
  }
}

.title-banner {
  height: 520px;
  background: url(img/about-coregones5.jpg) top center no-repeat;
  background-size: cover;

  @media screen and (max-width: @mobile-max-width) {
    height: 380px;
  }

  .title-overlay {
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    height: 100%;
    padding: 0 @margin-medium;
    background: fade(@carnet-ink, 45%);
    color: @white;
    text-align: center;
  }

  .main-title {
    margin: 0 0 @vertical-margin-small 0;
    font-size: 56px;
    font-weight: 700;
    line-height: 1.1;

    @media screen and (max-width: @mobile-max-width) {
      font-size: 34px;
    }
  }

  .sub-title {
    margin: 0;
    font-size: 22px;
    font-weight: 500;
    line-height: 1.3;

    @media screen and (max-width: @mobile-max-width) {
      font-size: 17px;
    }
  }
}

.about-section {
  max-width: @about-content-width;
  margin: 0 auto;
  padding: 0 @margin-medium @vertical-margin-large;
}

#presentation {
  padding-top: @vertical-margin-large;
}

.about-card {
  padding: @margin-large;
  border-radius: 16px;
  background-color: @surface;
  text-align: left;

  @media screen and (max-width: @mobile-max-width) {
    padding: @margin-medium;
  }

  p {
    margin: 0 0 @vertical-margin-small 0;
    font-size: 15px;
    line-height: 1.5;
  }
}

.eyebrow {
  margin-bottom: @vertical-margin-xx-small;
  color: @accent-text;
  font-size: 14px;
  font-weight: 600;
}

.section-title {
  margin: 0 0 @vertical-margin-small 0;
  color: @heading;
  font-size: 22px;
  font-weight: 700;
  line-height: 1.2;
}

.key-figures {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;

  .kf-item {
    display: flex;
    flex-direction: column;
    padding: 14px 12px;
    border-radius: 13px;
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
    font-size: 34px;
    font-weight: 700;

    @media screen and (max-width: 400px) {
      font-size: 26px;
    }
  }

  .kf-label {
    font-size: 16px;
    font-weight: 500;
  }
}

.communications-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: @vertical-margin-large 0;

  .section-title {
    width: 100%;
    max-width: @about-content-width;
    padding: 0 @margin-medium;
  }
}

.contact-card {
  .send-button {
    margin: @vertical-margin-medium 0 0 0;

    @media screen and (max-width: @mobile-max-width) {
      width: 100%;
    }
  }
}

.producers-card {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: @margin-large;
  text-align: center;

  @media screen and (max-width: @mobile-max-width) {
    grid-template-columns: 1fr;
  }

  .producer + .producer {
    @media screen and (max-width: @mobile-max-width) {
      padding-top: @margin-large;
      border-top: 1px solid @divider;
    }
  }

  .producer-title {
    margin: 0 0 @vertical-margin-xx-small 0;
    color: @accent-text;
    font-size: 16px;
    font-weight: 600;
  }

  .logo-ufbrmc {
    height: 120px;
  }

  .logo-symbiome {
    height: 50px;
    max-width: 100%;
    margin-top: @vertical-margin-small;
  }
}

.about-footer {
  padding: @vertical-margin-medium @margin-medium;
  background-color: @carnet-navy;
  text-align: center;

  p {
    margin: 0;
    color: @white;
    font-size: 13px;
    opacity: 0.8;
  }
}
</style>
