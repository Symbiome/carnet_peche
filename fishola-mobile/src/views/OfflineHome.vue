<!--
  #%L
  Carnet de pêche :: Mobile
  %%
  Copyright (C) 2019 - 2022 INRAE - UMR CARRTEL
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
  <div class="page-with-header shifted-background">
    <CarnetHeader />
    <div class="page">
      <div class="pane pane-only">
        <div class="pane-content rounded">
          <h1 class="hide-on-mobile">Accueil</h1>
          <div class="home-tabs" role="group" aria-label="Contenu de l'accueil">
            <button :class="{ active: !showNews }" :aria-pressed="!showNews" @click="goPresentation">
              Présentation
            </button>
            <button :class="{ active: showNews }" :aria-pressed="showNews" @click="goNews">
              Communications
            </button>
          </div>
          <div class="about-container" v-if="!showNews">
            <About :wrappedInTab="true" />
          </div>
          <NewsView :news="news" v-else />
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import CarnetHeader from "@/components/layout/CarnetHeader.vue";
import CarnetFooter from "@/components/layout/CarnetFooter.vue";
import MyTrips from "@/views/MyTrips.vue";
import About from "@/views/About.vue";
import NewsView from "@/views/News.vue";
import { Component, Prop, Vue, Watch } from "vue-facing-decorator";
import DocumentationService from "../services/DocumentationService";
import { NewsBean } from "@/pojos/BackendPojos";
import { RouterUtils } from "@/router/RouterUtils";

@Component({
  components: {
    CarnetHeader,
    MyTrips,
    CarnetFooter,
    NewsView,
    About,
  },
})
export default class OfflineHome extends Vue {
  @Prop({ default: "presentation" }) defaultTab: string;
  showNews = false;
  news: NewsBean[] = [];
  mounted(): void {
    Prop;
    this.loadNews();
  }

  @Watch("defaultTab")
  async loadNews() {
    try {
      this.news = await DocumentationService.getNews();
      this.showNews = this.defaultTab == "news";
    } catch (e) {
      // News section will be left empty
    }
  }

  async showNewsTab() {
    this.showNews = true;
  }

  goNews() {
    RouterUtils.pushRouteNoDuplicate(this.$router, "/offline-home/news");
  }

  goPresentation() {
    RouterUtils.pushRouteNoDuplicate(this.$router, "/about");
  }
}
</script>

<style scoped lang="less">
.home-tabs {
  display: flex;
  gap: 2px;
  max-width: 400px;
  margin: 0 auto @vertical-margin-medium auto;
  padding: 6px;
  border-radius: 10px;
  background-color: @surface;

  button {
    flex: 1;
    height: 38px;
    border: none;
    border-radius: 8px;
    background: transparent;
    color: @pale-sky;
    font-family: @font-family;
    font-size: 15px;
    font-weight: 500;
    letter-spacing: @letter-spacing;
    cursor: pointer;

    &.active {
      background-color: @carnet-blue;
      color: @white;
      font-weight: 600;
    }
  }
}

@media screen and (max-width: @mobile-max-width) {
  .home-tabs {
    margin-top: @vertical-margin-medium;
  }
}

.news-badge {
  width: 30px;
  border-radius: 15px;
  background-color: @terra-cotta;
  color: @white;
  margin-right: @margin-small;
}
</style>
