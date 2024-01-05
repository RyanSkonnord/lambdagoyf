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

import java.util.stream.Stream;

@FunctionalInterface
public interface CardVersionExtractor<V extends CardVersion> {

    public Stream<V> fromEdition(CardEdition edition);

    public default Stream<V> fromCard(Card card) {
        return card.getEditions().stream().flatMap(this::fromEdition);
    }

    public default Stream<V> fromCards(Stream<Card> cards) {
        return cards.flatMap(this::fromCard);
    }

    public default Stream<V> getAll(Spoiler spoiler) {
        return spoiler.getCards().stream().flatMap(this::fromCard);
    }


    public static CardVersionExtractor<CardEdition> getCardEditions() {
        return Stream::of;
    }

    public static CardVersionExtractor<PaperCard> getPaperCards() {
        return CardEdition::getPaperCards;
    }

    public static CardVersionExtractor<MtgoCard> getMtgoCards() {
        return CardEdition::getMtgoCards;
    }

    public static CardVersionExtractor<ArenaCard> getArenaCard() {
        return e -> e.getArenaCard().stream();
    }

}
