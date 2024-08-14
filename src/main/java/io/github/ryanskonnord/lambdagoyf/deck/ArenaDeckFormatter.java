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

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import io.github.ryanskonnord.lambdagoyf.card.ArenaCard;
import io.github.ryanskonnord.lambdagoyf.card.Card;
import io.github.ryanskonnord.lambdagoyf.card.CardEdition;
import io.github.ryanskonnord.lambdagoyf.card.ColorSet;
import io.github.ryanskonnord.lambdagoyf.card.Spoiler;
import io.github.ryanskonnord.lambdagoyf.card.field.CardType;
import io.github.ryanskonnord.util.OrderingUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ArenaDeckFormatter {

    private static boolean isLand(Card card) {
        return card.getMainTypeLine().is(CardType.LAND);
    }

    private static ColorSet getDeckBuilderColorIdentity(Card card) {
        ColorSet colors = card.getColors();
        return colors.isEmpty() ? card.getColorIdentity() : colors;
    }

    public static final Comparator<Card> ARENA_DECK_BUILDER_ORDER = Comparator
            .comparing(ArenaDeckFormatter::isLand)
            .thenComparing(Card::getCmc)
            .thenComparing(ArenaDeckFormatter::getDeckBuilderColorIdentity, ColorSet.COLORLESS_LAST)
            .thenComparing(Card::getMainName);

    public static final Comparator<CardEdition> ARENA_COLLECTION_VIEW_ORDER = Comparator
            .comparing(CardEdition::getCard,
                    Comparator.comparing(ArenaDeckFormatter::getDeckBuilderColorIdentity, ColorSet.COLORLESS_LAST)
                            .thenComparing(ArenaDeckFormatter::isLand)
                            .thenComparing(Card::getCmc)
                            .thenComparing(Card::getMainName))
            .thenComparing(Comparator.naturalOrder());

    public static Comparator<ArenaDeckEntry> orderArenaCards() {
        Comparator<ArenaCard> arenaCardOrder = Comparator.comparing((ArenaCard c) -> c.getEdition().getCard(), ARENA_DECK_BUILDER_ORDER)
                .thenComparing(Comparator.naturalOrder());
        return OrderingUtil.OptionalComparator.build(ArenaDeckEntry::getVersion, arenaCardOrder).emptyKeysLast();
    }

    private static boolean isSignificantFromSideboard(Card card) {
        if (card.getMainTypeLine().isSubtype("Lesson")) return true;
        String legionAngelText = "named " + card.getMainName() + " from outside the game";
        return card.getFaces().stream().anyMatch(face -> face.getOracleText().contains(legionAngelText));
    }

    public static Deck<ArenaDeckEntry> prioritizeBestOfOneSideboard(Deck<ArenaDeckEntry> deck) {
        boolean hasGrizzledHuntmaster = deck.get(Deck.Section.MAIN_DECK).elementSet().stream()
                .anyMatch(e -> e.getCardName().equals("Grizzled Huntmaster"));
        Predicate<Card> significance = ((Predicate<Card>) ArenaDeckFormatter::isSignificantFromSideboard)
                .or(c -> hasGrizzledHuntmaster && c.getMainTypeLine().is(CardType.CREATURE));

        Multiset<ArenaDeckEntry> oldSideboard = deck.get(Deck.Section.SIDEBOARD);
        Multiset<ArenaDeckEntry> significantFromSideboard = Multisets.filter(oldSideboard,
                e -> e.getVersion().filter(v -> significance.test(v.getCard())).isPresent());
        int size = significantFromSideboard.size();
        int bo1Capacity = BO1_SIDEBOARD_SIZE - deck.get(Deck.Section.COMPANION).size();
        if (size != 0 && size <= bo1Capacity && size != oldSideboard.size()) {
            Set<ArenaDeckEntry> significantCards = ImmutableSet.copyOf(significantFromSideboard.elementSet());
            return changeSideboardOrder(deck, Comparator.comparing(e -> !significantCards.contains(e.getElement())));
        }
        return deck;
    }

    private static <C> Deck<C> changeSideboardOrder(Deck<C> deck, Comparator<Multiset.Entry<C>> priority) {
        Deck.Builder<C> mutableCopy = deck.createMutableCopy();
        Multiset<C> newSideboard = mutableCopy.get(Deck.Section.SIDEBOARD);
        newSideboard.clear();
        deck.get(Deck.Section.SIDEBOARD).entrySet().stream()
                .sorted(priority)
                .forEachOrdered(e -> newSideboard.add(e.getElement(), e.getCount()));
        return mutableCopy.build();
    }

    private static final int BO1_SIDEBOARD_SIZE = 7;


    private static void writePart(PrintWriter printWriter, Multiset<ArenaDeckEntry> part) {
        for (Multiset.Entry<ArenaDeckEntry> entry : part.entrySet()) {
            printWriter.print(entry.getCount());
            printWriter.print(' ');
            printWriter.println(entry.getElement());
        }
    }

    public static void write(Writer writer, Deck<ArenaDeckEntry> deck) {
        PrintWriter printWriter = new PrintWriter(writer);
        Iterator<Map.Entry<Deck.Section, ImmutableMultiset<ArenaDeckEntry>>> sectionIterator = deck.getAllSections().iterator();
        while (sectionIterator.hasNext()) {
            Map.Entry<Deck.Section, ImmutableMultiset<ArenaDeckEntry>> entry = sectionIterator.next();
            printWriter.println(entry.getKey().getLabel());
            writePart(printWriter, entry.getValue());
            if (sectionIterator.hasNext()) {
                printWriter.println();
            }
        }
    }

    private static final Pattern DECK_ENTRY_PATTERN = Pattern.compile("(\\d+)\\s+(.*)");

    public static Deck<ArenaDeckEntry> readEntries(Reader reader) throws IOException {
        Deck.Builder<ArenaDeckEntry> builder = new Deck.Builder<>();
        Deck.Section currentSection = Deck.Section.MAIN_DECK;
        try (BufferedReader bufferedReader = new BufferedReader(reader)) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                line = CharMatcher.whitespace().trimTrailingFrom(line);
                line = CharMatcher.anyOf("\uFEFF\uFFFE").trimLeadingFrom(line);
                if (line.isEmpty()) {
                    currentSection = Deck.Section.SIDEBOARD;
                    continue;
                }
                Optional<Deck.Section> sectionLabel = Deck.Section.fromLabel(line);
                if (sectionLabel.isPresent()) {
                    currentSection = sectionLabel.get();
                    continue;
                }
                Matcher matcher = DECK_ENTRY_PATTERN.matcher(line);
                if (!matcher.matches()) {
                    throw new DeckDataException("Invalid syntax: " + line);
                }
                int count = Integer.parseInt(matcher.group(1));
                String entryStr = matcher.group(2);
                ArenaDeckEntry entryObj = ArenaDeckEntry.parse(entryStr);
                builder.addTo(currentSection, entryObj, count);
            }
        }
        return builder.build();
    }

    public static Deck<Card> readDeck(Spoiler spoiler, Reader reader) throws IOException {
        return readEntries(reader).transform(entry -> spoiler.lookUpByName(entry.getCardName())
                .orElseThrow(() -> new DeckDataException("No card found with name: " + entry)));
    }

    public static final class DeckDataException extends RuntimeException {
        private DeckDataException(String message) {
            super(message);
        }
    }

}
