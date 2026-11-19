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

-- Portée des communications (#233) : une actualité est nationale (publiée par
-- un administrateur national) ou rattachée aux départements du périmètre de
-- l'administrateur régional qui l'a publiée. Le rattachement à des milieux
-- (news_water_entity) disparaît : les actualités existantes reprennent les
-- départements de leurs milieux, buffer staff compris (water_entity_department).

CREATE TABLE news_department (
    news_id uuid NOT NULL REFERENCES news (id) ON DELETE CASCADE,
    department_code character varying(3) NOT NULL,
    CONSTRAINT news_department_pkey PRIMARY KEY (news_id, department_code)
);

CREATE INDEX news_department_department_code_idx ON news_department (department_code);

INSERT INTO news_department (news_id, department_code)
SELECT DISTINCT nwe.news_id, wed.department_code
FROM news_water_entity nwe
JOIN news n ON n.id = nwe.news_id
JOIN water_entity_department wed ON wed.water_entity_id = nwe.water_entity_id
WHERE n.is_national IS NOT TRUE;

DROP TABLE news_water_entity;

UPDATE news SET is_national = false WHERE is_national IS NULL;
ALTER TABLE news ALTER COLUMN is_national SET NOT NULL;

COMMENT ON TABLE news_department IS 'Départements ciblés par une actualité régionale (#233) ; aucune ligne pour une actualité nationale';
