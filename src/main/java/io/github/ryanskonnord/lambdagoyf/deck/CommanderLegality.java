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

import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import io.github.ryanskonnord.lambdagoyf.card.Card;
import io.github.ryanskonnord.lambdagoyf.card.CardIdentity;
import io.github.ryanskonnord.lambdagoyf.card.ColorSet;
import io.github.ryanskonnord.lambdagoyf.card.field.CardSupertype;
import io.github.ryanskonnord.lambdagoyf.deck.Deck.Section;

import java.util.Set;

public class CommanderLegality {

    public static <C extends CardIdentity> boolean checkLegality(Deck<C> deck, Multiset<C> commanders) {
        Set<C> cardsInDeck = Multisets.union(deck.get(Section.COMPANION), deck.get(Section.MAIN_DECK)).elementSet();
        for (C commander : commanders.elementSet()) {
            Card commanderCard = commander.getCard();
            if (!commanderCard.getMainTypeLine().is(CardSupertype.LEGENDARY)) {
                return false;
            }
            ColorSet colorIdentity = commanderCard.getColorIdentity();
            boolean cardsConform = cardsInDeck.stream().allMatch(
                    (C c) -> colorIdentity.containsAll(c.getCard().getColorIdentity()));
            if (!cardsConform) {
                return false;
            }
        }
        return true;
    }

    public static <C extends CardIdentity> Deck<C> inferCommander(Deck<C> deck) {
        Multiset<C> sideboard = deck.get(Section.SIDEBOARD);
        if (sideboard.isEmpty() || sideboard.size() > 3) {
            return deck;
        }

        if (sideboard.size() > 1) {
            deck = CompanionLegality.extractCompanion(deck);
            sideboard = deck.get(Section.SIDEBOARD);
        }
        if (deck.get(Section.COMMANDER).isEmpty() && checkLegality(deck, sideboard)) {
            Deck.Builder<C> mutableCopy = deck.createMutableCopy();
            mutableCopy.get(Section.SIDEBOARD).clear();
            mutableCopy.addTo(Section.COMMANDER, sideboard);
            return mutableCopy.build();
        } else {
            return deck;
        }
    }

}
