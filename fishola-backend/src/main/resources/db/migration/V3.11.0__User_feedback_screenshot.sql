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

-- Capture d'écran jointe à un retour utilisateur, conservée pour que les
-- administrateurs nationaux la consultent depuis la page « Retours utilisateurs ».
-- Table à part pour que la liste des retours ne charge pas les images.
-- Dépend uniquement de V3.10.0, qui la précède aussi dans l'ordre lexical de la concat jOOQ.

CREATE TABLE user_feedback_screenshot (
    feedback_id uuid NOT NULL,
    content bytea NOT NULL,
    CONSTRAINT user_feedback_screenshot_pkey PRIMARY KEY (feedback_id),
    CONSTRAINT user_feedback_screenshot_feedback_fkey FOREIGN KEY (feedback_id)
        REFERENCES user_feedback (id) ON DELETE CASCADE
);

COMMENT ON TABLE user_feedback_screenshot IS 'Capture d''écran (PNG) jointe à un retour utilisateur';
COMMENT ON COLUMN user_feedback.has_screenshot IS 'Une capture d''écran a été jointe (conservée dans user_feedback_screenshot depuis V3.11.0)';
