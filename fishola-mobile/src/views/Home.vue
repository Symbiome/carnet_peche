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
  <div class="home">
    <div class="home-content">
      <div class="home-header">
        <Title class="hide-on-desktop" />
        <Avatar
          v-if="initials"
          class="home-avatar"
          v-bind:initials="initials"
          v-on:goProfile="goAccount"
        />
      </div>
      <h1 class="home-greeting">Bonjour {{ firstName }} !</h1>

      <h2 class="home-section-title">Ma dernière sortie</h2>
      <LastTripCard
        v-if="lastTrip"
        v-bind:trip="lastTrip"
        v-bind:waterName="lastTripWaterName"
        v-bind:photoUrl="lastTripPhotoUrl"
      />
      <div class="home-empty" v-else-if="tripsLoaded">
        <span>Aucune sortie enregistrée pour le moment.</span>
        <div class="button button-primary">
          <button v-on:click="startNewTrip">Commencer</button>
        </div>
      </div>

      <HomeSummary
        v-bind:trips="trips"
        v-bind:markers="markers"
        v-bind:speciesKnown="markersLoaded"
        v-on:open="goActivitySummary"
      />

      <div class="home-badges-header">
        <h2 class="home-section-title">Badges</h2>
        <span class="badges-count" v-if="badges.length">
          <strong>{{ unlockedBadgesCount }}</strong>/{{ badges.length }}
        </span>
      </div>
      <div class="home-badges" v-if="displayedBadges.length">
        <BadgeTile
          v-for="badge in displayedBadges"
          :key="badge.id"
          :emoji="badge.icon || defaultBadgeEmoji"
          :label="badge.name"
          :locked="!badge.unlocked"
          v-on:clicked="goBadges"
        />
      </div>
      <button class="home-more" v-on:click="goBadges">Voir la collection&nbsp;›</button>
    </div>
    <BottomNav />
  </div>
</template>

<script lang="ts">
import { BadgeBean, CatchMarker, TripLight } from "@/pojos/BackendPojos";
import UserProfile from "@/pojos/UserProfile";
import Constants from "@/services/Constants";
import DashboardService, { DashboardAndSpecies } from "@/services/DashboardService";
import GamificationService from "@/services/GamificationService";
import ProfileService from "@/services/ProfileService";
import ReferentialService from "@/services/ReferentialService";
import TripsService, { TripsAndCount } from "@/services/TripsService";
import { RouterUtils } from "@/router/RouterUtils";
import Avatar from "@/components/common/Avatar.vue";
import BottomNav from "@/components/layout/BottomNav.vue";
import Title from "@/components/layout/Title.vue";
import BadgeTile from "@/components/home/BadgeTile.vue";
import HomeSummary from "@/components/home/HomeSummary.vue";
import LastTripCard from "@/components/home/LastTripCard.vue";

import { Component, Vue } from "vue-facing-decorator";

const DISPLAYED_BADGES_COUNT = 3;
const DEFAULT_BADGE_EMOJI = "🏅";
// Assez de sorties pour couvrir les 12 mois du résumé en un seul appel.
const SUMMARY_TRIPS_PAGE_SIZE = 500;

/** Accueil du pêcheur : dernière sortie, résumé de l'activité, badges. */
@Component({
  components: { Avatar, BottomNav, Title, BadgeTile, HomeSummary, LastTripCard },
})
export default class HomeView extends Vue {
  firstName: string = "";
  initials: string = "";
  tripsLoaded: boolean = false;
  trips: TripLight[] = [];
  markers: CatchMarker[] = [];
  markersLoaded: boolean = false;
  lastTripWaterName: string = "";
  lastTripPhotoUrl: string = "";
  badges: BadgeBean[] = [];
  defaultBadgeEmoji: string = DEFAULT_BADGE_EMOJI;

  mounted() {
    ProfileService.getProfile().then(this.profileLoaded, () => undefined);
    TripsService.listTrips(true, "", 0, SUMMARY_TRIPS_PAGE_SIZE).then(
      this.tripsListed,
      this.tripsFailed
    );
    TripsService.catchMarkers().then(this.markersListed, () => undefined);
    DashboardService.loadDashboardOrTimeout(undefined, "").then(
      this.dashboardLoaded,
      () => undefined
    );
    GamificationService.getMyBadges().then(
      (badges) => (this.badges = badges),
      () => undefined
    );
  }

