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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import io.github.ryanskonnord.lambdagoyf.card.ArenaCard;
import io.github.ryanskonnord.lambdagoyf.card.CardEdition;
import io.github.ryanskonnord.lambdagoyf.card.Expansion;
import io.github.ryanskonnord.lambdagoyf.card.Spoiler;
import io.github.ryanskonnord.lambdagoyf.card.field.CardSupertype;
import io.github.ryanskonnord.lambdagoyf.card.field.Rarity;
import io.github.ryanskonnord.lambdagoyf.scryfall.ScryfallParser;

import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ArenaWildcardSpender {

    private final Spoiler spoiler;

    public ArenaWildcardSpender(Spoiler spoiler) {
        this.spoiler = Objects.requireNonNull(spoiler);
    }

    public Collection<Deck<ArenaCard>> createDecks(Expansion expansion,
                                                   Rarity rarity,
                                                   int wildcardsToSpend) {
        Objects.requireNonNull(expansion);
        Objects.requireNonNull(rarity);
        Predicate<ArenaCard> condition = (ArenaCard card) -> {
            if (card.getCard().getMainTypeLine().is(CardSupertype.BASIC)) return false;
            CardEdition edition = card.getEdition();
            return edition.getExpansion().equals(expansion) && edition.getRarity().is(rarity);
        };
        return createDecks(condition, wildcardsToSpend);
    }

    private static final int ARENA_MAX_DECK_SIZE = 250;

    public Collection<Deck<ArenaCard>> createDecks(Predicate<? super ArenaCard> condition,
                                                   int wildcardsToSpend) {
        List<ArenaCard> cardsToBuy = spoiler.getCards().stream().flatMap(c -> c.getEditions().stream())
                .flatMap(e -> e.getArenaCard().stream())
                .filter(condition)
                .sorted()
                .collect(Collectors.toList());

        if (cardsToBuy.isEmpty()) return ImmutableList.of();
        int copiesPerCard = Math.min(wildcardsToSpend / cardsToBuy.size(), 4);
        if (copiesPerCard <= 0) return ImmutableList.of();
        int cardsPerDeck = ARENA_MAX_DECK_SIZE / copiesPerCard;

        return Lists.partition(cardsToBuy, cardsPerDeck).stream()
                .map(partition -> Deck.createSimpleDeck(partition, copiesPerCard))
                .collect(Collectors.toList());
    }

    public static void main(String[] args) throws Exception {
        Spoiler spoiler = ScryfallParser.createSpoiler();
        ArenaWildcardSpender spender = new ArenaWildcardSpender(spoiler);
        Expansion mh3 = spoiler.getExpansion("MH3").orElseThrow();
        Collection<Deck<ArenaCard>> commons = spender.createDecks(mh3, Rarity.COMMON, 504);
        Collection<Deck<ArenaCard>> uncommons = spender.createDecks(mh3, Rarity.UNCOMMON, 675);
        try (OutputStreamWriter writer = new OutputStreamWriter(System.out)) {
            for (Deck<ArenaCard> deck : Iterables.concat(commons, uncommons)) {
                ArenaDeckFormatter.write(writer, deck.transform(ArenaDeckEntry::new));
                writer.append("\n");
            }
        }
    }

}
