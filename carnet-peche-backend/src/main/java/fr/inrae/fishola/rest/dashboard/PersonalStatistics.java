package fr.inrae.fishola.rest.dashboard;

/*-
 * #%L
 * Carnet de pêche :: Backend
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

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import fr.inrae.fishola.ImmutableObject;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Statistiques du pêcheur sur une période (page « Mes statistiques ») :
 * activité jour par jour, moments de sortie, prises par espèce, qualité des
 * fiches et milieux fréquentés. Sorties non masquées dont le début tombe dans
 * la période ; les prises comptent le nombre de poissons des lots (#148).
 */
@ImmutableObject
@JsonSerialize(as = ImmutablePersonalStatistics.class)
public interface PersonalStatistics {

    /** Premier jour de la période, inclus. */
    LocalDate from();

    /** Dernier jour de la période, inclus. */
    LocalDate to();

    /** Jours avec au moins une sortie, dans l'ordre chronologique. */
    List<DailyActivity> dailyActivity();

    /** Nombre de sorties selon l'heure de début (moments sans sortie absents). */
    Map<DayMoment, Integer> tripsPerMoment();

    /** Nombre de poissons capturés par espèce. */
    Map<UUID, Integer> catchesPerSpecies();

    /**
     * Sorties dont la fiche est complète : position GPS de la sortie connue,
     * et chaque prise avec une taille et au moins une photo.
     */
    int completeTripsCount();

    /** Poissons capturés dont la prise a au moins une photo. */
    int catchesWithPictureCount();

    /** Sorties enregistrées au plus tard 24 h après leur fin. */
    int tripsEnteredWithinADayCount();

    /** Milieux des sorties, du plus fréquenté au moins fréquenté. */
    List<FrequentedWaterEntity> waterEntities();

    @ImmutableObject
    @JsonSerialize(as = ImmutableDailyActivity.class)
    interface DailyActivity {

        LocalDate day();

        int tripsCount();

        int catchesCount();
    }

    @ImmutableObject
    @JsonSerialize(as = ImmutableFrequentedWaterEntity.class)
    interface FrequentedWaterEntity {

        UUID id();

        String name();

        /** Sorties du pêcheur sur ce milieu pendant la période. */
        int tripsCount();

        /** Peu de sorties connues sur ce milieu, tous pêcheurs et toutes dates confondus. */
        boolean sparselyDocumented();
    }

    /** Moment de la journée d'après l'heure de début de la sortie. */
    enum DayMoment {
        DAWN, MORNING, AFTERNOON, EVENING;

        /** Aube de 4 h à 8 h, matin jusqu'à midi, après-midi jusqu'à 18 h, soir ensuite (nuit comprise). */
        public static DayMoment ofHour(int hour) {
            DayMoment result;
            if (hour >= 4 && hour < 8) {
                result = DAWN;
            } else if (hour >= 8 && hour < 12) {
                result = MORNING;
            } else if (hour >= 12 && hour < 18) {
                result = AFTERNOON;
            } else {
                result = EVENING;
            }
            return result;
        }
    }
}
