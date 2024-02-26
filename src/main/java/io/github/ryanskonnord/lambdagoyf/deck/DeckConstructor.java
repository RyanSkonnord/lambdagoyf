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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import io.github.ryanskonnord.lambdagoyf.card.ArenaCard;
import io.github.ryanskonnord.lambdagoyf.card.Card;
import io.github.ryanskonnord.lambdagoyf.card.CardVersion;
import io.github.ryanskonnord.lambdagoyf.card.CardVersionExtractor;
import io.github.ryanskonnord.lambdagoyf.card.DeckElement;
import io.github.ryanskonnord.lambdagoyf.card.FinishedCardVersion;
import io.github.ryanskonnord.lambdagoyf.card.MtgoCard;
import io.github.ryanskonnord.lambdagoyf.card.field.ExpansionType;
import io.github.ryanskonnord.util.ComparatorMutator;
import io.github.ryanskonnord.util.OrderingUtil;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * @param <V>
 * @param <T>
 */
public final class DeckConstructor<V extends CardVersion, T extends DeckElement<V>> {

    private static <V> ToIntFunction<V> getUnlimitedAvailability() {
        return (V version) -> Integer.MAX_VALUE;
    }

    private static <T> Comparator<T> getInactiveComparator() {
        return (T o1, T o2) -> 0;
    }

    private final CardVersionExtractor<V> versionExtractor;
    private final Function<? super V, T> outputConstructor;
    private final ToIntFunction<? super T> availability;
    private final Comparator<V> preference;
    private final Comparator<V> overflow;
    private final UnaryOperator<Deck<Card>> deckTransformation;
    private final UnaryOperator<V> cardVersionTransformation;
    private final UnaryOperator<Deck<T>> outputTransformation;
    private final Function<Card, Stream<T>> fallback;

    private DeckConstructor(Builder<V, T> builder) {
        this.versionExtractor = Objects.requireNonNull(builder.versionExtractor);
        this.outputConstructor = Objects.requireNonNull(builder.outputConstructor);
        this.availability = Objects.requireNonNull(builder.availability);
        this.preference = builder.preference.get().orElse(getInactiveComparator());
        this.overflow = builder.overflow.get().orElse(this.preference);
        this.deckTransformation = Objects.requireNonNull(builder.deckTransformation);
        this.cardVersionTransformation = Objects.requireNonNull(builder.cardVersionTransformation);
        this.outputTransformation = Objects.requireNonNull(builder.outputTransformation);
        this.fallback = Objects.requireNonNull(builder.fallback);
    }

    public static final class Builder<V extends CardVersion, T extends DeckElement<V>> {
        private final CardVersionExtractor<V> versionExtractor;
        private final Function<? super V, T> outputConstructor;
        private ToIntFunction<? super T> availability = getUnlimitedAvailability();
        private ComparatorMutator<V, Builder<V, T>> preference = new ComparatorMutator<>(this);
        private ComparatorMutator<V, Builder<V, T>> overflow = new ComparatorMutator<>(this);
        private UnaryOperator<Deck<Card>> deckTransformation = UnaryOperator.identity();
        private UnaryOperator<V> cardVersionTransformation = UnaryOperator.identity();
        private UnaryOperator<Deck<T>> outputTransformation = UnaryOperator.identity();
        private Function<Card, Stream<T>> fallback = card -> Stream.empty();

        private Builder(CardVersionExtractor<V> versionExtractor, Function<? super V, T> outputConstructor) {
            this.versionExtractor = Objects.requireNonNull(versionExtractor);
            this.outputConstructor = Objects.requireNonNull(outputConstructor);
        }

        public Builder<V, T> setAvailability(ToIntFunction<? super T> availability) {
            this.availability = Objects.requireNonNull(availability);
            return this;
        }

        public Builder<V, T> setAvailableCollection(Multiset<? extends T> collection) {
            return setAvailability(collection::count);
        }

        public ComparatorMutator<V, Builder<V, T>> withPreferenceOrder() {
            return preference;
        }

        public ComparatorMutator<V, Builder<V, T>> withOverflowOver() {
            return overflow;
        }

        public Builder<V, T> addDeckTransformation(UnaryOperator<Deck<Card>> next) {
            this.deckTransformation = append(this.deckTransformation, next);
            return this;
        }

        public Builder<V, T> addCardVersionTransformation(UnaryOperator<V> next) {
            this.cardVersionTransformation = append(this.cardVersionTransformation, next);
            return this;
        }

        public Builder<V, T> addOutputTransformation(UnaryOperator<Deck<T>> next) {
            this.outputTransformation = append(this.outputTransformation, next);
            return this;
        }

        public Builder<V, T> withFallback(Function<Card, Stream<T>> fallback) {
            this.fallback = Objects.requireNonNull(fallback);
            return this;
        }

        public DeckConstructor<V, T> build() {
            return new DeckConstructor<>(this);
        }
    }

    public static final class CardVersionNotFoundException extends RuntimeException {
        private CardVersionNotFoundException(Card card) {
            super("No versions found for: " + card);
        }
    }