  profileLoaded(profile: UserProfile) {
    this.firstName = profile.firstName;
    this.initials = profile.initials;
  }

  tripsListed(data: TripsAndCount) {
    this.tripsLoaded = true;
    this.trips = data.trips;
    const lastTrip = this.lastTrip;
    if (lastTrip && lastTrip.waterEntityId) {
      ReferentialService.getWaterEntityById(lastTrip.waterEntityId).then(
        (water) => (this.lastTripWaterName = water ? water.name : "")
      );
    }
  }

  tripsFailed() {
    this.tripsLoaded = true;
  }

  markersListed(markers: CatchMarker[]) {
    this.markers = markers;
    this.markersLoaded = true;
  }

  dashboardLoaded(data: DashboardAndSpecies) {
    const pictures = (data.dashboard.picturesPerTrip || []).find(
      (p) => this.lastTrip && p.tripId === this.lastTrip.id
    );
    if (pictures && pictures.pictureURLs.length) {
      this.lastTripPhotoUrl = Constants.apiUrl(pictures.pictureURLs[0]);
    }
  }

  get lastTrip(): TripLight | null {
    const sorted = this.trips
      .slice()
      .sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());
    return sorted.length ? sorted[0] : null;
  }

  get unlockedBadgesCount(): number {
    return this.badges.filter((b) => b.unlocked).length;
  }

  /** Derniers badges débloqués, complétés par les prochains à débloquer. */
  get displayedBadges(): BadgeBean[] {
    const unlocked = this.badges
      .filter((b) => b.unlocked)
      .sort((a, b) => new Date(b.unlockedAt).getTime() - new Date(a.unlockedAt).getTime());
    const locked = this.badges.filter((b) => !b.unlocked);
    return [...unlocked, ...locked].slice(0, DISPLAYED_BADGES_COUNT);
  }

  startNewTrip() {
    RouterUtils.startNewTrip(this.$router);
  }

  goAccount() {
    RouterUtils.pushRouteNoDuplicate(this.$router, "/account");
  }

  goBadges() {
    RouterUtils.pushRouteNoDuplicate(this.$router, "/badges");
  }

  goActivitySummary() {
    RouterUtils.pushRouteNoDuplicate(this.$router, "/activity-summary");
  }
}
</script>

<style scoped lang="less">
.home {
  position: relative;
  height: 100%;
  background-color: @white-smoke;

  @media screen and (min-width: @desktop-min-width) {
    height: 100vh;
  }
}

.home-content {
  height: calc(100% - @footer-height);
  overflow-y: auto;
  padding: 0 12px @margin-x-large 12px;
  margin-top: env(safe-area-inset-top);

  @media screen and (min-width: @desktop-min-width) {
    height: 100%;
    max-width: 900px;
    padding: @margin-large @margin-large-desktop;
  }
}

.home-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0 0 2px;

  @media screen and (min-width: @desktop-min-width) {
    justify-content: flex-end;
    padding: 0;
  }

  .home-avatar {
    width: 56px;
    height: 56px;
    font-size: @fontsize-span-big;
  }
}

.home-greeting {
  margin: 23px 0 0 2px;
  color: @heading;
  font-size: 27px;
  font-weight: 700;
  line-height: 1.2;
  letter-spacing: @letter-spacing;
}

.home-section-title {
  margin: 30px 0 18px 0;
  color: @heading;
  font-size: 22px;
  font-weight: 600;
  line-height: 1.2;
  letter-spacing: @letter-spacing;
}

.home-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: @margin-medium;
  padding: @margin-medium;
  border-radius: 13.118px;
  background-color: @surface;
  color: @pale-sky;
  text-align: center;
}

.home-badges-header {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-top: 40px;

  .home-section-title {
    margin: 0 0 26px 2px;
  }

  .badges-count {
    color: @heading;
    font-size: 18px;
    font-weight: 500;
    letter-spacing: @letter-spacing;

    strong {
      color: @accent-text;
      font-size: 22px;
      font-weight: 700;
    }
  }
}

.home-badges {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;

  @media screen and (min-width: @desktop-min-width) {
    grid-template-columns: repeat(3, 160px);
  }
}

.home-more {
  display: block;
  margin: 24px 4px 0 auto;
  padding: 0;
  border: none;
  background: none;
  color: @heading;
  font-size: 18px;
  font-weight: 500;
  letter-spacing: @letter-spacing;

  &:active {
    animation: none;
  }
}
</style>
