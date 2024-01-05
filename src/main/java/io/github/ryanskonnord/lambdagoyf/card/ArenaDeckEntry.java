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

package io.github.ryanskonnord.lambdagoyf.card;

import com.google.common.base.Preconditions;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ArenaDeckEntry {

    private final String cardName;
    private final String expansionCode;
    private final int collectorNumber;

    public ArenaDeckEntry(String cardName, String expansionCode, int collectorNumber) {
        Preconditions.checkArgument(collectorNumber >= 0);
        this.cardName = Objects.requireNonNull(cardName);
        this.expansionCode = Objects.requireNonNull(expansionCode);
        this.collectorNumber = collectorNumber;
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

    public String getExpansionCode() {
        return expansionCode;
    }

    public int getCollectorNumber() {
        return collectorNumber;
    }

    private transient String stringValue;

    @Override
    public String toString() {
        return (stringValue != null) ? stringValue : (stringValue =
                String.format("%s (%s) %d", cardName, expansionCode, collectorNumber));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ArenaDeckEntry)) return false;

        ArenaDeckEntry that = (ArenaDeckEntry) o;
        if (stringValue != null && that.stringValue != null) return stringValue.equals(that.stringValue);
        if (collectorNumber != that.collectorNumber) return false;
        if (!cardName.equals(that.cardName)) return false;
        return expansionCode.equals(that.expansionCode);
    }

    @Override
    public int hashCode() {
        int result = cardName.hashCode();
        result = 31 * result + expansionCode.hashCode();
        result = 31 * result + collectorNumber;
        return result;
    }
}
