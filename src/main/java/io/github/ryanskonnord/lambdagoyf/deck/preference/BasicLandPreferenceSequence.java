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

package io.github.ryanskonnord.lambdagoyf.deck.preference;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import io.github.ryanskonnord.lambdagoyf.card.Card;
import io.github.ryanskonnord.lambdagoyf.card.CardVersion;
import io.github.ryanskonnord.lambdagoyf.card.CardVersionExtractor;
import io.github.ryanskonnord.lambdagoyf.card.DeckElement;
import io.github.ryanskonnord.lambdagoyf.card.Spoiler;
import io.github.ryanskonnord.lambdagoyf.card.field.CardSupertype;
import io.github.ryanskonnord.lambdagoyf.deck.Deck;
import io.github.ryanskonnord.lambdagoyf.deck.DeckRandomChoice;
import io.github.ryanskonnord.util.MapCollectors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

public final class BasicLandPreferenceSequence<V extends CardVersion> {

    private static class Step<V extends CardVersion> {
        private final ImmutableListMultimap<Card, V> versions;

        public Step(Multimap<Card, V> versions) {
            this.versions = ImmutableListMultimap.copyOf(versions);
            for (Map.Entry<Card, V> entry : this.versions.entries()) {
                Preconditions.checkArgument(entry.getValue().getCard().equals(entry.getKey()));
            }
        }
    }

    public static <V> ToIntFunction<V> infiniteAvailability(Predicate<V> predicate) {
        Objects.requireNonNull(predicate);
        return v -> predicate.test(v) ? Integer.MAX_VALUE : 0;
    }

    public static <V> ToIntFunction<V> infiniteAvailability() {
        return infiniteAvailability(v -> true);
    }

    public static final class Context<V extends CardVersion> {
        private final Spoiler spoiler;
        private final CardVersionExtractor<V> extractor;
        private final ToIntFunction<V> availability;

        public Context(Spoiler spoiler, CardVersionExtractor<V> extractor, ToIntFunction<V> availability) {
            this.spoiler = Objects.requireNonNull(spoiler);
            this.extractor = Objects.requireNonNull(extractor);
            this.availability = Objects.requireNonNull(availability);
        }

        public Builder<V> builder() {
            return new Builder<>(this);
        }
    }

    public static final class Builder<V extends CardVersion> {
        private final Context<V> context;
        private final ImmutableList.Builder<Predicate<? super V>> criteria = ImmutableList.builder();
        private boolean defaultByMixingAll = false;

        private Builder(Context<V> context) {
            this.context = context;
        }

        public Builder<V> defaultByMixingAll() {
            this.defaultByMixingAll = true;
            return this;
        }

        public Builder<V> add(Predicate<? super V> element) {
            criteria.add(element);
            return this;
        }

        public Builder<V> addAll(Iterable<? extends Predicate<? super V>> elements) {
            criteria.addAll(elements);
            return this;
        }

        public BasicLandPreferenceSequence<V> build() {
            Set<Card> basicLandCards = context.spoiler.getCards().stream()
                    .filter(c -> c.getMainTypeLine().is(CardSupertype.BASIC))
                    .collect(ImmutableSet.toImmutableSet());
            List<Step<V>> steps = criteria.build().stream()
                    .map((Predicate<? super V> criterion) -> basicLandCards.stream()
                            .collect(MapCollectors.<Card>collecting().identityKey()
                                    .withFlatValues(card -> context.extractor.fromCard(card).filter(criterion))
                                    .toImmutableListMultimap()))
                    .map(Step::new)
                    .collect(ImmutableList.toImmutableList());
            return new BasicLandPreferenceSequence<>(steps, context.availability, defaultByMixingAll);
        }
    }

    private final ImmutableList<Step<V>> steps;
    private final ImmutableMultimap<Card, V> applicableCards;
    private final ToIntFunction<V> availability;
    private final boolean defaultByMixingAll;

    private BasicLandPreferenceSequence(List<Step<V>> steps, ToIntFunction<V> availability, boolean defaultByMixingAll) {
        this.steps = ImmutableList.copyOf(steps);
        this.defaultByMixingAll = defaultByMixingAll;
        this.applicableCards = this.steps.stream()
                .flatMap(s -> s.versions.values().stream())
                .collect(MapCollectors.<V>collecting()
                        .indexing(CardVersion::getCard)
                        .grouping().toImmutableListMultimap());
        this.availability = Objects.requireNonNull(availability);
    }

    private Optional<Map<Card, V>> chooseVersions(Deck<V> deck) {
        DeckRandomChoice seed = DeckRandomChoice.withSalt(0x5e469b12d2af4b40L).forDeck(deck);
        Multiset<Card> inDeck = Multisets.filter(Deck.toCards(deck).getAllCards(), applicableCards::containsKey);
        for (int i = 0; i < steps.size(); i++) {
            Step<V> step = steps.get(i);
            Optional<Map<Card, V>> chosenVersions = attemptChoice(inDeck, step.versions::get, seed);
            if (chosenVersions.isPresent()) return chosenVersions;
        }
        return defaultByMixingAll ? attemptChoice(inDeck, applicableCards::get, seed) : Optional.empty();
    }

    private Optional<Map<Card, V>> attemptChoice(Multiset<Card> inDeck,
                                                 Function<Card, Collection<V>> versionFunction,
                                                 DeckRandomChoice seed) {
        Map<Card, V> chosenVersions = Maps.newHashMapWithExpectedSize(inDeck.elementSet().size());
        List<Multiset.Entry<Card>> cardEntries = inDeck.entrySet().stream()
                .sorted(Comparator.comparing(Multiset.Entry::getElement))
                .collect(Collectors.toList());
        for (Multiset.Entry<Card> entry : cardEntries) {
            Card card = entry.getElement();
            List<V> availableVersions = new ArrayList<>(versionFunction.apply(card));
            availableVersions = seed.forCard(card).shuffle(availableVersions);
            Optional<V> chosenVersion = availableVersions.stream()
                    .filter((V version) -> availability.applyAsInt(version) >= entry.getCount())
                    .findFirst();
            if (chosenVersion.isPresent()) {
                chosenVersions.put(card, chosenVersion.get());
            } else {
                return Optional.empty();
            }
        }
        return Optional.of(chosenVersions);
    }

    public Deck<V> apply(Deck<V> deck) {
        return chooseVersions(deck)
                .map((Map<Card, V> chosenBasicLandVersions) -> deck.transform((V version) -> {
                    V alteredVersion = chosenBasicLandVersions.get(version.getCard());
                    return alteredVersion == null ? version : alteredVersion;
                }))
                .orElse(deck);
    }

    public <E extends DeckElement<V>> Deck<E> apply(Deck<E> deck, Function<? super V, ? extends E> elementCtor) {
        Objects.requireNonNull(elementCtor);
        Deck<V> versionedDeck = deck.flatTransform(DeckElement::getVersion);
        return chooseVersions(versionedDeck)
                .map((Map<Card, V> chosenBasicLandVersions) ->
                        deck.transform((E element) -> {
                            Optional<V> version = element.getVersion();
                            if (version.isEmpty()) return null;
                            V basicLandVersion = chosenBasicLandVersions.get(version.get().getCard());
                            return basicLandVersion == null ? element : elementCtor.apply(basicLandVersion);
                        }))
                .orElse(deck);
    }

}
