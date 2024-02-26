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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Multiset;
import io.github.ryanskonnord.lambdagoyf.card.Card;
import io.github.ryanskonnord.lambdagoyf.card.CardVersion;
import io.github.ryanskonnord.lambdagoyf.card.CardVersionExtractor;
import io.github.ryanskonnord.lambdagoyf.card.DeckElement;
import io.github.ryanskonnord.lambdagoyf.deck.Deck;
import io.github.ryanskonnord.lambdagoyf.deck.DeckRandomChoice;
import io.github.ryanskonnord.util.MapCollectors;
import io.github.ryanskonnord.util.MultisetUtil;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.function.UnaryOperator;

public class GroupReplacementWithAvailability<V extends CardVersion, T extends DeckElement<V>> implements UnaryOperator<Deck<T>> {

    private final CardVersionExtractor<V> extractor;
    private final Function<V, T> outputCtor;
    private final Predicate<? super V> groupPredicate;
    private final ToIntFunction<T> availabilityFunction;
    private final long salt;

    public GroupReplacementWithAvailability(CardVersionExtractor<V> extractor,
                                            Function<V, T> outputCtor,
                                            Predicate<? super V> groupPredicate,
                                            ToIntFunction<T> availabilityFunction,
                                            long salt) {
        this.extractor = Objects.requireNonNull(extractor);
        this.outputCtor = Objects.requireNonNull(outputCtor);
        this.groupPredicate = Objects.requireNonNull(groupPredicate);
        this.availabilityFunction = Objects.requireNonNull(availabilityFunction);
        this.salt = salt;
    }

    @Override
    public Deck<T> apply(Deck<T> deck) {
        Deck<Card> versionedDeck = deck.flatTransform(e -> e.getVersion().map(CardVersion::getCard));
        Multiset<Card> cards = versionedDeck.getAllCards();
        ListMultimap<Card, V> versions = cards.elementSet().stream()
                .sorted()
                .collect(MapCollectors.<Card>collecting()
                        .flattening(card -> extractor.fromCard(card).filter(groupPredicate))
                        .toImmutableListMultimap());

        List<Map.Entry<Card, List<V>>> replacements = ImmutableList.copyOf(Multimaps.asMap(versions).entrySet());
        DeckRandomChoice[] seeds = DeckRandomChoice.withSalt(salt).getArrayForDeck(versionedDeck, replacements.size());
        Map<Card, V> choices = Maps.newHashMapWithExpectedSize(replacements.size());
        for (int i = 0; i < replacements.size(); i++) {
            Map.Entry<Card, List<V>> entry = replacements.get(i);
            Card card = entry.getKey();
            int numberInDeck = cards.count(card);
            List<V> candidates = seeds[i].shuffle(entry.getValue());

            Optional<V> choice = candidates.stream()
                    .filter(candidate -> availabilityFunction.applyAsInt(outputCtor.apply(candidate)) >= numberInDeck)
                    .findFirst();
            if (choice.isPresent()) {
                choices.put(card, choice.get());
            } else {
                return deck;
            }
        }

        return deck.transformCards((Multiset.Entry<T> entry) -> {
            Optional<V> version = entry.getElement().getVersion();
            if (version.isEmpty()) return null;
            Card card = version.get().getCard();
            V choice = choices.get(card);
            return choice == null ? null : MultisetUtil.ofSingleEntry(outputCtor.apply(choice), entry.getCount());
        });
    }
}
