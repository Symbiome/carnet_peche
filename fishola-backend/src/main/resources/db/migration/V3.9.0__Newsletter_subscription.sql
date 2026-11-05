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

-- Inscription à la newsletter depuis la page d'accueil publique (#313), tant que
-- l'inscription à l'app est fermée : les visiteurs laissent leur e-mail pour être
-- prévenus de l'ouverture. Double opt-in : l'adresse n'est confirmée qu'après le
-- clic sur le lien reçu. Une ligne par e-mail ; une désinscription est conservée
-- (date) pour ne plus jamais exporter l'adresse, sauf nouvelle inscription.

CREATE TABLE newsletter_subscription (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    email text NOT NULL,
    department_code character varying(3),
    subscribed_on timestamp with time zone DEFAULT now() NOT NULL,
    confirmed_on timestamp with time zone,
    token uuid NOT NULL,
    unsubscribed_on timestamp with time zone,
    CONSTRAINT newsletter_subscription_pkey PRIMARY KEY (id),
    CONSTRAINT newsletter_subscription_email_key UNIQUE (email),
    CONSTRAINT newsletter_subscription_token_key UNIQUE (token)
);

COMMENT ON TABLE newsletter_subscription IS 'Inscrits à la newsletter depuis la page d''accueil publique (#313), double opt-in';
COMMENT ON COLUMN newsletter_subscription.email IS 'E-mail en minuscules, unique';
COMMENT ON COLUMN newsletter_subscription.department_code IS 'Département déclaré (optionnel), pour cibler la communication par fédération';
COMMENT ON COLUMN newsletter_subscription.subscribed_on IS 'Date de la dernière demande d''inscription';
COMMENT ON COLUMN newsletter_subscription.confirmed_on IS 'Date du clic sur le lien de confirmation ; null tant que l''adresse n''est pas confirmée';
COMMENT ON COLUMN newsletter_subscription.token IS 'Jeton des liens de confirmation et de désinscription, renouvelé à chaque nouvelle inscription';
COMMENT ON COLUMN newsletter_subscription.unsubscribed_on IS 'Date de désinscription ; l''inscrit n''apparaît plus dans la liste ni dans l''export';
