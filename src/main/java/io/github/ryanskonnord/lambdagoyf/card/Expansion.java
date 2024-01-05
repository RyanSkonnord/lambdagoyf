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

import com.google.common.base.Splitter;
import io.github.ryanskonnord.lambdagoyf.card.field.ExpansionType;
import io.github.ryanskonnord.lambdagoyf.scryfall.ScryfallSet;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class Expansion extends ScryfallEntity implements Comparable<Expansion> {

    private final String name;
    private final UUID scryfallId;

    private final String productCode;
    private final Optional<String> mtgoCode;
    private final LocalDate releaseDate;
    private final Word<ExpansionType> type;
    private final int cardCount;

    public Expansion(ScryfallSet data) {
        name = data.getName();
        scryfallId = data.getId();

        productCode = data.getCode().toUpperCase();
        mtgoCode = data.getMtgoCode().map(String::toUpperCase);
        releaseDate = data.getReleasedAt();
        type = Word.of(ExpansionType.class, data.getSetType());
        cardCount = data.getCardCount();
    }

    private static Optional<String> abbreviateName(String name, Word<ExpansionType> type) {
        if (!type.is(ExpansionType.EXPANSION)) return Optional.empty();
        int colonIndex = name.indexOf(':');
        if (colonIndex < 0) return Optional.empty();
        String abbreviatedName = name.substring(0, colonIndex);
        return Optional.of(abbreviatedName);
    }

    public String getName() {
        return name;
    }

    @Override
    public UUID getScryfallId() {
        return scryfallId;
    }

    public String getProductCode() {
        return productCode;
    }

    public Optional<String> getMtgoCode() {
        return mtgoCode;
    }

    private static final Splitter COLON_DELIMITER = Splitter.on(Pattern.compile("\\s*:\\s*"));

    public List<String> getColonNameTokens() {
        return COLON_DELIMITER.splitToList(name);
    }

    public Stream<String> getAllNames() {
        return Stream.of(
                Stream.of(this.name, productCode),
                mtgoCode.stream(),
                getColonNameTokens().stream()
        ).flatMap(Function.identity()).distinct();
    }

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public Word<ExpansionType> getType() {
        return type;
    }

    public int getCardCount() {
        return cardCount;
    }

    public boolean isNamed(String n) {
        return getAllNames().anyMatch(n::equalsIgnoreCase);
    }

    @Override
    public int compareTo(Expansion that) {
        if (this == that || this.scryfallId.equals(that.scryfallId)) return 0;
        int cmp = this.releaseDate.compareTo(that.releaseDate);
        if (cmp != 0) return cmp;
        cmp = this.type.compareTo(that.type);
        if (cmp != 0) return cmp;
        return this.name.compareTo(that.name);
    }

    @Override
    public String toString() {
        return getName();
    }

}
