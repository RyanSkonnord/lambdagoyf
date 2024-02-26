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

package io.github.ryanskonnord.lambdagoyf.deck;

import com.google.common.base.Preconditions;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ArenaDeckEntry {

    public static final class ArenaVersion {
        private final String expansionCode;
        private final int collectorNumber;

        private ArenaVersion(String expansionCode, int collectorNumber) {
            Preconditions.checkArgument(collectorNumber > 0);
            this.expansionCode = Objects.requireNonNull(expansionCode);
            this.collectorNumber = collectorNumber;
        }

        public String getExpansionCode() {
            return expansionCode;
        }

        public int getCollectorNumber() {
            return collectorNumber;
        }

        @Override
        public String toString() {
            return String.format("(%s) %d", expansionCode, collectorNumber);
        }

        @Override
        public boolean equals(Object o) {
            return this == o || o != null && getClass() == o.getClass()
                    && collectorNumber == ((ArenaVersion) o).collectorNumber
                    && expansionCode.equals(((ArenaVersion) o).expansionCode);
        }

        @Override
        public int hashCode() {
            return 31 * expansionCode.hashCode() + collectorNumber;
        }
    }

    private final String cardName;
    private final Optional<ArenaVersion> version;

    public ArenaDeckEntry(String cardName) {
        this.cardName = Objects.requireNonNull(cardName);
        this.version = Optional.empty();
    }

    public ArenaDeckEntry(String cardName, String expansionCode, int collectorNumber) {
        Preconditions.checkArgument(collectorNumber >= 0);
        this.cardName = Objects.requireNonNull(cardName);
        this.version = Optional.of(new ArenaVersion(expansionCode, collectorNumber));
    }

    private static final Pattern ENTRY_PATTERN = Pattern.compile("(?<name>.*?)\\s+\\((?<expansionCode>\\w+?)\\)\\s+(?<number>\\d+)\\s*");
    private static final Pattern LINE_PATTERN = Pattern.compile("(?<count>\\d+)\\s+" + ENTRY_PATTERN);

    public static Optional<ArenaDeckEntry> parse(String entry) {
        Matcher matcher = ENTRY_PATTERN.matcher(entry);
        if (!matcher.matches()) return Optional.empty();
        return Optional.of(new ArenaDeckEntry(
                matcher.group("name"),
                matcher.group("expansionCode"),
                Integer.parseInt(matcher.group("number"))));
    }

    public String getCardName() {
        return cardName;
    }

    public Optional<ArenaVersion> getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return version.map(v -> cardName + " " + v).orElse(cardName);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o != null && getClass() == o.getClass()
                && cardName.equals(((ArenaDeckEntry) o).cardName)
                && version.equals(((ArenaDeckEntry) o).version);
    }

    @Override
    public int hashCode() {
        return 31 * version.hashCode() + cardName.hashCode();
    }
}