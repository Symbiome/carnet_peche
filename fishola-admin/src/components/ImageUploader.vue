<!--
  #%L
  Carnet de pêche :: Admin
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
  <div>
    <input
      ref="upload"
      type="file"
      id="avatar"
      name="avatar"
      accept="image/*"
      @change="uploadImageFile"
    />
  </div>
</template>

<script setup lang="ts">
import BackendService from "@/services/BackendService";
import Constants from "@/services/Constants";
import { useTemplateRef } from "vue";
import { useToast } from "buefy";

interface Props {
  itemId: string;
  isMiniature?: boolean;
}

const {
  itemId,
  isMiniature = false,
} = defineProps<Props>();

const emit = defineEmits<{
  (e: "uploadedPic", url: string): void,
}>();

const uploadInput = useTemplateRef("upload");
const Toast = useToast();

async function uploadImageFile(payload: Event) {
  const uploadedFile = uploadInput.value as HTMLInputElement;
  if (uploadedFile.files && uploadedFile.files.length > 0) {
    const file = uploadedFile.files[0];
    const id = itemId ? itemId : "temp-id";
    let url = "/v1/news-picture/";
    if (isMiniature) {
      url = "/v1/news-miniature/";
    }
    try {
      const jpegDataUrl = await toJpegDataUrl(file);
      const newsPicture = await BackendService.backendPost(url + id, jpegDataUrl);
      const newsPicturesURL = Constants.apiUrl(
        "/v1/news-picture/" + newsPicture["id"]
      );
      emit("uploadedPic", newsPicturesURL);
    } catch (e) {
      Toast.open({
        message: "Impossible d'ajouter cette image : format non reconnu",
        type: "is-danger"
      });
    } finally {
      uploadedFile.value = "";
    }
  }
}

/**
 * Re-encode any image the browser can display (WebP, AVIF, GIF, PNG…) as a
 * JPEG data URL, the backend only decoding JPEG and PNG (#233).
 */
async function toJpegDataUrl(file: File): Promise<string> {
  const bitmap = await createImageBitmap(file);
  const canvas = document.createElement("canvas");
  canvas.width = bitmap.width;
  canvas.height = bitmap.height;
  const context = canvas.getContext("2d") as CanvasRenderingContext2D;
  context.fillStyle = "#ffffff";
  context.fillRect(0, 0, canvas.width, canvas.height);
  context.drawImage(bitmap, 0, 0);
  bitmap.close();
  return canvas.toDataURL("image/jpeg", 0.9);
}
</script>
