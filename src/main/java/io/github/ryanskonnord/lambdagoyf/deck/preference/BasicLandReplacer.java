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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import io.github.ryanskonnord.lambdagoyf.card.Card;
import io.github.ryanskonnord.lambdagoyf.card.CardIdentity;
import io.github.ryanskonnord.lambdagoyf.card.CardVersion;
import io.github.ryanskonnord.lambdagoyf.card.field.CardSupertype;
import io.github.ryanskonnord.lambdagoyf.deck.Deck;
import io.github.ryanskonnord.lambdagoyf.deck.DeckRandomChoice;
import io.github.ryanskonnord.util.MinimalRng;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public interface BasicLandReplacer<V extends CardVersion> extends UnaryOperator<Deck<V>> {

    private static boolean isBasic(CardIdentity card) {
        return card.getCard().getMainTypeLine().is(CardSupertype.BASIC);
    }

    public static <V extends CardVersion> BasicLandReplacer<V> fromVersions(Iterable<? extends V> versions) {
        ImmutableMap<Card, ? extends V> byCard = Maps.uniqueIndex(versions, CardVersion::getCard);
        return deck -> deck.flatTransform((V version) -> {
            Card card = version.getCard();
            if (!BasicLandReplacer.isBasic(card)) return Optional.of(version);
            V replacement = byCard.get(card);
            if (replacement == null) return Optional.of(version);
            return Optional.of(replacement);
        });
    }

    private static <V extends CardVersion> ListMultimap<Card, V> groupByCard(Iterable<? extends V> versions) {
        ListMultimap<Card, V> byCard = Multimaps.newListMultimap(new TreeMap<>(), () -> new ArrayList<>(4));
        for (V version : versions) {
            byCard.put(version.getCard(), version);
        }
        for (List<V> versionGroup : Multimaps.asMap(byCard).values()) {
            if (versionGroup.stream().allMatch(e -> e instanceof Comparable)) {
                versionGroup.sort((Comparator) Comparator.naturalOrder());
            }
        }
        return byCard;
    }


    public static <V extends CardVersion> BasicLandReplacer<V> fromVersionsChosenRandomly(Iterable<? extends V> versions) {
        ListMultimap<Card, V> byCard = groupByCard(versions);
        return (Deck<V> deck) -> {
            DeckRandomChoice rng = DeckRandomChoice.withSalt(0xd1cb53f5e85b965bL).forDeck(deck);
            return transformDeckWithRandomChoices(deck, byCard, rng);
        };
    }

    private static <V extends CardVersion> Deck<V> transformDeckWithRandomChoices(Deck<V> deck, ListMultimap<Card, V> versionsByCard, DeckRandomChoice rng) {
        ImmutableSet<Card> cardsInDeck = deck.getAllCards().elementSet()
                .stream().map(CardIdentity::getCard).collect(ImmutableSet.toImmutableSet());
        Set<Map.Entry<Card, List<V>>> versionGroups = Multimaps.asMap(versionsByCard).entrySet();
        Collection<V> choices = new ArrayList<>(versionGroups.size());
        MinimalRng statefulRng = rng.getStatefulRng();
        for (Map.Entry<Card, List<V>> versionGroup : versionGroups) {
            Card card = versionGroup.getKey();
            if (cardsInDeck.contains(card)) {
                V choice = statefulRng.choose(versionGroup.getValue());
                choices.add(choice);
            }
        }
        BasicLandReplacer<V> delegate = fromVersions(choices);
        return delegate.apply(deck);
    }

    public static <V extends CardVersion> BasicLandReplacer<V> chooseRandomSet(List<? extends Collection<? extends V>> versionGroups,
                                                                               Collection<? extends V> defaultVersions) {
        List<ListMultimap<Card, V>> groupMaps = versionGroups.stream()
                .map((Collection<? extends V> versionGroup) -> {
                    ListMultimap<Card, V> byCard = groupByCard(versionGroup);
                    for (V version : defaultVersions) {
                        Card card = version.getCard();
                        if (!byCard.containsKey(card)) {
                            byCard.put(card, version);
                        }
                    }
                    return byCard;
                })
                .collect(Collectors.toList());

        return (Deck<V> deck) -> {
            DeckRandomChoice[] rng = DeckRandomChoice.withSalt(0x0e1412eed3655902L).getArrayForDeck(deck, 2);
            ListMultimap<Card, V> chosenGroup = rng[0].choose(groupMaps);

            return transformDeckWithRandomChoices(deck, chosenGroup, rng[1]);
        };
    }
}
