/*-
 * #%L
 * Carnet de pêche :: Mobile
 * %%
 * Copyright (C) 2019 - 2026 INRAE - UMR CARRTEL
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
import { flushPromises, mount } from "@vue/test-utils";
import SpeciesCombobox from "@/components/common/SpeciesCombobox.vue";

const species = (id: string, name: string, scientificName?: string, alias?: string) => ({
  id, name, scientificName, alias, builtIn: true, mandatorySize: false, mandatoryReport: false,
  archived: false, authorizedSample: false, minSize: 0, maxSize: 1000,
});

const OPTIONS = [
  species("bream", "Brème", "Abramis brama"),
  species("pike", "Brochet", "Esox lucius"),
  species("perch", "Perche", "Perca fluviatilis"),
  species("trout", "Truite fario", "Salmo trutta", "Truite"),
  species("__other__", "Autre ..."),
];

const mountCombobox = (modelValue?: string, readonly = false) =>
  mount(SpeciesCombobox as any, {
    props: { name: "species", label: "Espèce", options: OPTIONS, modelValue, readonly, pinnedOptionId: "__other__" },
    attachTo: document.body,
  });

const optionNames = () => Array.from(document.querySelectorAll('[role="option"]')).map((o) => o.textContent!.trim());

/** Champ « Espèce » à autocomplétion du formulaire de capture (#311). */
describe("SpeciesCombobox", () => {
  afterEach(() => {
    document.body.innerHTML = "";
  });

  it("n'a qu'un champ, de rôle combobox", () => {
    const wrapper = mountCombobox();
    expect(wrapper.findAll("input").length).toBe(1);
    expect(wrapper.find("input").attributes("role")).toBe("combobox");
    expect(wrapper.find("select").exists()).toBe(false);
  });

  it("ouvre la liste complète d'un appui sur le champ", async () => {
    const wrapper = mountCombobox();
    await wrapper.find("input").trigger("click");
    await flushPromises();
    expect(optionNames()).toEqual(OPTIONS.map((o) => o.name));
    await wrapper.find("input").trigger("click");
    await flushPromises();
    expect(optionNames().length).toBe(OPTIONS.length);
  });

  it("réduit la liste aux espèces correspondant aux premières lettres, « Autre » en dernier", async () => {
    const wrapper = mountCombobox();
    await wrapper.find("input").setValue("bro");
    await flushPromises();
    expect(optionNames()).toEqual(["Brochet", "Autre ..."]);
  });

  it("cherche aussi par nom scientifique et sans accents", async () => {
    const wrapper = mountCombobox();
    await wrapper.find("input").setValue("esox");
    await flushPromises();
    expect(optionNames()).toEqual(["Brochet", "Autre ..."]);
    await wrapper.find("input").setValue("BREME");
    await flushPromises();
    expect(optionNames()).toEqual(["Brème", "Autre ..."]);
  });

  it("sélectionne l'espèce choisie et ferme la liste", async () => {
    const wrapper = mountCombobox();
    await wrapper.find("input").setValue("bro");
    await flushPromises();
    await wrapper.find('[role="option"]').trigger("mousedown", { button: 0 });
    await flushPromises();
    expect(wrapper.emitted("update:modelValue")![0]).toEqual(["pike"]);
    expect(document.querySelectorAll('[role="option"]').length).toBe(0);
  });

  it("se pilote au clavier : flèches, Entrée, Échap", async () => {
    const wrapper = mountCombobox();
    const input = wrapper.find("input");
    await input.setValue("tru");
    await flushPromises();
    await input.trigger("keydown", { key: "Enter" });
    await flushPromises();
    expect(wrapper.emitted("update:modelValue")![0]).toEqual(["trout"]);

    await input.trigger("keydown", { key: "ArrowDown" });
    await flushPromises();
    expect(optionNames().length).toBe(OPTIONS.length);
    await input.trigger("keydown", { key: "Escape" });
    await flushPromises();
    expect(optionNames().length).toBe(0);
  });

  it("rouvre la liste complète après une recherche abandonnée par Échap", async () => {
    const wrapper = mountCombobox("perch");
    const input = wrapper.find("input");
    await input.setValue("bro");
    await flushPromises();
    await input.trigger("keydown", { key: "Escape" });
    await flushPromises();
    expect((input.element as HTMLInputElement).value).toBe("Perche");
    await input.trigger("keydown", { key: "ArrowDown" });
    await flushPromises();
    expect(optionNames()).toEqual(OPTIONS.map((o) => o.name));
  });

  it("affiche l'espèce d'une capture existante, et la liste complète à l'ouverture", async () => {
    const wrapper = mountCombobox("trout");
    await flushPromises();
    expect((wrapper.find("input").element as HTMLInputElement).value).toBe("Truite fario");
    await wrapper.find("button").trigger("click");
    await flushPromises();
    expect(optionNames()).toEqual(OPTIONS.map((o) => o.name));
  });

  it("en lecture seule, affiche simplement le nom de l'espèce", () => {
    const wrapper = mountCombobox("perch", true);
    expect(wrapper.find("input").exists()).toBe(false);
    expect(wrapper.text()).toContain("Perche");
  });
});
