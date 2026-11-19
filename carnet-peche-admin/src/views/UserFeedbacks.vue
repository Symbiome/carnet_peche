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
  <section class="feedbacks-page">
    <header class="page-header">
      <div>
        <h1 class="title">Retours utilisateurs</h1>
        <p class="subtitle is-6">
          Retours envoyés depuis l'app pêcheur et l'administration, classés par catégorie.
        </p>
      </div>
      <div class="buttons">
        <b-button type="is-primary" icon-left="microsoft-excel" :loading="exporting === 'xlsx'"
                  :disabled="feedbacks.length === 0" @click="download('xlsx')">
          Export Excel
        </b-button>
        <b-button icon-left="file-download-outline" :loading="exporting === 'csv'"
                  :disabled="feedbacks.length === 0" @click="download('csv')">
          Export CSV
        </b-button>
      </div>
    </header>

    <p class="help-note">
      Dépliez une ligne (flèche à gauche) pour lire le retour en entier et voir le contexte technique.
      Cliquez sur « Voir » pour afficher la capture d'écran jointe.
    </p>

    <b-tabs v-model="activeCategory" class="category-tabs">
      <b-tab-item v-for="tab in tabs" :key="tab.value" :value="tab.value" :label="`${tab.label} (${tab.count})`" />
    </b-tabs>

    <b-table :data="visibleFeedbacks" :loading="loading" :paginated="visibleFeedbacks.length > 50" per-page="50"
             default-sort="receivedOn" default-sort-direction="desc" detailed detail-key="id"
             :show-detail-icon="true" hoverable narrowed>
      <b-table-column field="receivedOn" label="Date" sortable v-slot="props">
        <span class="nowrap">{{ formatDate(props.row.receivedOn) }}</span>
      </b-table-column>
      <b-table-column field="category" label="Catégorie" sortable :visible="activeCategory === ALL" v-slot="props">
        <b-tag :type="categoryTagType(props.row.category)">{{ props.row.category }}</b-tag>
      </b-table-column>
      <b-table-column field="source" label="Origine" sortable v-slot="props">
        {{ props.row.source }}
      </b-table-column>
      <b-table-column field="module" label="Module" sortable v-slot="props">
        <span v-if="props.row.module">{{ props.row.module }}</span>
        <span v-else class="muted">—</span>
      </b-table-column>
      <b-table-column field="email" label="E-mail" sortable searchable v-slot="props">
        <a v-if="props.row.email" :href="`mailto:${props.row.email}`">{{ props.row.email }}</a>
        <span v-else class="muted">—</span>
      </b-table-column>
      <b-table-column field="description" label="Description" searchable v-slot="props">
        <span v-if="props.row.description" class="description-preview">{{ props.row.description }}</span>
        <span v-else class="muted">Pas de description</span>
      </b-table-column>
      <b-table-column field="hasScreenshot" label="Capture" sortable centered v-slot="props">
        <b-button v-if="props.row.hasScreenshot" size="is-small" icon-left="image-outline"
                  :loading="screenshotLoadingId === props.row.id" @click.stop="openScreenshot(props.row)">
          Voir
        </b-button>
      </b-table-column>

      <template #detail="props">
        <div class="feedback-detail">
          <p class="description-full">{{ props.row.description || "Pas de description" }}</p>
          <dl class="tech-context">
            <template v-for="item in technicalContext(props.row)" :key="item.label">
              <dt>{{ item.label }}</dt>
              <dd>{{ item.value }}</dd>
            </template>
          </dl>
        </div>
      </template>

      <template #empty>
        <div class="empty-state">{{ loading ? "Chargement…" : "Aucun retour pour le moment." }}</div>
      </template>
    </b-table>

    <b-modal v-model="screenshotOpen" aria-role="dialog" aria-modal width="auto">
      <figure class="screenshot-viewer">
        <img :src="screenshotUrl" :alt="screenshotCaption" />
        <figcaption>{{ screenshotCaption }}</figcaption>
      </figure>
    </b-modal>
  </section>
</template>

<script setup lang="ts">
import BackendService from "@/services/BackendService";
import { useToast } from "buefy";
import { computed, onMounted, onUnmounted, ref, watch } from "vue";

interface UserFeedback {
  id: string;
  receivedOn: number;
  source: string;
  category: string;
  module: string | null;
  email: string | null;
  description: string | null;
  hasScreenshot: boolean;
  location: string | null;
  frontendVersion: string | null;
  backendVersion: string | null;
  browser: string | null;
  os: string | null;
  platform: string | null;
  device: string | null;
  screenResolution: string | null;
  displaySize: string | null;
  locale: string | null;
}

interface CategoryTab {
  value: string;
  label: string;
  count: number;
}

const ALL = "__all__";
const CATEGORY_ORDER = [
  "Bug",
  "Ergonomie",
  "Contenu",
  "Fonctionnalité manquante",
  "Performance",
  "Suggestion d'amélioration",
  "Autre",
];

const Toast = useToast();
const feedbacks = ref<UserFeedback[]>([]);
const loading = ref(false);
const exporting = ref<"" | "csv" | "xlsx">("");
const activeCategory = ref(ALL);
const screenshotOpen = ref(false);
const screenshotUrl = ref("");
const screenshotCaption = ref("");
const screenshotLoadingId = ref("");

const tabs = computed<CategoryTab[]>(() => {
  const counts = new Map<string, number>();
  feedbacks.value.forEach((feedback) => counts.set(feedback.category, (counts.get(feedback.category) ?? 0) + 1));
  const categories = [...counts.keys()].sort(compareCategories);
  return [
    { value: ALL, label: "Toutes", count: feedbacks.value.length },
    ...categories.map((category) => ({ value: category, label: category, count: counts.get(category) ?? 0 })),
  ];
});

