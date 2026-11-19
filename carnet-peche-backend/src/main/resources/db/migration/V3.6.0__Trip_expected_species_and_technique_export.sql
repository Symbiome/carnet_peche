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

-- Espèce recherchée et technique de pêche des sorties hors application (#300).
--
-- L'app pêcheur enregistre ses techniques dans trip_techniques ; les imports et saisies
-- manuelles (enquête, sortie souvenir, carnet volontaire) ne recopiaient leur technique
-- principale que sur les captures, perdue pour une sortie bredouille. Ils l'enregistrent
-- désormais dans trip_techniques ; reprise de l'existant depuis les captures. Les
-- sorties bredouille déjà importées restent sans technique.
INSERT INTO public.trip_techniques (trip_id, technique_id)
SELECT DISTINCT c.trip_id, c.technique_id
  FROM public.catch c
  JOIN public.trip t ON t.id = c.trip_id
 WHERE t.collection_method <> 'saisie_pecheur'
   AND c.technique_id IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM public.trip_techniques tt WHERE tt.trip_id = t.id);

-- Vues d'export : reprise à l'identique de V3.1.0, sauf espece_recherchee qui se rabat
-- sur trip.expected_species_id (enquête, sortie souvenir, carnet volontaire) quand
-- trip_expected_species est vide.
CREATE OR REPLACE VIEW public.catchs_openadom_export AS
 SELECT 'fishola'::text AS nom_du_projet,
    public.normalize_for_export((l.export_as)::character varying) AS nom_du_site,
    public.normalize_for_export(((l.export_as || ':peche amateur'::text))::character varying) AS nom_de_la_plateforme,
    to_char(t.begin_timestamp, 'DD/MM/YYYY'::text) AS date_de_la_sortie,
    u.id AS id_login,
    to_char(t.begin_timestamp, 'MM'::text) AS mois_de_la_sortie,
    to_char(t.begin_timestamp, 'YYYY'::text) AS annee_de_la_sortie,
        CASE t.type
            WHEN 'Craft'::public.trip_type THEN 'embarcation'::text
            WHEN 'Border'::public.trip_type THEN 'bord'::text
            ELSE NULL::text
        END AS type_de_peche,
    t.id AS id_sortie,
    COALESCE(public.normalize_for_export((tsn.species)::character varying),
        public.normalize_for_export((es.export_as)::character varying)) AS espece_recherchee,
    to_char(t.begin_timestamp, 'HH24:MI:SS'::text) AS debut_de_peche,
    to_char(t.end_timestamp, 'HH24:MI:SS'::text) AS fin_de_peche,
    (t.end_timestamp - t.begin_timestamp) AS duree_de_la_sortie,
    ttn.techniques AS technique_de_peche_par_sortie,
    c.id AS id_capture,
    public.normalize_for_export((ct.export_as)::character varying) AS technique_de_peche_par_capture,
    public.normalize_for_export((s.export_as)::character varying) AS espece_capturee,
        CASE
            WHEN ((c.edited_size IS NOT NULL) AND (c.edited_size > 0)) THEN c.edited_size
            ELSE (c.size * 10)
        END AS longueur_totale_du_poisson,
    (c.automatic_measure * 10) AS longueur_totale_du_poisson_calculee,
        CASE
            WHEN ((c.edited_weight IS NOT NULL) AND (c.edited_weight > 0)) THEN c.edited_weight
            ELSE c.weight
        END AS poids_du_poisson,
        CASE c.kept
            WHEN true THEN 'non'::text
            WHEN false THEN 'oui'::text
            ELSE NULL::text
        END AS poisson_relache,
    c.sample_id AS id_prelevement,
    public.normalize_for_export((w.export_as)::character varying) AS conditions_meteo,
        CASE t.mode
            WHEN 'Live'::public.trip_mode THEN 'en_direct'::text
            WHEN 'Afterwards'::public.trip_mode THEN 'a_posteriori'::text
            ELSE NULL::text
        END AS mode_de_peche,
        CASE c.exclude_from_exports
            WHEN true THEN 'oui'::text
            ELSE 'non'::text
        END AS a_exclure,
    c.id AS catch_id,
    c.quantity AS nombre_de_poissons,
    c.department AS departement,
    c.certainty::text AS certitude,
        CASE
            WHEN ((c.certainty <> 'CERTAIN'::public.identification_certainty) AND (c.validated_at IS NULL)) THEN 'oui'::text
            ELSE 'non'::text
        END AS a_valider,
    (c.lot_min_size_cm * 10) AS taille_min_du_lot,
    (c.lot_max_size_cm * 10) AS taille_max_du_lot,
    to_char(t.end_timestamp, 'DD/MM/YYYY'::text) AS date_de_fin_de_la_sortie,
    (t.collection_method)::text AS origine_donnee,
    t.session_code AS code_session,
    s.code_espece AS code_espece_capturee,
    COALESCE(es.code_espece, tec.codes) AS code_espece_recherchee
   FROM (((((((((public.trip t
     JOIN public.water_entity l ON ((l.id = t.water_entity_id)))
     LEFT JOIN public.fishola_user u ON ((u.id = t.owner_id)))
     LEFT JOIN public.trip_species_names tsn ON ((tsn.trip_id = t.id)))
     LEFT JOIN public.trip_techniques_names ttn ON ((ttn.trip_id = t.id)))
     LEFT JOIN public.weather w ON ((w.id = t.weather_id)))
     LEFT JOIN public.catch c ON ((t.id = c.trip_id)))
     LEFT JOIN public.technique ct ON ((ct.id = c.technique_id)))
     LEFT JOIN public.species s ON ((s.id =
        CASE
            WHEN (c.edited_species_id IS NOT NULL) THEN c.edited_species_id
            ELSE c.species_id
        END)))
     LEFT JOIN public.catch_picture_joined_urls cpju ON ((cpju.catch_id = c.id)))
     LEFT JOIN public.species es ON ((es.id = t.expected_species_id))
     LEFT JOIN public.trip_expected_species_codes tec ON ((tec.trip_id = t.id))
  WHERE (((t.owner_id IS NULL) OR (u.exclude_from_exports = false)) AND ((t.collection_method IS DISTINCT FROM 'saisie_pecheur'::public.collection_method)
       OR (t.created_on < ((now() - '168:00:00'::interval) AT TIME ZONE 'Europe/Paris'::text))));

