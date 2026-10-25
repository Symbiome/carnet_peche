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
  <div class="onboarding" role="dialog" aria-modal="true" aria-labelledby="onboarding-title">
    <button class="onboarding-skip" v-on:click="$emit('skip')">{{ labels.skip }}</button>

    <div class="onboarding-slide" :key="index">
      <div class="onboarding-emoji" aria-hidden="true">{{ slide.emoji }}</div>
      <h1 id="onboarding-title" class="onboarding-title">{{ slide.title }}</h1>
      <p class="onboarding-text">{{ slide.text }}</p>
    </div>

    <div class="onboarding-progress" role="tablist" aria-label="Progression">
      <span
        v-for="(item, dotIndex) in slides"
        :key="dotIndex"
        class="onboarding-dot"
        :class="{ active: dotIndex === index }"
        role="tab"
        :aria-selected="dotIndex === index"
        :aria-label="'Écran ' + (dotIndex + 1) + ' sur ' + slides.length"
      ></span>
    </div>

    <div class="onboarding-actions">
      <button class="onboarding-previous" v-if="index > 0" v-on:click="index--">
        {{ labels.previous }}
      </button>
      <button class="onboarding-next" v-on:click="next">
        {{ isLast ? labels.start : labels.next }}
      </button>
    </div>
  </div>
</template>

<script lang="ts">
import { ONBOARDING_LABELS, ONBOARDING_SLIDES, OnboardingSlide } from "@/services/OnboardingContent";
import { Component, Vue } from "vue-facing-decorator";

/**
 * Écrans d'introduction de l'onboarding (#194) : but de l'application, données
 * anonymisées, remerciement. « Passer » ferme tout ; le dernier écran enchaîne
 * sur la visite guidée.
 */
@Component({ emits: ["skip", "finished"] })
export default class OnboardingSlides extends Vue {
  slides: OnboardingSlide[] = ONBOARDING_SLIDES;
  labels = ONBOARDING_LABELS;
  index: number = 0;

  get slide(): OnboardingSlide {
    return this.slides[this.index];
  }

  get isLast(): boolean {
    return this.index === this.slides.length - 1;
  }

  next() {
    if (this.isLast) {
      this.$emit("finished");
    } else {
      this.index++;
    }
  }
}
</script>

<style scoped lang="less">
.onboarding {
  position: fixed;
  inset: 0;
  z-index: 1000;

  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: calc(env(safe-area-inset-top) + 60px) 28px calc(env(safe-area-inset-bottom) + 32px) 28px;

  background-color: @white-smoke;
  color: @gunmetal;
  text-align: center;
}

.onboarding-skip {
  position: absolute;
  top: calc(env(safe-area-inset-top) + 16px);
  right: 16px;
  padding: 8px 4px;
  border: none;
  background: none;
  color: @accent-text;
  font-size: 16px;
  font-weight: 600;

  &:active {
    animation: none;
  }
}

.onboarding-slide {
  max-width: 420px;
  margin-top: auto;
}

.onboarding-emoji {
  font-size: 72px;
  line-height: 1.2;
}

.onboarding-title {
  margin: 24px 0 16px 0;
  color: @heading;
  font-size: 27px;
  font-weight: 700;
  line-height: 1.2;
  letter-spacing: @letter-spacing;
}

.onboarding-text {
  margin: 0;
  font-size: 18px;
  font-weight: 500;
  line-height: 1.4;
}

.onboarding-progress {
  display: flex;
  gap: 8px;
  margin: auto 0 28px 0;

  .onboarding-dot {
    width: 10px;
    height: 10px;
    border-radius: 5px;
    background-color: @pale-sky;
    opacity: 0.45;
    transition: width 0.2s;

    &.active {
      width: 26px;
      background-color: @accent-text;
      opacity: 1;
    }
  }
}

.onboarding-actions {
  display: flex;
  gap: 12px;
  width: 100%;
  max-width: 420px;

  button {
    flex: 1;
    height: 48px;
    border-radius: 24px;
    font-family: @font-family;
    font-size: 18px;
    font-weight: 600;

    &:active {
      animation: none;
    }
  }

  .onboarding-previous {
    border: 2px solid @accent-text;
    background: transparent;
    color: @accent-text;
  }

  .onboarding-next {
    flex: 1.6;
    white-space: nowrap;
    border: none;
    background-color: @carnet-blue;
    color: @white;
  }
}
</style>
