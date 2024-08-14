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
import io.github.ryanskonnord.lambdagoyf.card.ArenaCard;
import io.github.ryanskonnord.lambdagoyf.card.DeckElement;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ArenaDeckEntry implements DeckElement<ArenaCard> {
    public static class ArenaDeckEntryDataException extends RuntimeException {
        private ArenaDeckEntryDataException(String message) {
            super(message);
        }
    }



    private final String cardName;
    private final Optional<ArenaVersionId> versionId;
    private final Optional<ArenaCard> version;

    private ArenaDeckEntry(String cardName) {
        this.cardName = Objects.requireNonNull(cardName);
        this.versionId = Optional.empty();
        this.version = Optional.empty();
    }

    public ArenaDeckEntry(String cardName, String expansionCode, int collectorNumber) {
        Preconditions.checkArgument(collectorNumber >= 0);
        this.cardName = Objects.requireNonNull(cardName);
        this.versionId = Optional.of(new ArenaVersionId(expansionCode, collectorNumber));
        this.version = Optional.empty();
    }

    public ArenaDeckEntry(ArenaCard version) {
        this.cardName = version.getCardNameInArenaFormat();
        this.versionId = Optional.of(version.getVersionId());
        this.version = Optional.of(version);
    }

    private static final Pattern ENTRY_PATTERN = Pattern.compile("(?<name>.*?)(\\s+\\((?<expansionCode>\\w+?)\\)\\s+(?<number>\\d+))?\\s*");
    private static final Pattern LINE_PATTERN = Pattern.compile("(?<count>\\d+)\\s+" + ENTRY_PATTERN);

    public static ArenaDeckEntry fromSimpleCardName(String cardName) {
        return new ArenaDeckEntry(cardName);
    }

    public static ArenaDeckEntry parse(String entry) {
        Matcher matcher = ENTRY_PATTERN.matcher(entry);
        if (!matcher.matches()) throw new ArenaDeckEntryDataException("Invalid Arena deck syntax: " + entry);

        String name = matcher.group("name");
        String expansionCode = matcher.group("expansionCode");
        String number = matcher.group("number");
        return expansionCode == null && number == null
                ? new ArenaDeckEntry(name)
                : new ArenaDeckEntry(name, expansionCode, Integer.parseInt(number));
    }

    @Override
    public String getCardName() {
        return cardName;
    }

    public Optional<ArenaVersionId> getVersionId() {
        return versionId;
    }

    @Override
    public Optional<ArenaCard> getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return versionId.map(v -> cardName + " " + v).orElse(cardName);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o != null && getClass() == o.getClass()
                && cardName.equals(((ArenaDeckEntry) o).cardName)
                && versionId.equals(((ArenaDeckEntry) o).versionId);
    }

    @Override
    public int hashCode() {
        return 31 * versionId.hashCode() + cardName.hashCode();
    }
}