CREATE OR REPLACE VIEW public.catchs_pending_validation AS
 SELECT 'fishola'::text AS nom_du_projet,
    public.normalize_for_export((l.export_as)::character varying) AS nom_du_site,
    public.normalize_for_export(((l.export_as || ':peche amateur'::text))::character varying) AS nom_de_la_plateforme,
    to_char(t.begin_timestamp, 'DD/MM/YYYY'::text) AS date_de_la_sortie,
    u.id AS id_login,
    to_char(t.begin_timestamp, 'MM'::text) AS mois_de_la_sortie,
    to_char(t.begin_timestamp, 'YYYY'::text) AS annee_de_la_sortie,
        CASE t.type
            WHEN 'Craft'::public.trip_type THEN 'embarcation'::text
            WHEN 'Border'::public.trip_type THEN 'bord'::text
            ELSE NULL::text
        END AS type_de_peche,
    t.id AS id_sortie,
    COALESCE(public.normalize_for_export((tsn.species)::character varying),
        public.normalize_for_export((es.export_as)::character varying)) AS espece_recherchee,
    to_char(t.begin_timestamp, 'HH24:MI:SS'::text) AS debut_de_peche,
    to_char(t.end_timestamp, 'HH24:MI:SS'::text) AS fin_de_peche,
    (t.end_timestamp - t.begin_timestamp) AS duree_de_la_sortie,
    ttn.techniques AS technique_de_peche_par_sortie,
    c.id AS id_capture,
    public.normalize_for_export((ct.export_as)::character varying) AS technique_de_peche_par_capture,
    public.normalize_for_export((s.export_as)::character varying) AS espece_capturee,
        CASE
            WHEN ((c.edited_size IS NOT NULL) AND (c.edited_size > 0)) THEN c.edited_size
            ELSE (c.size * 10)
        END AS longueur_totale_du_poisson,
    (c.automatic_measure * 10) AS longueur_totale_du_poisson_calculee,
        CASE
            WHEN ((c.edited_weight IS NOT NULL) AND (c.edited_weight > 0)) THEN c.edited_weight
            ELSE c.weight
        END AS poids_du_poisson,
        CASE c.kept
            WHEN true THEN 'non'::text
            WHEN false THEN 'oui'::text
            ELSE NULL::text
        END AS poisson_relache,
    c.sample_id AS id_prelevement,
    public.normalize_for_export((w.export_as)::character varying) AS conditions_meteo,
        CASE t.mode
            WHEN 'Live'::public.trip_mode THEN 'en_direct'::text
            WHEN 'Afterwards'::public.trip_mode THEN 'a_posteriori'::text
            ELSE NULL::text
        END AS mode_de_peche,
        CASE c.exclude_from_exports
            WHEN true THEN 'oui'::text
            ELSE 'non'::text
        END AS a_exclure,
    c.id AS catch_id,
    c.quantity AS nombre_de_poissons,
    c.department AS departement,
    c.certainty::text AS certitude,
        CASE
            WHEN ((c.certainty <> 'CERTAIN'::public.identification_certainty) AND (c.validated_at IS NULL)) THEN 'oui'::text
            ELSE 'non'::text
        END AS a_valider,
    (c.lot_min_size_cm * 10) AS taille_min_du_lot,
    (c.lot_max_size_cm * 10) AS taille_max_du_lot,
    to_char(t.end_timestamp, 'DD/MM/YYYY'::text) AS date_de_fin_de_la_sortie,
    (t.collection_method)::text AS origine_donnee,
    t.session_code AS code_session,
    s.code_espece AS code_espece_capturee,
    COALESCE(es.code_espece, tec.codes) AS code_espece_recherchee
   FROM (((((((((public.trip t
     JOIN public.water_entity l ON ((l.id = t.water_entity_id)))
     LEFT JOIN public.fishola_user u ON ((u.id = t.owner_id)))
     LEFT JOIN public.trip_species_names tsn ON ((tsn.trip_id = t.id)))
     LEFT JOIN public.trip_techniques_names ttn ON ((ttn.trip_id = t.id)))
     LEFT JOIN public.weather w ON ((w.id = t.weather_id)))
     LEFT JOIN public.catch c ON ((t.id = c.trip_id)))
     LEFT JOIN public.technique ct ON ((ct.id = c.technique_id)))
     LEFT JOIN public.species s ON ((s.id =
        CASE
            WHEN (c.edited_species_id IS NOT NULL) THEN c.edited_species_id
            ELSE c.species_id
        END)))
     LEFT JOIN public.catch_picture_joined_urls cpju ON ((cpju.catch_id = c.id)))
     LEFT JOIN public.species es ON ((es.id = t.expected_species_id))
     LEFT JOIN public.trip_expected_species_codes tec ON ((tec.trip_id = t.id));
