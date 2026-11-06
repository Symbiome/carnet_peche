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
  <section class="newsletter-page">
    <header class="page-header">
      <div>
        <h1 class="title">Inscrits à la newsletter</h1>
        <p class="subtitle is-6">
          Visiteurs de la page d'accueil prévenus de l'ouverture de l'app. Les désinscrits n'apparaissent pas.
        </p>
      </div>
      <b-button type="is-primary" icon-left="file-download-outline" :loading="exporting"
                :disabled="subscribers.length === 0" @click="exportCsv">
        Export CSV
      </b-button>
    </header>

    <p class="help-note">
      Seules les adresses confirmées (double opt-in) peuvent recevoir un envoi. L'export contient le lien de
      désinscription personnel de chaque inscrit : reprenez-le dans tout e-mail envoyé depuis un autre outil.
    </p>

    <b-table :data="subscribers" :loading="loading" :paginated="subscribers.length > 50" per-page="50"
             default-sort="subscribedOn" default-sort-direction="desc" hoverable narrowed>
      <b-table-column field="email" label="E-mail" sortable searchable v-slot="props">
        {{ props.row.email }}
      </b-table-column>
      <b-table-column field="departmentCode" label="Département" sortable v-slot="props">
        <span v-if="props.row.departmentCode">{{ props.row.departmentCode }} - {{ props.row.departmentName }}</span>
        <span v-else class="muted">—</span>
      </b-table-column>
      <b-table-column field="subscribedOn" label="Inscription" sortable v-slot="props">
        {{ formatDate(props.row.subscribedOn) }}
      </b-table-column>
      <b-table-column field="confirmedOn" label="Statut" sortable v-slot="props">
        <b-tag v-if="props.row.confirmedOn" type="is-success" :title="formatDate(props.row.confirmedOn)">Confirmé</b-tag>
        <b-tag v-else type="is-light">En attente</b-tag>
      </b-table-column>

      <template #empty>
        <div class="empty-state">{{ loading ? "Chargement…" : "Aucun inscrit pour le moment." }}</div>
      </template>
    </b-table>
  </section>
</template>

<script setup lang="ts">
import BackendService from "@/services/BackendService";
import { useToast } from "buefy";
import { onMounted, ref } from "vue";

interface NewsletterSubscriber {
  email: string;
  departmentCode: string | null;
  departmentName: string | null;
  subscribedOn: number;
  confirmedOn: number | null;
}

const Toast = useToast();
const subscribers = ref<NewsletterSubscriber[]>([]);
const loading = ref(false);
const exporting = ref(false);

/** Charge les inscrits du périmètre de l'administrateur connecté. */
async function load() {
  loading.value = true;
  try {
    subscribers.value = await BackendService.backendGet("/v1/admin/newsletter-subscriptions");
  } catch (err) {
    subscribers.value = [];
    Toast.open({ message: "Impossible de charger les inscrits", type: "is-danger" });
  } finally {
    loading.value = false;
  }
}

/** Télécharge l'export CSV généré par le backend. */
async function exportCsv() {
  exporting.value = true;
  try {
    const blob = await BackendService.backendGetBlob("/v1/admin/newsletter-subscriptions/export");
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `inscrits-newsletter_${new Date().toISOString().slice(0, 10)}.csv`;
    link.click();
    URL.revokeObjectURL(url);
  } catch (err) {
    Toast.open({ message: "L'export n'a pas pu être téléchargé, réessayez.", type: "is-danger" });
  } finally {
    exporting.value = false;
  }
}

function formatDate(at: number | string | null): string {
  if (at === null || at === undefined) return "";
  // Le backend sérialise les OffsetDateTime en secondes epoch, d'où le ×1000.
  const ms = typeof at === "number" ? at * 1000 : Date.parse(at);
  return new Date(ms).toLocaleString("fr-FR");
}

onMounted(load);
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

.muted {
  color: #aaa;
}

.empty-state {
  text-align: center;
  padding: 2rem;
  color: #888;
}
</style>
