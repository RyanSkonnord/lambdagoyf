/*
 * Lambdagoyf: A Software Suite for MTG Hobbyists
 * https://github.com/RyanSkonnord/lambdagoyf
 *
 * Copyright 2024 Ryan Skonnord
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.ryanskonnord.lambdagoyf.deck.preference.demo;

import com.google.common.base.Preconditions;
import io.github.ryanskonnord.lambdagoyf.card.Card;
import io.github.ryanskonnord.lambdagoyf.deck.Deck;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.Objects;

public final class WinterCheer {

    private final Clock clock;
    private final SnowConversion snowConversion;

    public WinterCheer(Clock clock, SnowConversion snowConversion) {
        this.clock = Objects.requireNonNull(clock);
        this.snowConversion = Objects.requireNonNull(snowConversion);
    }

    public Deck<Card> convertSeasonally(Deck<Card> deck) {
        Objects.requireNonNull(deck);
        LocalDate date = LocalDate.now(clock);
        return tisTheSeason(date) ? snowConversion.convert(deck) : deck;
    }

    /**
     * Check whether a date is between Thanksgiving (U.S.) and New Year's Eve, inclusive.
     */
    private static boolean tisTheSeason(LocalDate date) {
        if (date.getMonth().compareTo(Month.NOVEMBER) < 0) return false;
        LocalDate thanksgiving = nthDayOfWeek(4, DayOfWeek.THURSDAY, YearMonth.of(date.getYear(), Month.NOVEMBER));
        return thanksgiving.minusDays(3).compareTo(date) <= 0;
    }

    private static LocalDate nthDayOfWeek(int weekOrdinal, DayOfWeek dayOfWeek, YearMonth yearMonth) {
        Preconditions.checkArgument(weekOrdinal > 0);
        int minDayOfMonth = 7 * (weekOrdinal - 1) + 1;
        LocalDate startOfNthWeek = yearMonth.atDay(minDayOfMonth);
        int dayOfWeekOffset = (dayOfWeek.getValue() - startOfNthWeek.getDayOfWeek().getValue() + 7) % 7;
        return yearMonth.atDay(minDayOfMonth + dayOfWeekOffset);
    }

}
