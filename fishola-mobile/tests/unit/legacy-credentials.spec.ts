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

import { forgetStoredPassword } from "@/services/LegacyCredentials";

function memoryStorage(initial: Record<string, string>): Storage {
  const values = new Map(Object.entries(initial));
  return {
    get length() {
      return values.size;
    },
    clear: () => values.clear(),
    getItem: (key) => values.get(key) ?? null,
    key: (index) => Array.from(values.keys())[index] ?? null,
    removeItem: (key) => {
      values.delete(key);
    },
    setItem: (key, value) => {
      values.set(key, value);
    },
  };
}

// #123 : le mot de passe ne doit plus rester en clair sur l'appareil.
describe("forgetStoredPassword", () => {
  it("efface le mot de passe mémorisé et garde l'e-mail", () => {
    const storage = memoryStorage({ latestPassword: "secret", latestEmail: "pecheur@exemple.fr" });
    forgetStoredPassword(storage);
    expect(storage.getItem("latestPassword")).toBeNull();
    expect(storage.getItem("latestEmail")).toBe("pecheur@exemple.fr");
  });

  it("ne plante pas si le stockage est indisponible", () => {
    const broken = memoryStorage({});
    broken.removeItem = () => {
      throw new Error("SecurityError");
    };
    expect(() => forgetStoredPassword(broken)).not.toThrow();
  });
});
