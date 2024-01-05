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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import io.github.ryanskonnord.lambdagoyf.card.Card;
import io.github.ryanskonnord.lambdagoyf.card.CardIdentity;
import io.github.ryanskonnord.lambdagoyf.card.Spoiler;
import io.github.ryanskonnord.lambdagoyf.card.TypeLine;
import io.github.ryanskonnord.lambdagoyf.card.Word;
import io.github.ryanskonnord.lambdagoyf.card.field.CardSupertype;
import io.github.ryanskonnord.lambdagoyf.card.field.CardType;
import io.github.ryanskonnord.lambdagoyf.card.field.Format;
import io.github.ryanskonnord.util.MapCollectors;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.github.ryanskonnord.lambdagoyf.deck.Deck.Section.COMPANION;
import static io.github.ryanskonnord.lambdagoyf.deck.Deck.Section.MAIN_DECK;
import static io.github.ryanskonnord.lambdagoyf.deck.Deck.Section.SIDEBOARD;

public enum CompanionLegality {
    GYRUDA_DOOM_OF_DEPTHS("Gyruda, Doom of Depths") {
        @Override
        public boolean isLegal(Multiset<Card> mainDeck) {
            return allCardsAre(mainDeck, c -> c.getCmc() % 2 == 0);
        }
    },

    JEGANTHA_THE_WELLSPRING("Jegantha, the Wellspring") {
        private final Pattern MANA_SYMBOL = Pattern.compile("\\{.*?\\}");

        @Override
        public boolean isLegal(Multiset<Card> mainDeck) {
            return allCardsAre(mainDeck, c -> {
                String manaCost = c.getBaseFaces().map(f -> f.getManaCost().orElse("")).collect(Collectors.joining());
                if (manaCost.isEmpty()) return true;
                Matcher matcher = MANA_SYMBOL.matcher(manaCost);
                Set<String> symbolsSeen = Sets.newHashSetWithExpectedSize(6);
                while (matcher.find()) {
                    String symbol = matcher.group();
                    if (!symbolsSeen.add(symbol)) {
                        return false;
                    }
                }
                return true;
            });
        }
    },

    KAHEERA_THE_ORPHANGUARD("Kaheera, the Orphanguard") {
        private final ImmutableSet<String> ACCEPTED_CREATURE_TYPES = ImmutableSet.of(
                "Cat", "Elemental", "Nightmare", "Dinosaur", "Beast");

        private boolean isChangelingAbility(String rulesTextParagraph) {
            return rulesTextParagraph.equals("Changeling") || rulesTextParagraph.startsWith("Changeling (");
        }

        private boolean hasChangeling(Card card) {
            Stream<String> rulesTextParagraphs = card.getBaseFaces().flatMap(f -> f.getOracleTextParagraphs().stream());
            return rulesTextParagraphs.anyMatch(this::isChangelingAbility)
                    || card.getCard().getMainName().equals("Mistform Ultimus");
        }

        @Override
        public boolean isLegal(Multiset<Card> mainDeck) {
            return allCardsAre(mainDeck, (Card c) -> {
                TypeLine typeLine = c.getMainTypeLine();
                return !typeLine.is(CardType.CREATURE) ||
                        typeLine.getSubtypes().stream().anyMatch(ACCEPTED_CREATURE_TYPES::contains) ||
                        hasChangeling(c);
            });
        }
    },

    KERUGA_THE_MACROSAGE("Keruga, the Macrosage") {
        @Override
        public boolean isLegal(Multiset<Card> mainDeck) {
            return allCardsAre(mainDeck, c -> c.getCmc() >= 3 || c.getMainTypeLine().is(CardType.LAND));
        }
    },

    LURRUS_OF_THE_DREAM_DEN("Lurrus of the Dream-Den") {
        @Override
        public boolean isLegal(Multiset<Card> mainDeck) {
            return allCardsAre(mainDeck, c -> !c.getMainTypeLine().isPermanentCard() || c.getCmc() <= 2);
        }
    },

    LUTRI_THE_SPELLCHASER("Lutri, the Spellchaser") {
        @Override
        public boolean isLegal(Multiset<Card> mainDeck) {
            return mainDeck.entrySet().stream().allMatch((Multiset.Entry<Card> entry) ->
                    entry.getElement().getMainTypeLine().is(CardType.LAND) || entry.getCount() == 1);
        }
    },

    OBOSH_THE_PREYPIERCER("Obosh, the Preypiercer") {
        @Override
        public boolean isLegal(Multiset<Card> mainDeck) {
            return allCardsAre(mainDeck, c -> c.getCmc() % 2 == 1 || c.getMainTypeLine().is(CardType.LAND));
        }
    },

