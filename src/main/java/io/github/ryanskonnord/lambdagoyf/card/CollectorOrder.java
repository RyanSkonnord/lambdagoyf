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

import io.github.ryanskonnord.lambdagoyf.card.field.CardSupertype;
import io.github.ryanskonnord.lambdagoyf.card.field.CardType;

import java.util.Comparator;

public enum CollectorOrder implements Comparator<CardVersion> {
    INSTANCE;

    @SuppressWarnings("unchecked")
    public static <V extends CardVersion> Comparator<V> instance() {
        return (Comparator<V>) INSTANCE;
    }

    private static enum TopLevelGrouping {
        COLORLESS_NONARTIFACT_NONLAND,
        MONOCOLOR,
        MULTICOLOR,
        COLORLESS_ARTIFACT,
        NONBASIC_LAND,
        BASIC_LAND;

        public static TopLevelGrouping get(Card card) {
            TypeLine typeLine = card.getMainTypeLine();
            if (typeLine.is(CardType.LAND)) return typeLine.is(CardSupertype.BASIC) ? BASIC_LAND : NONBASIC_LAND;
            ColorSet colorSet = card.getColors();
            return colorSet.size() == 0 ? (typeLine.is(CardType.ARTIFACT) ? COLORLESS_ARTIFACT : COLORLESS_NONARTIFACT_NONLAND)
                    : colorSet.size() == 1 ? MONOCOLOR : MULTICOLOR;
        }
    }

    private int compareCards(Card c1, Card c2) {
        TopLevelGrouping g1 = TopLevelGrouping.get(c1);
        TopLevelGrouping g2 = TopLevelGrouping.get(c1);
        if (g1 != g2) return g1.compareTo(g2);
        if (g1 == TopLevelGrouping.MONOCOLOR) {
            int colorCmp = c1.getColors().compareTo(c2.getColors());
            if (colorCmp != 0) return colorCmp;
        }
        return c1.getMainName().compareTo(c2.getMainName());
    }

    @Override
    public int compare(CardVersion v1, CardVersion v2) {
        return compareCards(v1.getCard(), v2.getCard());
    }
}