const visibleFeedbacks = computed(() =>
  activeCategory.value === ALL
    ? feedbacks.value
    : feedbacks.value.filter((feedback) => feedback.category === activeCategory.value)
);

/** Ordonne les catégories comme dans le formulaire de retour, les inconnues ensuite par ordre alphabétique. */
function compareCategories(a: string, b: string): number {
  const rankA = CATEGORY_ORDER.indexOf(a);
  const rankB = CATEGORY_ORDER.indexOf(b);
  if (rankA === -1 && rankB === -1) return a.localeCompare(b, "fr");
  if (rankA === -1) return 1;
  if (rankB === -1) return -1;
  return rankA - rankB;
}

/** Couleur d'étiquette par catégorie, pour repérer les bugs d'un coup d'œil. */
function categoryTagType(category: string): string {
  if (category === "Bug") return "is-danger is-light";
  if (category === "Performance") return "is-warning is-light";
  return "is-info is-light";
}

/** Contexte technique renseigné du retour, pour l'affichage détaillé. */
function technicalContext(feedback: UserFeedback): { label: string; value: string }[] {
  const items: [string, string | null][] = [
    ["Page", feedback.location],
    ["Version frontend", feedback.frontendVersion],
    ["Version backend", feedback.backendVersion],
    ["Navigateur", feedback.browser],
    ["OS", feedback.os],
    ["Plateforme", feedback.platform],
    ["Appareil", feedback.device],
    ["Résolution écran", feedback.screenResolution],
    ["Taille d'affichage", feedback.displaySize],
    ["Langue", feedback.locale],
  ];
  return items.filter(([, value]) => !!value).map(([label, value]) => ({ label, value: value as string }));
}

/** Charge tous les retours utilisateurs (administrateur national uniquement). */
async function load() {
  loading.value = true;
  try {
    feedbacks.value = await BackendService.backendGet("/v1/admin/feedbacks");
  } catch (err) {
    feedbacks.value = [];
    Toast.open({ message: "Impossible de charger les retours utilisateurs", type: "is-danger" });
  } finally {
    loading.value = false;
  }
}

/** Télécharge l'export Excel ou CSV généré par le backend. */
async function download(format: "csv" | "xlsx") {
  exporting.value = format;
  try {
    const blob = await BackendService.backendGetBlob(`/v1/admin/feedbacks/export?format=${format}`);
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `retours-utilisateurs_${new Date().toISOString().slice(0, 10)}.${format}`;
    link.click();
    URL.revokeObjectURL(url);
  } catch (err) {
    Toast.open({ message: "L'export n'a pas pu être téléchargé, réessayez.", type: "is-danger" });
  } finally {
    exporting.value = "";
  }
}

/** Charge la capture d'écran du retour et l'affiche en grand dans une fenêtre. */
async function openScreenshot(feedback: UserFeedback) {
  screenshotLoadingId.value = feedback.id;
  try {
    const blob = await BackendService.backendGetBlob(`/v1/admin/feedbacks/${feedback.id}/screenshot`);
    closeScreenshot();
    screenshotUrl.value = URL.createObjectURL(blob);
    screenshotCaption.value = `${feedback.category} · ${feedback.email ?? "anonyme"} · ${formatDate(feedback.receivedOn)}`;
    screenshotOpen.value = true;
  } catch (err) {
    Toast.open({ message: "La capture d'écran n'a pas pu être chargée.", type: "is-danger" });
  } finally {
    screenshotLoadingId.value = "";
  }
}

/** Libère l'image affichée. */
function closeScreenshot() {
  if (screenshotUrl.value) {
    URL.revokeObjectURL(screenshotUrl.value);
  }
  screenshotUrl.value = "";
}

function formatDate(at: number | string | null): string {
  if (at === null || at === undefined) return "";
  // Le backend sérialise les OffsetDateTime en secondes epoch, d'où le ×1000.
  const ms = typeof at === "number" ? at * 1000 : Date.parse(at);
  return new Date(ms).toLocaleString("fr-FR", { dateStyle: "short", timeStyle: "short" });
}

watch(screenshotOpen, (open) => {
  if (!open) closeScreenshot();
});

onMounted(load);
onUnmounted(closeScreenshot);
</script>

<style scoped lang="less">
.page-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
  margin-bottom: 1rem;
}

.help-note {
  color: #6b7780;
  font-size: 0.9rem;
  margin-bottom: 1rem;
}

.category-tabs :deep(.tab-content) {
  display: none;
}

.nowrap {
  white-space: nowrap;
}

.description-preview {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  max-width: 40rem;
}

.feedback-detail {
  display: grid;
  gap: 1rem;
}

.description-full {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.tech-context {
  display: grid;
  grid-template-columns: max-content 1fr;
  gap: 0.25rem 1rem;
  font-size: 0.85rem;
  color: #4a5560;

  dt {
    font-weight: 600;
  }

  dd {
    overflow-wrap: anywhere;
  }
}

.screenshot-viewer {
  background: #fff;
  padding: 0.75rem;
  border-radius: 6px;
  text-align: center;

  img {
    display: block;
    max-width: 90vw;
    max-height: 80vh;
    margin: 0 auto;
  }

  figcaption {
    margin-top: 0.5rem;
    font-size: 0.85rem;
    color: #4a5560;
  }
}

.muted {
  color: #aaa;
}

.empty-state {
  text-align: center;
  padding: 2rem;
  color: #888;
}
</style>