    UMORI_THE_COLLECTOR("Umori, the Collector") {
        @Override
        public boolean isLegal(Multiset<Card> mainDeck) {
            Set<Word<CardType>> commonTypes = null;
            for (Card card : mainDeck.elementSet()) {
                Collection<Word<CardType>> cardTypes = card.getMainTypeLine().getCardTypes().asList();
                if (!cardTypes.contains(Word.of(CardType.LAND))) {
                    if (commonTypes == null) {
                        commonTypes = new HashSet<>(cardTypes);
                    } else {
                        commonTypes.retainAll(cardTypes);
                        if (commonTypes.isEmpty()) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }
    },

    YORION_SKY_NOMAD("Yorion, Sky Nomad") {
        private final int MINIMUM_DECK_SIZE = 60;

        @Override
        public boolean isLegal(Multiset<Card> mainDeck) {
            return mainDeck.size() >= MINIMUM_DECK_SIZE + 20;
            // Requires information about format to be added to signature?
        }
    },

    ZIRDA_THE_DAWNWAKER("Zirda, the Dawnwaker") {
        private boolean hasActivatedAbility(Card c) {
            if (c.getMainTypeLine().is(CardSupertype.BASIC)) return true;

            // TODO: Account for keyword activated abilities without reminder text
            // TODO: Account for activated abilities that are nested in quotation marks
            return c.getMainFace().filter(f -> f.getOracleText().contains(":")).isPresent();
        }

        @Override
        public boolean isLegal(Multiset<Card> mainDeck) {
            return allCardsAre(mainDeck, c -> !c.getMainTypeLine().isPermanentCard() || hasActivatedAbility(c));
        }
    };

    private static boolean allCardsAre(Multiset<Card> cardMultiset, Predicate<Card> predicate) {
        return cardMultiset.elementSet().stream().allMatch(predicate);
    }


    private final String cardName;

    CompanionLegality(String cardName) {
        this.cardName = cardName;
    }

    protected abstract boolean isLegal(Multiset<Card> mainDeck);

    public boolean isLegal(Deck<? extends CardIdentity> deck) {
        Multiset<Card> mainDeck = deck.get(MAIN_DECK).entrySet().stream()
                .collect(MapCollectors.<Multiset.Entry<? extends CardIdentity>>collecting()
                        .withKey(e -> e.getElement().getCard())
                        .countingBy(Multiset.Entry::getCount)
                        .toImmutableMultiset());
        return isLegal(mainDeck);
    }

    private static final ImmutableMap<String, CompanionLegality> BY_CARD_NAME = Maps.uniqueIndex(
            EnumSet.allOf(CompanionLegality.class), c -> c.cardName);

    public static <C extends CardIdentity> ImmutableSet<C> findLegalCompanions(Deck<C> deck) {
        return deck.get(SIDEBOARD).elementSet().stream()
                .filter((C card) -> {
                    CompanionLegality companionLegality = BY_CARD_NAME.get(card.getCard().getMainName());
                    return companionLegality != null && companionLegality.isLegal(deck);
                })
                .collect(ImmutableSet.toImmutableSet());
    }

    public static <C extends CardIdentity> Deck<C> extractCompanion(Deck<C> deck) {
        if (!deck.get(COMPANION).isEmpty()) return deck;
        Set<C> legalCompanions = findLegalCompanions(deck);
        if (legalCompanions.size() != 1) return deck;
        C companion = Iterables.getOnlyElement(legalCompanions);

        Deck.Builder<C> builder = deck.createMutableCopy();
        boolean removed = builder.get(SIDEBOARD).remove(companion);
        if (!removed) throw new RuntimeException();
        builder.addTo(COMPANION, companion, 1);
        return builder.build();
    }

    public static Deck<Card> addMissingCompanion(Spoiler spoiler, Deck<Card> deck, Format format) {
        return addMissingCompanion(spoiler, deck,
                (Card card) -> card.getCardLegality().isPermittedIn(Word.of(format)),
                Function.identity());
    }

    public static <V extends CardIdentity> Deck<V> addMissingCompanion(Spoiler spoiler,
                                                                       Deck<V> deck,
                                                                       Predicate<Card> condition,
                                                                       Function<Card, V> versionChoice) {
        if (!deck.get(COMPANION).isEmpty() || deck.getLegalSideboard().size() >= 15) {
            return deck;
        }
        Deck<Card> cardDeck = Deck.toCards(deck);
        ImmutableMultiset<Card> mainDeck = cardDeck.get(MAIN_DECK);
        ImmutableSet<Card> possibleCompanions = EnumSet.allOf(CompanionLegality.class).stream()
                .filter(companion -> companion.isLegal(mainDeck))
                .flatMap(companion -> spoiler.lookUpByName(companion.cardName).stream())
                .filter((Card companionCard) -> condition.test(companionCard) && cardDeck.getTotalCopiesOf(companionCard) < 4)
                .collect(ImmutableSet.toImmutableSet());

        if (possibleCompanions.size() == 1) {
            Card companionCard = Iterables.getOnlyElement(possibleCompanions);
            V companionVersion = versionChoice.apply(companionCard);
            Deck.Builder<V> mutableDeck = deck.createMutableCopy();
            mutableDeck.addTo(COMPANION, companionVersion, 1);
            return mutableDeck.build();
        }
        return deck;
    }

}
