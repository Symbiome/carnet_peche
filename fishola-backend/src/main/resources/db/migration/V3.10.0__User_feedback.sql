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

-- Retours utilisateurs (formulaire « Un retour ? » de l'app pêcheur et de
-- l'administration) : jusqu'ici seulement envoyés par e-mail, ils sont désormais
-- conservés pour être consultés et exportés par les administrateurs nationaux.
-- La capture d'écran n'est pas stockée : elle reste jointe à l'e-mail.
-- Sans dépendance vers une autre migration (cf. ordre lexical de la concat jOOQ).

CREATE TABLE user_feedback (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    received_on timestamp with time zone DEFAULT now() NOT NULL,
    source text NOT NULL,
    category text NOT NULL,
    module text,
    email text,
    description text,
    has_screenshot boolean DEFAULT false NOT NULL,
    location text,
    frontend_version text,
    backend_version text,
    browser text,
    os text,
    platform text,
    device text,
    screen_resolution text,
    display_size text,
    locale text,
    CONSTRAINT user_feedback_pkey PRIMARY KEY (id)
);

CREATE INDEX user_feedback_category_received_on_idx ON user_feedback (category, received_on DESC);

COMMENT ON TABLE user_feedback IS 'Retours utilisateurs envoyés depuis l''app pêcheur ou l''administration, consultables par les administrateurs nationaux';
COMMENT ON COLUMN user_feedback.received_on IS 'Date de réception par le backend';
COMMENT ON COLUMN user_feedback.source IS 'Application d''origine : « Application pêcheur » ou « Administration »';
COMMENT ON COLUMN user_feedback.category IS 'Catégorie choisie par l''utilisateur (Bug, Ergonomie, Suggestion d''amélioration…)';
COMMENT ON COLUMN user_feedback.module IS 'Partie de l''application concernée, si précisée';
COMMENT ON COLUMN user_feedback.email IS 'E-mail de l''auteur, si connu';
COMMENT ON COLUMN user_feedback.description IS 'Texte libre saisi par l''utilisateur';
COMMENT ON COLUMN user_feedback.has_screenshot IS 'Une capture d''écran a été jointe (envoyée par e-mail uniquement)';
COMMENT ON COLUMN user_feedback.location IS 'URL de la page d''où le retour a été envoyé';
