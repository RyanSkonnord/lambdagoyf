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

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.github.ryanskonnord.lambdagoyf.card.field.CardSupertype;
import io.github.ryanskonnord.lambdagoyf.card.field.CardType;
import io.github.ryanskonnord.util.OrderingUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class TypeLine implements Comparable<TypeLine> {

    static final TypeLine EMPTY = new TypeLine(WordSet.empty(), WordSet.empty(), ImmutableList.of());

    private final WordSet<CardSupertype> supertypes;
    private final WordSet<CardType> cardTypes;
    private final ImmutableList<String> subtypes;

    TypeLine(WordSet<CardSupertype> supertypes,
             WordSet<CardType> cardTypes,
             Collection<String> subtypes) {
        this.supertypes = Objects.requireNonNull(supertypes);
        this.cardTypes = Objects.requireNonNull(cardTypes);
        this.subtypes = ImmutableList.copyOf(subtypes);
    }

    private static final char DASH = '\u2014';
    private static final Splitter DASH_SPLITTER = Splitter.on(DASH).trimResults();
    private static final Splitter SPACE_SPLITTER = Splitter.on(CharMatcher.whitespace());
    private static final Collector<CharSequence, ?, String> SPACE_JOINER = Collectors.joining(" ");
    private static final ImmutableSet<String> SUPERTYPE_KEYS = EnumSet.allOf(CardSupertype.class).stream()
            .map(WordType::getKey).collect(ImmutableSet.toImmutableSet());

    private static final String SUMMON = "Summon";
    private static final ImmutableMap<String, CardType> JOKES = ImmutableMap.<String, CardType>builder()
            .put(SUMMON, CardType.CREATURE)
            .put("Eaturecray", CardType.CREATURE)
            .put("instant", CardType.INSTANT)
            .put("Character", CardType.CREATURE)
            .put("Universewalker", CardType.PLANESWALKER)
            .build();
    private static final ImmutableSet<String> JOKE_SUPERTYPES = ImmutableSet.of(
            "Elemental", "Autobot"
    );
    private static final String BFM_JOKE = "Scariest Creature You’ll Ever See";

    private static final class Builder {
        private final List<Word<CardSupertype>> supertypes = new ArrayList<>(3);
        private final List<Word<CardType>> cardTypes = new ArrayList<>(3);
    }


    static TypeLine parse(String line) {
        if (line.equals(BFM_JOKE)) {
            return new TypeLine(WordSet.ofEnums(), WordSet.ofEnums(CardType.CREATURE), SPACE_SPLITTER.splitToList(line));
        }

        List<String> parts = DASH_SPLITTER.splitToList(line);
        Preconditions.checkArgument(!parts.isEmpty(), "Empty type line");
        Preconditions.checkArgument(parts.size() <= 2, "Too many dashes");
        if (parts.get(0).startsWith(SUMMON) && parts.size() == 1) {
            parts = ImmutableList.of(SUMMON, line.substring(SUMMON.length()).trim());
        }

        Builder builder = new Builder();
        for (String word : SPACE_SPLITTER.splitToList(parts.get(0))) {
            parseBeforeDash(word).accept(builder);
        }
        WordSet<CardSupertype> supertypes = WordSet.copyWords(builder.supertypes);
        WordSet<CardType> cardTypes = WordSet.copyWords(builder.cardTypes);

        Collection<String> subtypes = parts.size() == 1 ? ImmutableList.of()
                : cardTypes.contains(CardType.PLANE) ? ImmutableList.of(parts.get(1))
                : SPACE_SPLITTER.splitToList(parts.get(1));
        return new TypeLine(supertypes, cardTypes, subtypes);
    }

    private static Consumer<Builder> parseBeforeDash(String word) {
        // Must do this first to make sure that silly words don't pollute the cache of Word values.
        if (JOKE_SUPERTYPES.contains(word)) return nullConsumer();
        CardType jokeType = JOKES.get(word);
        if (jokeType != null) {
            return builder -> builder.cardTypes.add(Word.of(jokeType));
        }

        if (SUPERTYPE_KEYS.contains(word)) {
            return builder -> builder.supertypes.add(Word.of(CardSupertype.class, word));
        }

        // Else, it's either a proper card type or an unrecognized variant type. If it's an unrecognized variant type,
        // make a guess that it's likelier to be a card type than a supertype.
        return builder -> builder.cardTypes.add(Word.of(CardType.class, word));
    }

    private static <T> Consumer<T> nullConsumer() {
        return value -> {
        };
    }

    private static <E extends Enum<E> & WordType> WordSet<E> createUnion(Stream<WordSet<E>> sets) {
        return WordSet.copyWords(
                sets.flatMap((WordSet<E> s) -> s.asList().stream()).collect(Collectors.toList()));
    }

    public static TypeLine compose(Collection<TypeLine> typeLines) {
        return new TypeLine(
                createUnion(typeLines.stream().map(TypeLine::getSupertypes)),
                createUnion(typeLines.stream().map(TypeLine::getCardTypes)),
                typeLines.stream().flatMap((TypeLine t) -> t.getSubtypes().stream())
                        .distinct().collect(Collectors.toList()));
    }


    public boolean is(CardSupertype type) {
        return supertypes.contains(type);
    }

    public boolean is(CardType type) {
        return cardTypes.contains(type);
    }

    public boolean isSubtype(String type) {
        return subtypes.contains(type);
    }

    public boolean is(String type) {
        return isSubtype(type) ||
                Stream.of(supertypes, cardTypes).flatMap(s -> s.asList().stream()).anyMatch(w -> w.is(type));
    }

    public WordSet<CardSupertype> getSupertypes() {
        return supertypes;
    }

    public WordSet<CardType> getCardTypes() {
        return cardTypes;
    }

    public Collection<String> getSubtypes() {
        return subtypes;
    }

    public boolean isPermanentCard() {
        return cardTypes.asList().stream().anyMatch(t -> t.getEnum().filter(CardType::isPermanentType).isPresent());
    }

    @Override
    public int compareTo(TypeLine that) {
        int cmp = OrderingUtil.compareLexicographically(this.supertypes, that.supertypes);
        if (cmp != 0) return cmp;
        cmp = OrderingUtil.compareLexicographically(this.cardTypes, that.cardTypes);
        if (cmp != 0) return cmp;
        return OrderingUtil.compareLexicographically(this.subtypes, that.subtypes);
    }

    @Override
    public String toString() {
        String beforeDash = Stream.of(supertypes, cardTypes).flatMap(words -> words.asList().stream())
                .map(Word::getKey).collect(SPACE_JOINER);
        return subtypes.isEmpty() ? beforeDash
                : String.format("%s %s %s", beforeDash, DASH, subtypes.stream().collect(SPACE_JOINER));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TypeLine)) return false;
        TypeLine typeLine = (TypeLine) o;
        return supertypes.equals(typeLine.supertypes)
                && cardTypes.equals(typeLine.cardTypes)
                && subtypes.equals(typeLine.subtypes);
    }

    @Override
    public int hashCode() {
        return 127 * supertypes.hashCode() + 31 * cardTypes.hashCode() + subtypes.hashCode();
    }
}
