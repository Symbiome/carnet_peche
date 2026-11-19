package fr.inrae.fishola.rest.trips;

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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

/** Captures aux bornes d'une sortie dont le début et la fin ont des secondes (#310). */
class TripTimestampsTest {

    private static final LocalDate DAY = LocalDate.of(2026, 9, 12);
    private static final LocalDate NEXT_DAY = DAY.plusDays(1);
    private static final LocalDateTime LIVE_BEGIN = LocalDateTime.of(DAY, LocalTime.of(9, 18, 34));
    private static final LocalDateTime LIVE_END = LocalDateTime.of(DAY, LocalTime.of(17, 30, 12));

    private static Optional<LocalDateTime> catchAt(LocalDate day, int hour, int minute,
                                                   LocalDateTime begin, LocalDateTime end) {
        return TripTimestamps.catchTimestamp(day, LocalTime.of(hour, minute), begin, end);
    }

    @Test
    void catchAtTheBeginOrEndMinuteIsWithinTheTrip() {
        Assertions.assertEquals(Optional.of(LocalDateTime.of(DAY, LocalTime.of(9, 18))),
                catchAt(DAY, 9, 18, LIVE_BEGIN, LIVE_END));
        Assertions.assertEquals(Optional.of(LocalDateTime.of(DAY, LocalTime.of(17, 30))),
                catchAt(DAY, 17, 30, LIVE_BEGIN, LIVE_END));
        Assertions.assertTrue(catchAt(null, 9, 18, LIVE_BEGIN, LIVE_END).isPresent());
        Assertions.assertTrue(catchAt(null, 17, 30, LIVE_BEGIN, LIVE_END).isPresent());
    }

    @Test
    void catchOneMinuteBeforeTheBeginOrAfterTheEndIsRefused() {
        Assertions.assertTrue(catchAt(DAY, 9, 17, LIVE_BEGIN, LIVE_END).isEmpty());
        Assertions.assertTrue(catchAt(DAY, 17, 31, LIVE_BEGIN, LIVE_END).isEmpty());
        Assertions.assertTrue(catchAt(null, 9, 17, LIVE_BEGIN, LIVE_END).isEmpty());
        Assertions.assertTrue(catchAt(null, 17, 31, LIVE_BEGIN, LIVE_END).isEmpty());
    }

    @Test
    void multiDayTripBoundsAreComparedToTheMinute() {
        LocalDateTime begin = LocalDateTime.of(DAY, LocalTime.of(22, 10, 45));
        LocalDateTime end = LocalDateTime.of(NEXT_DAY, LocalTime.of(6, 5, 30));

        Assertions.assertEquals(Optional.of(LocalDateTime.of(DAY, LocalTime.of(22, 10))),
                catchAt(null, 22, 10, begin, end));
        Assertions.assertEquals(Optional.of(LocalDateTime.of(NEXT_DAY, LocalTime.of(6, 5))),
                catchAt(null, 6, 5, begin, end));
        Assertions.assertTrue(catchAt(DAY, 22, 9, begin, end).isEmpty());
        Assertions.assertTrue(catchAt(NEXT_DAY, 6, 6, begin, end).isEmpty());
    }

    @Test
    void instantWithSecondsInTheEndMinuteIsWithinTheTrip() {
        Assertions.assertTrue(TripTimestamps.isWithin(LIVE_END.withSecond(59), LIVE_BEGIN, LIVE_END));
        Assertions.assertFalse(TripTimestamps.isWithin(LIVE_END.plusMinutes(1).withSecond(0), LIVE_BEGIN, LIVE_END));
    }
}
