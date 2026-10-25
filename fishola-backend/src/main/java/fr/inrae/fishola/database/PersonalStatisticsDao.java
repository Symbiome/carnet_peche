package fr.inrae.fishola.database;

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

import fr.inrae.fishola.rest.dashboard.ImmutableDailyActivity;
import fr.inrae.fishola.rest.dashboard.ImmutableFrequentedWaterEntity;
import fr.inrae.fishola.rest.dashboard.PersonalStatistics.DailyActivity;
import fr.inrae.fishola.rest.dashboard.PersonalStatistics.DayMoment;
import fr.inrae.fishola.rest.dashboard.PersonalStatistics.FrequentedWaterEntity;
import jakarta.inject.Singleton;
import org.jooq.Record;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Agrégats de la page « Mes statistiques » du pêcheur, calculés en SQL comme
 * {@link AnglerStatisticsDao} : sorties non masquées du pêcheur dont le début
 * tombe dans la période.
 */
@Singleton
public class PersonalStatisticsDao extends AbstractCarnetDao {

    private static final String MY_TRIPS = "t.owner_id = ? AND t.hidden = false "
            + "AND t.begin_timestamp >= ? AND t.begin_timestamp < ? ";

    private static final String COMPLETE_CATCH = "(coalesce(c.size, 0) > 0 OR c.lot_min_size_cm IS NOT NULL "
            + "OR c.size_class IS NOT NULL) "
            + "AND EXISTS (SELECT 1 FROM catch_picture p WHERE p.catch_id = c.id)";

    /** Période d'un pêcheur, du premier au dernier jour inclus. */
    public record Period(UUID userId, LocalDate from, LocalDate to) {

        Object[] binds() {
            return new Object[] {userId, from.atStartOfDay(), to.plusDays(1).atStartOfDay()};
        }
    }

    /** Sorties et poissons capturés par jour de début de sortie. */
    public List<DailyActivity> dailyActivity(Period period) {
        String sql = "SELECT t.begin_timestamp::date AS day, count(DISTINCT t.id) AS trips, "
                + "coalesce(sum(c.quantity), 0) AS catches "
                + "FROM trip t LEFT JOIN catch c ON c.trip_id = t.id "
                + "WHERE " + MY_TRIPS
                + "GROUP BY 1 ORDER BY 1";
        return withContext(context -> context.fetch(sql, period.binds())).stream()
                .map(rec -> (DailyActivity) ImmutableDailyActivity.builder()
                        .day(rec.get("day", LocalDate.class))
                        .tripsCount(rec.get("trips", Integer.class))
                        .catchesCount(rec.get("catches", Integer.class))
                        .build())
                .toList();
    }

    /** Sorties par moment de la journée, d'après l'heure de début. */
    public Map<DayMoment, Integer> tripsPerMoment(Period period) {
        String sql = "SELECT EXTRACT(HOUR FROM t.begin_timestamp)::int AS hour, count(*) AS trips "
                + "FROM trip t WHERE " + MY_TRIPS
                + "GROUP BY 1";
        Map<DayMoment, Integer> result = new EnumMap<>(DayMoment.class);
        withContext(context -> context.fetch(sql, period.binds())).forEach(rec ->
                result.merge(DayMoment.ofHour(rec.get("hour", Integer.class)),
                        rec.get("trips", Integer.class), Integer::sum));
        return result;
    }

    /** Poissons capturés par espèce (lots comptés à leur quantité). */
    public Map<UUID, Integer> catchesPerSpecies(Period period) {
        String sql = "SELECT c.species_id, sum(c.quantity) AS catches "
                + "FROM catch c JOIN trip t ON t.id = c.trip_id "
                + "WHERE " + MY_TRIPS
                + "GROUP BY c.species_id";
        return withContext(context -> context.fetch(sql, period.binds())).stream()
                .collect(Collectors.toMap(
                        rec -> rec.get("species_id", UUID.class),
                        rec -> rec.get("catches", Integer.class)));
    }

    /** Nombre de sorties à fiche complète et de sorties enregistrées dans les 24 h suivant leur fin. */
    public record TripsQuality(int completeTripsCount, int tripsEnteredWithinADayCount) {
    }

    /**
     * Qualité des fiches : une sortie est complète si sa position GPS est
     * connue et si chacune de ses prises a une taille et au moins une photo.
     */
    public TripsQuality tripsQuality(Period period) {
        String sql = "SELECT "
                + "count(*) FILTER (WHERE (t.begin_position IS NOT NULL OR t.snapped_position IS NOT NULL) "
                + "  AND NOT EXISTS (SELECT 1 FROM catch c WHERE c.trip_id = t.id AND NOT (" + COMPLETE_CATCH + "))"
                + ") AS complete, "
                + "count(*) FILTER (WHERE t.created_on <= t.end_timestamp + interval '24 hours') AS within_a_day "
                + "FROM trip t WHERE " + MY_TRIPS;
        Record rec = withContext(context -> context.fetchOne(sql, period.binds()));
        return new TripsQuality(rec.get("complete", Integer.class), rec.get("within_a_day", Integer.class));
    }

    /** Poissons capturés dont la prise a au moins une photo. */
    public int catchesWithPictureCount(Period period) {
        String sql = "SELECT coalesce(sum(c.quantity), 0) AS catches "
                + "FROM catch c JOIN trip t ON t.id = c.trip_id "
                + "WHERE " + MY_TRIPS
                + "AND EXISTS (SELECT 1 FROM catch_picture p WHERE p.catch_id = c.id)";
        return withContext(context -> context.fetchOne(sql, period.binds())).get("catches", Integer.class);
    }

    /**
     * Milieux des sorties de la période, du plus fréquenté au moins fréquenté.
     * Un milieu est peu documenté tant qu'il compte au plus
     * {@code sparselyDocumentedMaxTrips} sorties, tous pêcheurs confondus.
     */
    public List<FrequentedWaterEntity> waterEntities(Period period, int sparselyDocumentedMaxTrips) {
        String sql = "WITH mine AS ( "
                + "  SELECT t.water_entity_id, count(*) AS trips FROM trip t WHERE " + MY_TRIPS
                + "  GROUP BY 1 "
                + ") "
                + "SELECT w.id, w.name, mine.trips, "
                + "  (SELECT count(*) FROM trip o WHERE o.water_entity_id = w.id AND o.hidden = false) AS known_trips "
                + "FROM mine JOIN water_entity w ON w.id = mine.water_entity_id "
                + "ORDER BY mine.trips DESC, w.name";
        return withContext(context -> context.fetch(sql, period.binds())).stream()
                .map(rec -> (FrequentedWaterEntity) ImmutableFrequentedWaterEntity.builder()
                        .id(rec.get("id", UUID.class))
                        .name(rec.get("name", String.class))
                        .tripsCount(rec.get("trips", Integer.class))
                        .sparselyDocumented(rec.get("known_trips", Integer.class) <= sparselyDocumentedMaxTrips)
                        .build())
                .toList();
    }
}
