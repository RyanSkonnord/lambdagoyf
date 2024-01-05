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

package io.github.ryanskonnord.lambdagoyf.deck.preference.demo;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import com.google.common.math.IntMath;
import io.github.ryanskonnord.lambdagoyf.card.Card;
import io.github.ryanskonnord.lambdagoyf.card.CardFace;
import io.github.ryanskonnord.lambdagoyf.card.Color;
import io.github.ryanskonnord.lambdagoyf.card.Spoiler;
import io.github.ryanskonnord.lambdagoyf.card.Word;
import io.github.ryanskonnord.lambdagoyf.card.field.Format;
import io.github.ryanskonnord.lambdagoyf.deck.Deck;
import io.github.ryanskonnord.util.MapCollectors;

import java.math.RoundingMode;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SnowConversion {

    public static ImmutableSet<Card> lookUpBasicSnowLands(Spoiler spoiler) {
        return Color.getBasicLandTypes().stream()
                .map(basicLandType -> spoiler.lookUpByName("Snow-Covered " + basicLandType).orElseThrow())
                .collect(ImmutableSet.toImmutableSet());
    }

    // Non-snow lands as keys; snow lands as values
    private final ImmutableBiMap<Card, Card> snowCards;

    private final ImmutableSet<Word<Format>> snowLegality;

    public SnowConversion(Spoiler spoiler) {
        this.snowCards = Color.getBasicLandTypes().stream()
                .collect(MapCollectors.<String>collecting()
                        .withKey(basicLandType -> spoiler.lookUpByName(basicLandType).orElseThrow())
                        .withValue(basicLandType -> spoiler.lookUpByName("Snow-Covered " + basicLandType).orElseThrow())
                        .toImmutableBiMap());
        this.snowLegality = this.snowCards.values().stream()
                .map((Card card) -> card.getCardLegality().getPermittedFormats()
                        .collect(ImmutableSet.toImmutableSet()))
                .distinct()
                .collect(Collectors.collectingAndThen(ImmutableList.toImmutableList(), SnowConversion::getIntersection));
    }

    private static ImmutableSet<Word<Format>> getIntersection(Collection<ImmutableSet<Word<Format>>> sets) {
        Iterator<ImmutableSet<Word<Format>>> iterator = sets.iterator();
        Set<Word<Format>> values = new TreeSet<>(iterator.next());
        while (iterator.hasNext()) {
            values.retainAll(iterator.next());
        }
        return ImmutableSet.copyOf(values);
    }

    public Deck<Card> convert(Deck<Card> deck) {
        if (containsSnowBasics(deck) || !inferSnowLandLegality(deck)) {
            return deck;
        }
        return deck.transform((Card card) -> {
            Card snowEquivalent = snowCards.get(card);
            return snowEquivalent == null ? card : snowEquivalent;
        });
    }

    public Deck<Card> convertWithFieldBluff(Deck<Card> deck) {
        if (containsSnowBasics(deck) || !inferSnowLandLegality(deck)
                || deck.getAllCards().elementSet().stream().anyMatch(this::isSnowRelevant)) {
            return deck;
        }

        return deck.transformCards((Multiset.Entry<Card> entry) -> {
            Card card = entry.getElement();
            Card snowEquivalent = snowCards.get(card);
            if (snowEquivalent == null) return null;

            int count = entry.getCount();
            int snowCopies = IntMath.divide(count, 2, RoundingMode.UP);
            return ImmutableMultiset.<Card>builder()
                    .addCopies(card, count - snowCopies)
                    .addCopies(snowEquivalent, snowCopies)
                    .build();
        });
    }

    private boolean inferSnowLandLegality(Deck<Card> deck) {
        return snowLegality.stream().anyMatch((Word<Format> format) ->
                deck.getAllCards().elementSet().stream().allMatch((Card card) ->
                        card.getCardLegality().isPermittedIn(format)));
    }


    private boolean containsSnowBasics(Deck<Card> deck) {
        return containsAnyOf(deck, snowCards.values());
    }

    private static boolean containsAnyOf(Deck<Card> deck, Set<Card> targetCards) {
        Set<Card> allCards = deck.getAllCards().elementSet();
        return targetCards.stream().anyMatch(allCards::contains);
    }

    private static final Pattern SNOW_WORD = Pattern.compile(
            "\\{S}|\\b(snow|land( card)?s? with (different|the same) names?)\\b",
            Pattern.CASE_INSENSITIVE);

    private boolean isSnowRelevant(Card card) {
        return card.getFaces().stream().anyMatch((CardFace face) -> {
            String text = face.getManaCost().orElse("") + "\n" + face.getOracleText();
            return SNOW_WORD.matcher(text).find();
        });
    }

    public Deck<Card> removeSuperfluousSnowLands(Deck<Card> deck) {
        if (!containsSnowBasics(deck)) return deck;
        boolean hasSuperfluousSnowLands = deck.getAllCards().elementSet().stream()
                .noneMatch(card -> !snowCards.containsValue(card) && isSnowRelevant(card));
        if (hasSuperfluousSnowLands) {
            return deck.transform((Card card) -> {
                Card nonsnowEquivalent = snowCards.inverse().get(card);
                return nonsnowEquivalent == null ? card : nonsnowEquivalent;
            });
        }
        return deck;
    }

}