    private static <T> UnaryOperator<T> append(UnaryOperator<T> first, UnaryOperator<T> second) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(second);
        return t -> second.apply(first.apply(t));
    }

    private static <V extends FinishedCardVersion> Comparator<V> getDefaultOrder() {
        return Comparator.comparing((V version) -> version.getEdition().getReleaseDate()).reversed()
                .thenComparing(CardVersion::getEdition)
                .thenComparing(FinishedCardVersion::getFinish);
    }


    public static Builder<MtgoCard, MtgoDeck.CardEntry> createForMtgo() {
        return new Builder<>(CardVersionExtractor.getMtgoCards(), MtgoDeck.CardEntry::new)
                .withPreferenceOrder().set(getDefaultOrder());
    }

    public static Builder<ArenaCard, ArenaDeckEntry> createForArena() {
        Comparator<ArenaCard> defaultOrder = Comparator
                .comparing((ArenaCard arenaCard) -> arenaCard.getEdition().getExpansion()
                        .getType().getEnum().filter(ExpansionType::isStandardRelease).isEmpty())
                .thenComparing(Comparator.comparing(ArenaCard::getEdition).reversed());
        return new Builder<>(CardVersionExtractor.getArenaCard(), ArenaCard::getDeckEntry)
                .withPreferenceOrder().set(defaultOrder)
                .withFallback(card -> Stream.of(new ArenaDeckEntry(card.getMainName())));
    }


    private T transformCardVersion(T element) {
        return element.getVersion().map(cardVersionTransformation).map(outputConstructor).orElse(element);
    }

    public Deck<T> createDeck(Deck<Card> unversionedDeck) {
        Deck<Card> transformedDeck = deckTransformation.apply(unversionedDeck);
        Deck<T> versionedDeck = transformedDeck.getEntries()
                .map(this::chooseVersion)
                .collect(Deck.toDeck())
                .transform(this::transformCardVersion);
        return outputTransformation.apply(versionedDeck);
    }

    private Comparator<T> comparingVersions(Comparator<V> versionComparator) {
        return OrderingUtil.OptionalComparator.<T, V>build(DeckElement::getVersion, versionComparator).emptyKeysLast();
    }

    private static final ImmutableSet<Deck.Section> DECK_SECTIONS = Sets.immutableEnumSet(EnumSet.allOf(Deck.Section.class));

    private Deck<T> chooseVersion(Deck.Entry<Card> entry) {
        Card card = entry.getCard();
        ImmutableSet<T> allVersions = Stream.concat(
                versionExtractor.fromCard(card).map(outputConstructor),
                fallback.apply(card)
        ).collect(ImmutableSet.toImmutableSet());

        Optional<T> favoriteAvailableVersion = allVersions.stream()
                .filter((T version) -> availability.applyAsInt(version) >= entry.getTotal())
                .min(comparingVersions(preference));
        if (favoriteAvailableVersion.isPresent()) {
            Deck.Builder<T> builder = new Deck.Builder<>();
            T output = favoriteAvailableVersion.get();
            for (Deck.Section section : DECK_SECTIONS) {
                builder.addTo(section, output, entry.getNumberIn(section));
            }
            return builder.build();
        }

        // Else, there are not enough copies of any one version to match.
        Deck.Builder<T> accumulation = new Deck.Builder<>();
        List<T> orderedVersions = allVersions.stream()
                .filter((T version) -> availability.applyAsInt(version) > 0)
                .sorted(comparingVersions(preference))
                .collect(ImmutableList.toImmutableList());

        // First, fill the sections with matching groups of cards. Iterate over the sections in descending order of
        // importance, and use whichever version has enough cards to match, if any.
        for (Deck.Section section : DECK_SECTIONS) {
            int numberWanted = entry.getNumberIn(section);
            if (numberWanted == 0) continue;
            for (T version : orderedVersions) {
                int numberAvailable = availability.applyAsInt(version) - accumulation.getTotalCopiesOf(version);
                if (numberAvailable >= numberWanted) {
                    accumulation.addTo(section, version, numberWanted);
                    break;
                }
            }
        }
        if (accumulation.getSize() == entry.getTotal()) return accumulation.build();

        // Then, use whatever is available in preferred order, with mismatched groups if necessary.
        for (T version : orderedVersions) {
            int numberAvailable = availability.applyAsInt(version) - accumulation.getTotalCopiesOf(version);
            for (Deck.Section section : DECK_SECTIONS) {
                int numberWanted = entry.getNumberIn(section) - accumulation.get(section).size();
                int numberToAdd = Math.min(numberAvailable, numberWanted);
                accumulation.addTo(section, version, numberToAdd);
                numberAvailable -= numberToAdd;
            }
        }
        if (accumulation.getSize() == entry.getTotal()) return accumulation.build();

        // In case we still didn't find enough available copies, add unavailable copies using overflow logic.
        T overflowVersion = !orderedVersions.isEmpty() ? orderedVersions.get(0) :
                allVersions.stream().min(comparingVersions(overflow))
                        .orElseThrow(() -> new CardVersionNotFoundException(card));
        for (Deck.Section section : DECK_SECTIONS) {
            int numberOfOverflowCopies = entry.getNumberIn(section) - accumulation.get(section).size();
            accumulation.addTo(section, overflowVersion, numberOfOverflowCopies);
        }
        return accumulation.build();
    }

}
