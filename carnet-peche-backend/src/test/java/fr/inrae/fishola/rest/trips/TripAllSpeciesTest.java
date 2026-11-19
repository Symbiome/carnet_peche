package fr.inrae.fishola.rest.trips;

/*-
 * #%L
 * Carnet de pêche :: Backend
 * %%
 * Copyright (C) 2019 - 2021 INRAE - UMR CARRTEL
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

import com.google.common.collect.ImmutableSet;
import fr.inrae.fishola.database.ReferentialDao;
import fr.inrae.fishola.entities.enums.TripMode;
import fr.inrae.fishola.entities.enums.TripType;
import fr.inrae.fishola.entities.tables.pojos.Species;
import fr.inrae.fishola.rest.AbstractCarnetResource;
import fr.inrae.fishola.rest.AbstractCarnetTest;
import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.MediaType;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

/**
 * « Toutes espèces » (#305) : enregistré explicitement sur la sortie, il exclut les
 * espèces recherchées et s'exporte en toutes_espèces dans espece_recherchee.
 */
@QuarkusTest
class TripAllSpeciesTest extends AbstractCarnetTest {

    @Inject
    protected ReferentialDao referentialDao;
    @Inject
    protected AgroalDataSource dataSource;

    protected List<Species> species;
    protected UUID techniqueId;
    protected UUID waterEntityId;
    protected String token;

    @BeforeEach
    @Transactional
    void loadReferentials() {
        this.species = referentialDao.listBuiltInSpecies().stream().limit(2).toList();
        this.techniqueId = referentialDao.listBuiltInTechniques().iterator().next().getId();
        this.waterEntityId = referentialDao.listWaterEntities().iterator().next().getId();
    }

    /** Sortie bredouille de la veille, sur le premier plan d'eau du référentiel. */
    protected TripBean trip(boolean allSpecies) {
        TripBean trip = new TripBean();
        trip.id = "all-species-trip";
        trip.date = LocalDate.now().minusDays(1);
        trip.startedAt = "08:00";
        trip.finishedAt = "10:00";
        trip.waterEntityId = waterEntityId;
        trip.name = "Toutes espèces";
        trip.type = TripType.Border;
        trip.mode = TripMode.Afterwards;
        trip.speciesIds = ImmutableSet.of(species.get(0).getId());
        trip.allSpecies = allSpecies;
        trip.techniqueIds = ImmutableSet.of(techniqueId);
        trip.catchs = List.of();
        return trip;
    }

    protected UUID create(TripBean trip) {
        this.token = login("thimel@codelutin.com", "sispea");
        String tripId = given()
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(AbstractCarnetResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .body(trip)
                .when().post("/api/v1/trips")
                .then().statusCode(201)
                .extract().path(trip.id);
        return UUID.fromString(tripId);
    }

    protected String exportedExpectedSpecies(UUID tripId) {
        return DSL.using(dataSource, SQLDialect.POSTGRES).fetchValue(
                "SELECT espece_recherchee FROM catchs_pending_validation WHERE id_sortie = ?", tripId).toString();
    }

    @Test
    void allSpeciesIsSavedWithoutExpectedSpeciesAndExported() {
        UUID tripId = create(trip(true));

        given()
                .cookie(AbstractCarnetResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .when().get("/api/v1/trips/" + tripId)
                .then().statusCode(200)
                .body("allSpecies", equalTo(true))
                .body("speciesIds", empty());
        Assertions.assertEquals("toutes_espèces", exportedExpectedSpecies(tripId));
    }

    @Test
    void choosingASpeciesAgainReplacesAllSpecies() {
        UUID tripId = create(trip(true));

        TripBean update = trip(false);
        update.id = tripId.toString();
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(AbstractCarnetResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .body(update)
                .when().put("/api/v1/trips/" + tripId)
                .then().statusCode(200);

        given()
                .cookie(AbstractCarnetResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .when().get("/api/v1/trips/" + tripId)
                .then().statusCode(200)
                .body("allSpecies", equalTo(false))
                .body("speciesIds", hasSize(1));
        Assertions.assertEquals(
                DSL.using(dataSource, SQLDialect.POSTGRES).fetchValue(
                        "SELECT normalize_for_export(export_as) FROM species WHERE id = ?", species.get(0).getId()),
                exportedExpectedSpecies(tripId));
    }
}
