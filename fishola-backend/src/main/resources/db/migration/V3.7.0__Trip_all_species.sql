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

-- « Toutes espèces » (#305) : le pêcheur indique qu'il ne cible pas d'espèce en
-- particulier. Valeur explicite, distincte d'une sortie sans espèce renseignée
-- (aucune ligne dans trip_expected_species et all_species = false).
ALTER TABLE public.trip ADD COLUMN all_species boolean DEFAULT false NOT NULL;

COMMENT ON COLUMN public.trip.all_species IS 'Le pêcheur ne cible pas d''espèce en particulier (« Toutes espèces », #305) ; exclut trip_expected_species';

-- espece_recherchee des vues d'export (catchs_openadom_export, catchs_export,
-- personal_catchs_export, catchs_pending_validation) : « Toutes espèces » pour ces
-- sorties, normalisé en toutes_espèces comme les noms d'espèces.
CREATE OR REPLACE VIEW public.trip_species_names AS
 SELECT tes.trip_id,
    string_agg(s.export_as, ','::text ORDER BY s.export_as) AS species
   FROM (public.trip_expected_species tes
     JOIN public.species s ON ((s.id = tes.species_id)))
  GROUP BY tes.trip_id
UNION ALL
 SELECT t.id AS trip_id,
    'Toutes espèces'::text AS species
   FROM public.trip t
  WHERE t.all_species
    AND NOT EXISTS (SELECT 1 FROM public.trip_expected_species tes WHERE tes.trip_id = t.id);

COMMENT ON VIEW public.trip_species_names IS 'Permet d''avoir, pour chaque sortie, la liste des espèces recherchées séparées par des virgules, ou « Toutes espèces » (#305)';
