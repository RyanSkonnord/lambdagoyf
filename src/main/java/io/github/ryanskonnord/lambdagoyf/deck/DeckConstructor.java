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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import io.github.ryanskonnord.lambdagoyf.card.ArenaCard;
import io.github.ryanskonnord.lambdagoyf.card.Card;
import io.github.ryanskonnord.lambdagoyf.card.CardEdition;
import io.github.ryanskonnord.lambdagoyf.card.CardVersion;
import io.github.ryanskonnord.lambdagoyf.card.CardVersionExtractor;
import io.github.ryanskonnord.lambdagoyf.card.FinishedCardVersion;
import io.github.ryanskonnord.lambdagoyf.card.MtgoCard;
import io.github.ryanskonnord.lambdagoyf.card.PaperCard;
import io.github.ryanskonnord.lambdagoyf.card.field.ExpansionType;
import io.github.ryanskonnord.util.ComparatorMutator;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.ToIntFunction;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public final class DeckConstructor<V extends CardVersion> {

    private static <V> ToIntFunction<V> getUnlimitedAvailability() {
        return (V version) -> Integer.MAX_VALUE;
    }

    private static <T> Comparator<T> getInactiveComparator() {
        return (T o1, T o2) -> 0;
    }

    private final CardVersionExtractor<V> versionExtractor;
    private final ToIntFunction<? super V> availability;
    private final UnaryOperator<Deck<Card>> deckTransformation;
    private final Comparator<V> preference;
    private final Comparator<V> overflow;
    private final UnaryOperator<Deck<V>> versionTransformation;

    private DeckConstructor(Builder<V> builder) {
        this.versionExtractor = Objects.requireNonNull(builder.versionExtractor);
        this.availability = Objects.requireNonNull(builder.availability);
        this.deckTransformation = Objects.requireNonNull(builder.deckTransformation);
        this.preference = builder.preference.get().orElse(getInactiveComparator());
        this.overflow = builder.overflow.get().orElse(this.preference);
        this.versionTransformation = Objects.requireNonNull(builder.versionTransformation);
    }

    public static final class Builder<V extends CardVersion> {
        private final CardVersionExtractor<V> versionExtractor;
        private ToIntFunction<? super V> availability = getUnlimitedAvailability();
        private ComparatorMutator<V, Builder<V>> preference = new ComparatorMutator<>(this);
        private ComparatorMutator<V, Builder<V>> overflow = new ComparatorMutator<>(this);
        private UnaryOperator<Deck<Card>> deckTransformation = UnaryOperator.identity();
        private UnaryOperator<Deck<V>> versionTransformation = UnaryOperator.identity();

        private Builder(CardVersionExtractor<V> versionExtractor) {
            this.versionExtractor = versionExtractor;
        }

        public Builder<V> setAvailability(ToIntFunction<? super V> availability) {
            this.availability = Objects.requireNonNull(availability);
            return this;
        }

        public Builder<V> setAvailableCollection(Multiset<? extends V> collection) {
            return setAvailability(collection::count);
        }

        public ComparatorMutator<V, Builder<V>> withPreferenceOrder() {
            return preference;
        }

        public ComparatorMutator<V, Builder<V>> withOverflowOver() {
            return overflow;
        }

        public Builder<V> addDeckTransformation(UnaryOperator<Deck<Card>> next) {
            this.deckTransformation = append(this.deckTransformation, next);
            return this;
        }

        public Builder<V> addVersionTransformation(UnaryOperator<Deck<V>> next) {
            this.versionTransformation = append(this.versionTransformation, next);
            return this;
        }

        public DeckConstructor<V> build() {
            return new DeckConstructor<>(this);
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

    public static Builder<CardEdition> createForCardEditions() {
        Comparator<CardEdition> defaultOrder = Comparator.comparing(CardEdition::getReleaseDate).reversed()
                .thenComparing(Comparator.naturalOrder());
        return new Builder<>(CardVersionExtractor.getCardEditions())
                .withPreferenceOrder().set(defaultOrder);
    }

    public static Builder<MtgoCard> createForMtgo() {
        return new Builder<>(CardVersionExtractor.getMtgoCards())
                .withPreferenceOrder().set(getDefaultOrder());
    }

    public static Builder<ArenaCard> createForArena() {
        Comparator<ArenaCard> defaultOrder = Comparator
                .comparing((ArenaCard arenaCard) -> arenaCard.getEdition().getExpansion()
                        .getType().getEnum().filter(ExpansionType::isStandardRelease).isEmpty())
                .thenComparing(Comparator.comparing(ArenaCard::getEdition).reversed());
        return new Builder<>(CardVersionExtractor.getArenaCard())
                .withPreferenceOrder().set(defaultOrder);
    }

    public static Builder<PaperCard> createForPaper() {
        return new Builder<>(CardVersionExtractor.getPaperCards())
                .withPreferenceOrder().set(getDefaultOrder());
    }


    public Deck<V> createDeck(Deck<Card> unversionedDeck) {
        Deck<Card> transformedDeck = deckTransformation.apply(unversionedDeck);
        Deck<V> versionedDeck = transformedDeck.getEntries()
                .map(this::chooseVersion)
                .collect(Deck.toDeck());
        return versionTransformation.apply(versionedDeck);
    }

    private static final ImmutableSet<Deck.Section> DECK_SECTIONS = Sets.immutableEnumSet(EnumSet.allOf(Deck.Section.class));

    private Deck<V> chooseVersion(Deck.Entry<Card> entry) {
        Card card = entry.getCard();

        Optional<V> favoriteAvailableVersion = versionExtractor.fromCard(card)
                .filter((V version) -> availability.applyAsInt(version) >= entry.getTotal())
                .min(preference);
        if (favoriteAvailableVersion.isPresent()) {
            Deck.Builder<V> builder = new Deck.Builder<>();
            for (Deck.Section section : DECK_SECTIONS) {
                builder.addTo(section, favoriteAvailableVersion.get(), entry.getNumberIn(section));
            }
            return builder.build();
        }

        // Else, there are not enough copies of any one version to match.
        Deck.Builder<V> accumulation = new Deck.Builder<>();
        List<V> orderedVersions = versionExtractor.fromCard(card)
                .filter((V version) -> availability.applyAsInt(version) > 0)
                .sorted(preference)
                .collect(Collectors.toList());

        // First, fill the sections with matching groups of cards. Iterate over the sections in descending order of
        // importance, and use whichever version has enough cards to match, if any.
        for (Deck.Section section : DECK_SECTIONS) {
            int numberWanted = entry.getNumberIn(section);
            if (numberWanted == 0) continue;
            for (V version : orderedVersions) {
                int numberAvailable = availability.applyAsInt(version) - accumulation.getTotalCopiesOf(version);
                if (numberAvailable >= numberWanted) {
                    accumulation.addTo(section, version, numberWanted);
                    break;
                }
            }
        }
        if (accumulation.getSize() == entry.getTotal()) return accumulation.build();

        // Then, use whatever is available in preferred order, with mismatched groups if necessary.
        for (V version : orderedVersions) {
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
        V overflowVersion = !orderedVersions.isEmpty() ? orderedVersions.get(0) :
                versionExtractor.fromCard(card).min(overflow)
                        .orElseThrow(() -> {
                            return new IllegalArgumentException("Versions not found for " + card);
                        });
        for (Deck.Section section : DECK_SECTIONS) {
            int numberOfOverflowCopies = entry.getNumberIn(section) - accumulation.get(section).size();
            accumulation.addTo(section, overflowVersion, numberOfOverflowCopies);
        }
        return accumulation.build();
    }

}
