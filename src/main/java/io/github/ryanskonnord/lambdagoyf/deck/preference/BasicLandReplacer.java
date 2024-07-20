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
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import io.github.ryanskonnord.lambdagoyf.card.Card;
import io.github.ryanskonnord.lambdagoyf.card.CardIdentity;
import io.github.ryanskonnord.lambdagoyf.card.CardVersion;
import io.github.ryanskonnord.lambdagoyf.card.field.CardSupertype;
import io.github.ryanskonnord.lambdagoyf.deck.Deck;
import io.github.ryanskonnord.lambdagoyf.deck.DeckRandomChoice;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.UnaryOperator;

public interface BasicLandReplacer<V extends CardVersion> extends UnaryOperator<Deck<V>> {

    public static final class BasicLandReplacementException extends RuntimeException {
    }

    private static boolean isBasic(CardIdentity card) {
        return card.getCard().getMainTypeLine().is(CardSupertype.BASIC);
    }

    private static <E> void sortIfComparable(List<E> list) {
        if (list.stream().allMatch(e -> e instanceof Comparable)) {
            list.sort((Comparator<? super E>) Comparator.naturalOrder());
        }
    }

    public static <V extends CardVersion> BasicLandReplacer<V> fromVersions(Iterable<? extends V> versions) {
        ImmutableMap<Card, ? extends V> byCard = Maps.uniqueIndex(versions, CardVersion::getCard);
        return deck -> deck.flatTransform((V version) -> {
            Card card = version.getCard();
            if (!BasicLandReplacer.isBasic(card)) return Optional.empty();
            V replacement = byCard.get(card);
            if (replacement == null) throw new BasicLandReplacementException();
            return Optional.of(replacement);
        });
    }

    public static <V extends CardVersion> BasicLandReplacer<V> fromVersionsChosenRandomly(Iterable<? extends V> versions) {
        ListMultimap<Card, V> byCard = Multimaps.newListMultimap(new TreeMap<>(), ArrayList::new);
        for (V version : versions) {
            byCard.put(version.getCard(), version);
        }
        for (List<V> versionGroup : Multimaps.asMap(byCard).values()) {
            sortIfComparable(versionGroup);
        }

        return (Deck<V> deck) -> {
            DeckRandomChoice rng = DeckRandomChoice.withSalt(0xd1cb53f5e85b965bL).forDeck(deck);
            Collection<List<V>> versionGroups = Multimaps.asMap(byCard).values();
            Collection<V> choices = new ArrayList<>(versionGroups.size());
            for (List<V> versionGroup : versionGroups) {
                choices.add(rng.choose(versionGroup));
            }
            BasicLandReplacer<V> delegate = fromVersions(choices);
            return delegate.apply(deck);
        };
    }

}
