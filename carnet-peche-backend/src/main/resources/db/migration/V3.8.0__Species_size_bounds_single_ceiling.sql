--
-- #%L
-- Carnet de pêche :: Backend
-- %%
-- Copyright (C) 2019 - 2026 INRAE - UMR CARRTEL
-- %%
-- This program is free software: you can redistribute it and/or modify
-- it under the terms of the GNU Affero General Public License as published by
-- the Free Software Foundation, either version 3 of the License, or
-- (at your option) any later version.
--
-- This program is distributed in the hope that it will be useful,
-- but WITHOUT ANY WARRANTY; without even the implied warranty of
-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
-- GNU General Public License for more details.
--
-- You should have received a copy of the GNU Affero General Public License
-- along with this program.  If not, see <http://www.gnu.org/licenses/>.
-- #L%
--

-- Bornes de taille aberrante : plafond unique 1–300 cm pour toutes les espèces.
-- Les valeurs de « Liste sp v1.xlsx » (scripts/species_taxon_seed.sql) sont des
-- étendues observées, pas des bornes plausibles (Sandre 3–13 cm, Brochet 45–45,
-- Silure 30–53…) : elles rejetaient des captures réelles. Seules les erreurs de
-- saisie grossières (unité oubliée, frappe en mm) restent bloquées, en attendant
-- des bornes par espèce fournies par l'UFBRMC. Les bornes de masse sont conservées.
INSERT INTO public.species_size_bounds (species_id, min_size_cm, max_size_cm)
SELECT s.id, 1, 300
FROM public.species s
ON CONFLICT (species_id) DO UPDATE
SET min_size_cm = EXCLUDED.min_size_cm,
    max_size_cm = EXCLUDED.max_size_cm;
