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
import { mount } from "@vue/test-utils";
import FormInput from "@/components/common/FormInput.vue";

const mountNumberInput = () =>
  mount(FormInput as any, { props: { name: "size", label: "Taille", type: "number", modelValue: "" } });

const typeInto = async (input: HTMLInputElement, badInput: boolean, value: string) => {
  Object.defineProperty(input, "validity", { configurable: true, value: { badInput } });
  input.value = value;
  input.dispatchEvent(new Event("input"));
};

/**
 * Un champ numérique contenant un texte illisible (ex. « 12,5 ») n'est pas un
 * champ vide : le formulaire de capture affiche une erreur de saisie et non
 * « Vous devez renseigner les champs obligatoires ».
 */
describe("FormInput numérique", () => {
  it("signale une saisie illisible, que le navigateur renvoie vide", async () => {
    const wrapper = mountNumberInput();
    await typeInto(wrapper.find("input").element as HTMLInputElement, true, "");
    expect(wrapper.emitted("badInput")).toEqual([[true]]);
    expect(wrapper.emitted("update:modelValue")).toEqual([[""]]);
  });

  it("lève le signalement quand la saisie redevient lisible", async () => {
    const wrapper = mountNumberInput();
    const input = wrapper.find("input").element as HTMLInputElement;
    await typeInto(input, true, "");
    await typeInto(input, false, "12");
    expect(wrapper.emitted("badInput")).toEqual([[true], [false]]);
  });

  it("lève le signalement quand le champ disparaît", () => {
    const wrapper = mountNumberInput();
    wrapper.unmount();
    expect(wrapper.emitted("badInput")).toEqual([[false]]);
  });

  it("ne signale rien pour un champ texte", async () => {
    const wrapper = mount(FormInput as any, { props: { name: "name", label: "Nom", modelValue: "" } });
    await typeInto(wrapper.find("input").element as HTMLInputElement, false, "abc");
    expect(wrapper.emitted("badInput")).toBeUndefined();
  });
});
