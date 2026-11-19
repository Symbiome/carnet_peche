package fr.inrae.fishola.rest.metrics;

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

import fr.inrae.fishola.database.CatchsDao;
import fr.inrae.fishola.database.PersonalStatisticsDao;
import fr.inrae.fishola.database.PersonalStatisticsDao.Period;
import fr.inrae.fishola.database.PersonalStatisticsDao.TripsQuality;
import fr.inrae.fishola.database.TripsDao;
import fr.inrae.fishola.database.UsersDao;
import fr.inrae.fishola.entities.enums.DeviceType;
import fr.inrae.fishola.entities.enums.Maillage;
import fr.inrae.fishola.entities.enums.TripMode;
import fr.inrae.fishola.entities.enums.TripType;
import fr.inrae.fishola.entities.tables.pojos.Catch;
import fr.inrae.fishola.entities.tables.pojos.Trip;
import fr.inrae.fishola.rest.AbstractCarnetResource;
import fr.inrae.fishola.rest.AbstractCarnetTest;
import fr.inrae.fishola.rest.dashboard.PersonalStatistics.DailyActivity;
import fr.inrae.fishola.rest.dashboard.PersonalStatistics.DayMoment;
import fr.inrae.fishola.rest.dashboard.PersonalStatistics.FrequentedWaterEntity;
import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;

