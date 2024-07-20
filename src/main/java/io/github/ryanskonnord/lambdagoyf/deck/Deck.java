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

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.common.collect.TreeMultiset;
import io.github.ryanskonnord.lambdagoyf.card.Card;
import io.github.ryanskonnord.lambdagoyf.card.CardEdition;
import io.github.ryanskonnord.lambdagoyf.card.CardIdentity;
import io.github.ryanskonnord.lambdagoyf.card.CardVersion;
import io.github.ryanskonnord.util.MapCollectors;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Deck<C> {

    public static enum Section {
        COMMANDER("Commander"),
        COMPANION("Companion"),
        MAIN_DECK("Deck"),
        SIDEBOARD("Sideboard");

        private final String label;

        Section(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        private static final ImmutableSortedMap<String, Section> BY_LABEL = EnumSet.allOf(Section.class).stream()
                .collect(MapCollectors.<Section>collecting()
                        .indexing(Section::getLabel)
                        .unique().toImmutableSortedMap(String.CASE_INSENSITIVE_ORDER));

        public static Optional<Section> fromLabel(String label) {
            return Optional.ofNullable(BY_LABEL.get(label));
        }
    }

    private static int getExpectedElements(Section section) {
        return switch (section) {
            case COMMANDER -> 2;
            case COMPANION -> 1;
            case MAIN_DECK -> 30;
            case SIDEBOARD -> 15;
        };
    }

    public static final class Builder<C> {
        private final Map<Section, Multiset<C>> sectionMap = new EnumMap<>(Section.class);

        public Multiset<C> get(Section section) {
            return sectionMap.computeIfAbsent(section, s -> LinkedHashMultiset.create(getExpectedElements(s)));
        }

        public Builder<C> addTo(Section section, C card, int copies) {
            get(section).add(card, copies);
            return this;
        }

        public Builder<C> addTo(Section section, Collection<? extends C> cards) {
            get(section).addAll(cards);
            return this;
        }

        public Builder<C> addAll(Deck<? extends C> deck) {
            for (Map.Entry<Section, ? extends ImmutableMultiset<? extends C>> entry : deck.sectionMap.entrySet()) {
                addTo(entry.getKey(), entry.getValue());
            }
            return this;
        }

        public int getSize() {
            return sectionMap.values().stream().mapToInt(Collection::size).sum();
        }

        public int getTotalCopiesOf(C card) {
            return sectionMap.values().stream().mapToInt(section -> section.count(card)).sum();
        }

        private Builder<C> combine(Builder<C> that) {
            for (Map.Entry<Section, Multiset<C>> entry : that.sectionMap.entrySet()) {
                addTo(entry.getKey(), entry.getValue());
            }
            return this;
        }

        public Deck<C> build() {
            return new Deck<>(sectionMap);
        }
    }

    public static <C> Collector<Deck<C>, ?, Deck<C>> toDeck() {
        return Collector.<Deck<C>, Builder<C>, Deck<C>>of(
                Builder::new, Builder::addAll, Builder::combine, Builder::build);
    }

    public static <C> Deck<C> createSimpleDeck(Collection<? extends C> cards) {
        return new Deck<>(ImmutableMap.of(Section.MAIN_DECK, ImmutableMultiset.copyOf(cards)));
    }
    public static <C> Deck<C> createSimpleDeck(Collection<? extends C> cards, int copiesOfEach) {
        Multiset<C> multiset = LinkedHashMultiset.create(cards.size());
        for (C card : cards) {
            multiset.add(card, copiesOfEach);
        }
        return new Deck<>(ImmutableMap.of(Section.MAIN_DECK, multiset));
    }


    private final ImmutableMap<Section, ImmutableMultiset<C>> sectionMap;

    private Deck(Map<Section, Multiset<C>> sectionMap) {
        Map<Section, ImmutableMultiset<C>> builder = new EnumMap<>(Section.class);
        for (Map.Entry<Section, ? extends Collection<C>> entry : sectionMap.entrySet()) {
            ImmutableMultiset<C> cards = ImmutableMultiset.copyOf(entry.getValue());
            if (!cards.isEmpty()) {
                builder.put(entry.getKey(), cards);
            }
        }
        this.sectionMap = Maps.immutableEnumMap(builder);
    }

    public ImmutableMultiset<C> get(Section section) {
        return sectionMap.getOrDefault(section, ImmutableMultiset.of());
    }

    public Multiset<C> getAllCards() {
        List<ImmutableMultiset<C>> sectionSets = sectionMap.values().asList();
        return switch (sectionSets.size()) {
            case 0 -> ImmutableMultiset.of();
            case 1 -> sectionSets.get(0);
            case 2 -> Multisets.sum(sectionSets.get(0), sectionSets.get(1));
            default -> sectionSets.stream().flatMap((Multiset<C> m) -> m.entrySet().stream())
                    .collect(MapCollectors.<C>collectingMultisetEntries().toImmutableMultiset());
        };
    }

    public Iterable<Map.Entry<Section, ImmutableMultiset<C>>> getAllSections() {
        return sectionMap.entrySet();
    }

    /**
     * Get the sum of all sections other than the main deck. This is the sideboard as it would be represented in MTGO or
     * in a tournament. Although Arena models the companion as a section outside the sideboard, it must still be
     * considered part of the sideboard for purposes of determining deck legality (i.e., 15 or fewer cards in the
     * sideboard).
     */
    public Multiset<C> getLegalSideboard() {
        // Account for typical real-world cases, to avoid building a new multiset if we can.
        // In no ordinary format does a deck have both a sideboard and a commander,
        // and only a minority of decks in every format have a companion (except of course from April 17 to June 1, 2020 :-P).
        Multiset<C> commander = get(Section.COMMANDER);
        Multiset<C> companion = get(Section.COMPANION);
        Multiset<C> sideboard = get(Section.SIDEBOARD);

        if (commander.isEmpty()) {
            return companion.isEmpty() ? sideboard : Multisets.sum(companion, sideboard);
        } else if (sideboard.isEmpty()) {
            return companion.isEmpty() ? commander : Multisets.sum(commander, companion);
        }
        return ImmutableMultiset.<C>builder()
                .addAll(commander).addAll(companion).addAll(sideboard)
                .build();

    }

    public Deck.Builder<C> createMutableCopy() {
        Builder<C> builder = new Builder<>();
        for (Map.Entry<Section, ImmutableMultiset<C>> entry : sectionMap.entrySet()) {
            builder.addTo(entry.getKey(), entry.getValue());
        }
        return builder;
    }


    public static final class Entry<C> {
        private final C card;
        private final ImmutableMultiset<Section> copiesPerSection;

        private Entry(C card, Multiset<Section> copiesPerSection) {
            this.card = Objects.requireNonNull(card);
            this.copiesPerSection = ImmutableMultiset.copyOf(copiesPerSection);
        }

        public C getCard() {
            return card;
        }

        public int getNumberIn(Section section) {
            return copiesPerSection.count(section);
        }

        public int getTotal() {
            return copiesPerSection.size();
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("card", card)
                    .add("copiesPerSection", copiesPerSection)
                    .toString();
        }

        @Override
        public boolean equals(Object o) {
            return this == o || o instanceof Entry
                    && card.equals(((Entry<?>) o).card)
                    && copiesPerSection.equals(((Entry<?>) o).copiesPerSection);
        }

        @Override
        public int hashCode() {
            return 31 * card.hashCode() + copiesPerSection.hashCode();
        }
    }

    private Entry<C> getEntryFor(C card) {
        ImmutableMultiset.Builder<Section> copiesPerSection = ImmutableMultiset.builder();
        for (Map.Entry<Section, ImmutableMultiset<C>> entry : sectionMap.entrySet()) {
            copiesPerSection.addCopies(entry.getKey(), entry.getValue().count(card));
        }
        return new Entry<>(card, copiesPerSection.build());
    }

    public Stream<Entry<C>> getEntries() {
        return getAllCards().elementSet().stream().map(this::getEntryFor);
    }

    public int getTotalCopiesOf(C card) {
        return sectionMap.values().stream().mapToInt(section -> section.count(card)).sum();
    }


    private static <C, D> ImmutableMultiset<D> transformMultiset(Multiset<C> multiset,
                                                                 Function<? super C, ? extends D> function) {
        Objects.requireNonNull(function);
        return multiset.entrySet().stream().collect(MapCollectors.<Multiset.Entry<C>>collecting()
                .withKey(entry -> (D) function.apply(entry.getElement()))
                .countingBy(Multiset.Entry::getCount)
                .toImmutableMultiset());
    }

    public <D> Deck<D> transform(Function<? super C, ? extends D> function) {
        return new Deck<D>(Maps.transformValues(sectionMap, valueSet -> transformMultiset(valueSet, function)));
    }

    public static <V extends CardVersion> Deck<CardEdition> toEditions(Deck<V> deck) {
        return deck.transform(CardVersion::getEdition);
    }

    public static <C extends CardIdentity> Deck<Card> toCards(Deck<C> deck) {
        return deck.transform(CardIdentity::getCard);
    }


    private static <C, D> ImmutableMultiset<D> flatTransformMultiset(Multiset<C> multiset,
                                                                     Function<? super C, Optional<? extends D>> function) {
        Objects.requireNonNull(function);
        return multiset.entrySet().stream()
                .flatMap((Multiset.Entry<C> originalEntry) -> {
                    Optional<? extends D> transformedElement = function.apply(originalEntry.getElement());
                    Optional<Multiset.Entry<D>> transformedEntry = transformedElement
                            .map((D transformed) -> Multisets.immutableEntry(transformed, originalEntry.getCount()));
                    return transformedEntry.stream();
                })
                .collect(MapCollectors.<D>collectingMultisetEntries().toImmutableMultiset());
    }

    public <D> Deck<D> flatTransform(Function<? super C, Optional<? extends D>> function) {
        return new Deck<>(Maps.transformValues(sectionMap, valueSet -> flatTransformMultiset(valueSet, function)));
    }

    private static <C> ImmutableMultiset<C> flatTransformEntries(Multiset<C> multiset,
                                                                 Function<Multiset.Entry<C>, Multiset<C>> function) {
        Objects.requireNonNull(function);
        return multiset.entrySet().stream()
                .flatMap((Multiset.Entry<C> originalEntry) -> {
                    Multiset<C> transformedElements = function.apply(originalEntry);
                    if (transformedElements == null) return Stream.of(originalEntry);
                    return transformedElements.entrySet().stream();
                })
                .collect(MapCollectors.<C>collectingMultisetEntries().toImmutableMultiset());
    }

    /**
     * Transform the quantity of each unique card into a multiset of other cards.
     *
     * @param function a function that accepts a number of instances of a single card and returns the multiset of cards
     *                 to replace it, or {@code null} for no change
     * @return the transformed deck
     */
    public Deck<C> transformCards(Function<Multiset.Entry<C>, Multiset<C>> function) {
        return new Deck<>(Maps.transformValues(sectionMap, valueSet -> flatTransformEntries(valueSet, function)));
    }


    public Deck<C> sortCards(Comparator<? super C> order) {
        Objects.requireNonNull(order);
        return sortCards(section -> order);
    }

    public Deck<C> sortCards(Function<? super Section, Comparator<? super C>> orderFunction) {
        Objects.requireNonNull(orderFunction);
        return sortEntries(section -> Comparator.comparing(
                Multiset.Entry::getElement,
                orderFunction.apply(section)));
    }

    public Deck<C> sortEntries(Comparator<? super Multiset.Entry<C>> order) {
        Objects.requireNonNull(order);
        return sortEntries(section -> order);
    }

    public Deck<C> sortEntries(Function<? super Section, Comparator<? super Multiset.Entry<C>>> orderFunction) {
        Objects.requireNonNull(orderFunction);
        Builder<C> builder = new Builder<>();
        for (Map.Entry<Section, ImmutableMultiset<C>> sectionEntry : sectionMap.entrySet()) {
            Section section = sectionEntry.getKey();
            List<Multiset.Entry<C>> cardEntries = new ArrayList<>(sectionEntry.getValue().entrySet());
            cardEntries.sort(orderFunction.apply(section));
            for (Multiset.Entry<C> cardEntry : cardEntries) {
                builder.addTo(section, cardEntry.getElement(), cardEntry.getCount());
            }
        }
        return builder.build();
    }

    private static <C> ImmutableMultiset<C> sort(Comparator<? super Multiset.Entry<C>> order, Multiset<C> multiset) {
        ImmutableMultiset.Builder<C> builder = ImmutableMultiset.builder();
        multiset.entrySet().stream().sorted(order)
                .forEachOrdered(entry -> builder.addCopies(entry.getElement(), entry.getCount()));
        return builder.build();
    }

    private static String formatPart(Multiset<?> part) {
        return part.entrySet().stream()
                .map(e -> String.format("%dx %s", e.getCount(), e.getElement()))
                .collect(Collectors.joining("; ", "[", "]"));
    }

    @Override
    public String toString() {
        MoreObjects.ToStringHelper helper = MoreObjects.toStringHelper(this);
        for (Map.Entry<Section, ImmutableMultiset<C>> entry : sectionMap.entrySet()) {
            helper = helper.add(entry.getKey().name(), formatPart(entry.getValue()));
        }
        return helper.toString();
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o != null && getClass() == o.getClass() && sectionMap.equals(((Deck<?>) o).sectionMap);
    }

    @Override
    public int hashCode() {
        return sectionMap.hashCode();
    }
}