/**
 * Page « Mes statistiques » : activité par jour, moments de sortie, prises
 * par espèce, qualité des fiches et milieux peu documentés, sur la période
 * demandée seulement.
 */
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PersonalStatisticsTest extends AbstractCarnetTest {

    private static final int YEAR = 2019;
    private static final String EMAIL_PREFIX = "personal-stats-";
    private static final String TRIP_NAME = "PERSONAL-STATS";

    @Inject
    AgroalDataSource dataSource;

    @Inject
    UsersDao usersDao;

    @Inject
    TripsDao tripsDao;

    @Inject
    CatchsDao catchsDao;

    @Inject
    PersonalStatisticsDao personalStatisticsDao;

    private UUID anglerId;
    private UUID busyLakeId;
    private UUID quietLakeId;
    private UUID techniqueId;
    private UUID frequentSpecies;
    private UUID rareSpecies;

    @BeforeAll
    @Transactional
    void seed() {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        busyLakeId = insertLake(ctx, "IT Perso Lac", "IT_PERSO1");
        quietLakeId = insertLake(ctx, "IT Perso Etang", "IT_PERSO2");
        techniqueId = ctx.fetchOne("SELECT id FROM technique ORDER BY name LIMIT 1").get("id", UUID.class);
        List<UUID> species = ctx.fetch("SELECT id FROM species ORDER BY name LIMIT 2").getValues("id", UUID.class);
        frequentSpecies = species.get(0);
        rareSpecies = species.get(1);

        anglerId = createAngler("me");
        LocalDateTime dawn = LocalDateTime.of(YEAR, 3, 10, 6, 0);
        UUID dawnTrip = createTrip(anglerId, busyLakeId, dawn, dawn, true);
        createCatch(dawnTrip, frequentSpecies, 3, true);
        createCatch(dawnTrip, rareSpecies, 1, false);
        LocalDateTime afternoon = LocalDateTime.of(YEAR, 3, 10, 14, 0);
        createTrip(anglerId, busyLakeId, afternoon, afternoon.plusDays(3), true);
        LocalDateTime evening = LocalDateTime.of(YEAR, 5, 2, 20, 0);
        UUID eveningTrip = createTrip(anglerId, quietLakeId, evening, evening, false);
        createCatch(eveningTrip, frequentSpecies, 2, true);

        LocalDateTime nextYear = LocalDateTime.of(YEAR + 1, 1, 1, 9, 0);
        createTrip(anglerId, busyLakeId, nextYear, nextYear, true);
        UUID hiddenTrip = createTrip(anglerId, busyLakeId, dawn, dawn, true);
        ctx.execute("UPDATE trip SET hidden = true WHERE id = ?", hiddenTrip);

        UUID other = createAngler("other");
        for (int day = 1; day <= 11; day++) {
            LocalDateTime begin = LocalDateTime.of(YEAR, 4, day, 9, 0);
            createTrip(other, busyLakeId, begin, begin, false);
        }
    }

    private static UUID insertLake(DSLContext ctx, String name, String code) {
        return ctx.fetchOne("INSERT INTO water_entity (name, export_as, water_entity_code, kind) "
                + "VALUES (?, ?, ?, 'STILL') RETURNING id", name, name, code).get("id", UUID.class);
    }

    private UUID createAngler(String suffix) {
        String email = EMAIL_PREFIX + suffix + "@carnet-peche.test";
        usersDao.create("Angler", suffix, EMAIL_PREFIX + suffix, email, usersDao.hashPassword("sispea"),
                false, "74000", 1990);
        return usersDao.findByEmail(email).orElseThrow().getId();
    }

    private UUID createTrip(UUID ownerId, UUID lakeId, LocalDateTime begin, LocalDateTime createdOn,
                            boolean withPosition) {
        Trip trip = new Trip();
        trip.setOwnerId(ownerId);
        trip.setCreatedOn(createdOn);
        trip.setBeginTimestamp(begin);
        trip.setEndTimestamp(begin.plusHours(2));
        trip.setWaterEntityId(lakeId);
        trip.setName(TRIP_NAME);
        trip.setType(TripType.Border);
        trip.setHidden(false);
        trip.setMode(TripMode.Afterwards);
        trip.setSource(DeviceType.web);
        UUID tripId = tripsDao.create(trip);
        if (withPosition) {
            DSL.using(dataSource, SQLDialect.POSTGRES).execute("UPDATE trip "
                    + "SET begin_position = ST_SetSRID(ST_MakePoint(6.1, 45.9), 4326) WHERE id = ?", tripId);
        }
        return tripId;
    }

    private void createCatch(UUID tripId, UUID speciesId, int quantity, boolean withPicture) {
        Catch aCatch = new Catch();
        aCatch.setTripId(tripId);
        aCatch.setCreatedOn(LocalDateTime.of(YEAR, 1, 1, 0, 0));
        aCatch.setSpeciesId(speciesId);
        aCatch.setTechniqueId(techniqueId);
        aCatch.setSize(25);
        aCatch.setKept(false);
        aCatch.setQuantity(quantity);
        aCatch.setMaillee(Maillage.NON_DEFINI);
        UUID catchId = catchsDao.create(aCatch);
        if (withPicture) {
            DSL.using(dataSource, SQLDialect.POSTGRES).execute(
                    "INSERT INTO catch_picture (catch_id, content, picture_index) VALUES (?, ?, 0)",
                    catchId, new byte[] {1, 2, 3});
        }
    }

    @AfterAll
    @Transactional
    void cleanup() {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        String trips = "(SELECT id FROM trip WHERE name = '" + TRIP_NAME + "')";
        ctx.execute("DELETE FROM catch_picture WHERE catch_id IN (SELECT id FROM catch WHERE trip_id IN " + trips + ")");
        ctx.execute("DELETE FROM catch WHERE trip_id IN " + trips);
        ctx.execute("DELETE FROM trip WHERE name = ?", TRIP_NAME);
        ctx.execute("DELETE FROM fishola_user WHERE email LIKE ?", EMAIL_PREFIX + "%");
        ctx.execute("DELETE FROM water_entity WHERE id IN (?, ?)", busyLakeId, quietLakeId);
    }

    private Period season() {
        return new Period(anglerId, LocalDate.of(YEAR, 1, 1), LocalDate.of(YEAR, 12, 31));
    }

    @Test
    @Transactional
    void groupsTripsAndCatchesPerDayOfTheRequestedPeriodOnly() {
        List<DailyActivity> days = personalStatisticsDao.dailyActivity(season());
        Assertions.assertEquals(2, days.size());
        Assertions.assertEquals(LocalDate.of(YEAR, 3, 10), days.get(0).day());
        Assertions.assertEquals(2, days.get(0).tripsCount());
        Assertions.assertEquals(4, days.get(0).catchesCount());
        Assertions.assertEquals(LocalDate.of(YEAR, 5, 2), days.get(1).day());
        Assertions.assertEquals(1, days.get(1).tripsCount());
        Assertions.assertEquals(2, days.get(1).catchesCount());
    }

    @Test
    @Transactional
    void sortsTripsByMomentOfTheDay() {
        Assertions.assertEquals(Map.of(DayMoment.DAWN, 1, DayMoment.AFTERNOON, 1, DayMoment.EVENING, 1),
                personalStatisticsDao.tripsPerMoment(season()));
        Assertions.assertEquals(DayMoment.EVENING, DayMoment.ofHour(2));
        Assertions.assertEquals(DayMoment.DAWN, DayMoment.ofHour(4));
        Assertions.assertEquals(DayMoment.MORNING, DayMoment.ofHour(11));
        Assertions.assertEquals(DayMoment.AFTERNOON, DayMoment.ofHour(12));
        Assertions.assertEquals(DayMoment.EVENING, DayMoment.ofHour(18));
    }

    @Test
    @Transactional
    void countsLotQuantitiesPerSpecies() {
        Assertions.assertEquals(Map.of(frequentSpecies, 5, rareSpecies, 1),
                personalStatisticsDao.catchesPerSpecies(season()));
    }

    @Test
    @Transactional
    void measuresTheQualityOfTheRecords() {
        TripsQuality quality = personalStatisticsDao.tripsQuality(season());
        Assertions.assertEquals(1, quality.completeTripsCount());
        Assertions.assertEquals(2, quality.tripsEnteredWithinADayCount());
        Assertions.assertEquals(5, personalStatisticsDao.catchesWithPictureCount(season()));
    }

    @Test
    @Transactional
    void flagsSparselyDocumentedWaterEntities() {
        List<FrequentedWaterEntity> waterEntities = personalStatisticsDao.waterEntities(season(), 10);
        Assertions.assertEquals(List.of(busyLakeId, quietLakeId),
                waterEntities.stream().map(FrequentedWaterEntity::id).toList());
        Assertions.assertEquals(2, waterEntities.get(0).tripsCount());
        Assertions.assertFalse(waterEntities.get(0).sparselyDocumented());
        Assertions.assertTrue(waterEntities.get(1).sparselyDocumented());
    }

    @Test
    void endpointReturnsTheStatisticsOfTheConnectedAngler() {
        String token = login(EMAIL_PREFIX + "me@carnet-peche.test", "sispea");
        var json = given()
                .cookie(AbstractCarnetResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .queryParam("from", YEAR + "-01-01")
                .queryParam("to", YEAR + "-12-31")
                .when().get("/api/v1/dashboard/statistics")
                .then().statusCode(200)
                .extract().jsonPath();

        Assertions.assertEquals(2, json.getList("dailyActivity").size());
        Assertions.assertEquals(1, json.getInt("tripsPerMoment.DAWN"));
        Assertions.assertEquals(5, json.getInt("catchesPerSpecies." + frequentSpecies));
        Assertions.assertEquals(1, json.getInt("completeTripsCount"));
        Assertions.assertEquals(5, json.getInt("catchesWithPictureCount"));
        Assertions.assertEquals(2, json.getInt("tripsEnteredWithinADayCount"));
        Assertions.assertTrue(json.getBoolean("waterEntities.find { it.name == 'IT Perso Etang' }.sparselyDocumented"));
    }

    @Test
    void endpointRejectsAnInvalidPeriod() {
        String token = login(EMAIL_PREFIX + "me@carnet-peche.test", "sispea");
        given().cookie(AbstractCarnetResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .queryParam("from", YEAR + "-01-01")
                .queryParam("to", (YEAR + 1) + "-06-30")
                .when().get("/api/v1/dashboard/statistics")
                .then().statusCode(400);
        given().cookie(AbstractCarnetResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .queryParam("from", YEAR + "-12-31")
                .queryParam("to", YEAR + "-01-01")
                .when().get("/api/v1/dashboard/statistics")
                .then().statusCode(400);
    }
}
