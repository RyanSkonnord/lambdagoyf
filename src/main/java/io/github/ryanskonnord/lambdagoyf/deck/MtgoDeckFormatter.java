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
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.MoreCollectors;
import com.google.common.collect.Multiset;
import io.github.ryanskonnord.lambdagoyf.card.Card;
import io.github.ryanskonnord.lambdagoyf.card.CardEdition;
import io.github.ryanskonnord.lambdagoyf.card.Expansion;
import io.github.ryanskonnord.lambdagoyf.card.MtgoCard;
import io.github.ryanskonnord.lambdagoyf.card.Spoiler;
import io.github.ryanskonnord.lambdagoyf.card.Word;
import io.github.ryanskonnord.lambdagoyf.card.field.Finish;
import io.github.ryanskonnord.lambdagoyf.card.field.Rarity;
import io.github.ryanskonnord.util.NodeListAdapter;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.github.ryanskonnord.lambdagoyf.deck.Deck.Section.MAIN_DECK;
import static io.github.ryanskonnord.lambdagoyf.deck.Deck.Section.SIDEBOARD;
import static io.github.ryanskonnord.lambdagoyf.deck.MtgoDeck.getMtgoName;

public final class MtgoDeckFormatter {

    public static final class DeckDataException extends RuntimeException {
        private DeckDataException(String message) {
            super(message);
        }
    }

    private static final class DeckEntry<C> {
        private final C card;
        private final int quantity;
        private final boolean isInSideboard;

        private DeckEntry(C card, int quantity, boolean isInSideboard) {
            Preconditions.checkArgument(quantity >= 0);
            this.card = Objects.requireNonNull(card);
            this.quantity = quantity;
            this.isInSideboard = isInSideboard;
        }

        public void addTo(Deck.Builder<C> deckBuilder) {
            Deck.Section section = isInSideboard ? SIDEBOARD : MAIN_DECK;
            deckBuilder.addTo(section, card, quantity);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DeckEntry that = (DeckEntry) o;
            return quantity == that.quantity && isInSideboard == that.isInSideboard && card.equals(that.card);
        }

        @Override
        public int hashCode() {
            return Objects.hash(card, quantity, isInSideboard);
        }
    }

    private static <C> Deck<C> create(Collection<DeckEntry<C>> entries) {
        Deck.Builder<C> builder = new Deck.Builder<>();
        for (DeckEntry<C> entry : entries) {
            entry.addTo(builder);
        }
        return builder.build();
    }


    private static final Pattern MTGO_TXT_LINE = Pattern.compile("(\\d+)\\s+(.*)");

    public static Deck<String> parseTxt(Reader reader) throws IOException {
        Deck.Builder<String> builder = new Deck.Builder<>();
        Deck.Section currentSection = MAIN_DECK;
        try (BufferedReader bufferedReader = new BufferedReader(reader)) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                line = line.trim();
                line = CharMatcher.anyOf("\uFEFF\uFFFE").trimLeadingFrom(line);
                Optional<Deck.Section> sectionLabel = Deck.Section.fromLabel(line);
                if (line.isEmpty()) {
                    currentSection = SIDEBOARD;
                } else if (sectionLabel.isPresent()) {
                    currentSection = sectionLabel.get();
                } else {
                    Matcher matcher = MTGO_TXT_LINE.matcher(line.trim());
                    if (!matcher.matches()) throw new DeckDataException("Invalid syntax");
                    int quantity = Integer.parseInt(matcher.group(1));
                    String name = matcher.group(2);
                    builder.addTo(currentSection, name, quantity);
                }
            }
        }
        return builder.build();
    }


    private static String formatTxtLine(Multiset.Entry<Card> entry) {
        return String.format("%d %s\n", entry.getCount(), getMtgoName(entry.getElement()));
    }

    public static void writeTxt(Writer writer, Deck<Card> deck) throws IOException {
        try (BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
            for (Multiset.Entry<Card> entry : deck.get(MAIN_DECK).entrySet()) {
                bufferedWriter.write(formatTxtLine(entry));
            }

            Multiset<Card> sideboard = deck.getLegalSideboard();
            if (!sideboard.isEmpty()) {
                bufferedWriter.write("\n");
                bufferedWriter.write(SIDEBOARD.getLabel());
                bufferedWriter.write("\n");
                for (Multiset.Entry<Card> entry : sideboard.entrySet()) {
                    bufferedWriter.write(formatTxtLine(entry));
                }
            }
        } finally {
            writer.close();
        }
    }

    public static Deck<Long> parseDek(InputStream inputStream) throws IOException {
        DocumentBuilder documentBuilder;
        try {
            documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException();
        }

        Document deckFile;
        try {
            deckFile = documentBuilder.parse(inputStream);
        } catch (SAXException e) {
            throw new IOException(e);
        }
        Node deckNode = new NodeListAdapter(deckFile.getChildNodes()).stream()
                .filter(n -> n.getNodeName().equals("Deck"))
                .collect(MoreCollectors.onlyElement());
        Collection<DeckEntry<Long>> entries = new NodeListAdapter(deckNode.getChildNodes()).stream()
                .filter(n -> n.getNodeName().equals("Cards"))
                .map(n -> {
                    NamedNodeMap attributes = n.getAttributes();
                    long id = Long.parseLong(attributes.getNamedItem("CatID").getTextContent());
                    int quantity = Integer.parseInt(attributes.getNamedItem("Quantity").getTextContent());
                    boolean sideboard = Optional.ofNullable(attributes.getNamedItem("Sideboard"))
                            .map(a -> Boolean.parseBoolean(a.getTextContent())).orElse(false);
                    return new DeckEntry<>(id, quantity, sideboard);
                })
                .collect(Collectors.toList());
        return create(entries);
    }

    private static <C> Stream<DeckEntry<C>> streamPartToEntries(Multiset<C> part, boolean isSideboard) {
        return part.entrySet().stream()
                .map((Multiset.Entry<C> e) -> new DeckEntry<>(e.getElement(), e.getCount(), isSideboard));
    }

    private static Stream<DeckEntry<MtgoDeck.CardEntry>> streamEntries(Deck<MtgoDeck.CardEntry> deck) {
        Stream<DeckEntry<MtgoDeck.CardEntry>> mainDeck = streamPartToEntries(deck.get(MAIN_DECK), false);
        Stream<DeckEntry<MtgoDeck.CardEntry>> sideboard = streamPartToEntries(deck.getLegalSideboard(), true);
        return Stream.of(mainDeck, sideboard)
                .flatMap((Stream<DeckEntry<MtgoDeck.CardEntry>> part) ->
                        part.sorted(Comparator.comparing((DeckEntry<MtgoDeck.CardEntry> entry) -> entry.card.getId())));
    }

    private static String formatDekEntry(DeckEntry<MtgoDeck.CardEntry> entry) {
        return String.format("  <Cards CatID=\"%d\" Quantity=\"%d\" Sideboard=\"%s\" Name=\"%s\" />\n",
                entry.card.getId(), entry.quantity, entry.isInSideboard,
                entry.card.getName().replace("\"", "&quot;"));
    }

    public static void writeDek(OutputStream outputStream, Deck<MtgoDeck.CardEntry> deck) throws IOException {
        List<String> lines = streamEntries(deck)
                .map(MtgoDeckFormatter::formatDekEntry)
                .collect(Collectors.toList());
        try (Writer writer = new OutputStreamWriter(outputStream, Charsets.UTF_8)) {
            writer.write("" +
                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                    "<Deck xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +
                    "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                    "  <NetDeckID>0</NetDeckID>\n" +
                    "  <PreconstructedDeckID>0</PreconstructedDeckID>\n");
            for (String line : lines) {
                writer.write(line);
            }
            writer.write("</Deck>");
        }
    }

    public static final class MtgoCsvEntry {
        public final String name;
        public final int quantity;
        public final long id;
        public final String rarity;
        public final String expansion;
        public final String collectorNumber;
        public final boolean premium;
        public final boolean sideboarded;
        public final Optional<String> annotation;

        private MtgoCsvEntry(String name, List<String> cells) {
            this.name = name;
            quantity = Integer.parseInt(cells.get(0));
            id = Long.parseLong(cells.get(1));
            rarity = cells.get(2);
            expansion = cells.get(3);
            collectorNumber = cells.get(4);
            premium = cells.size() > 5 && parseCsvBoolean(cells.get(5));
            sideboarded = cells.size() > 6 && parseCsvBoolean(cells.get(6));
            annotation = cells.size() > 7 ? Optional.of(cells.get(7)) : Optional.empty();
        }

        public long getId() {
            return id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MtgoCsvEntry)) return false;
            MtgoCsvEntry that = (MtgoCsvEntry) o;
            return quantity == that.quantity && id == that.id && premium == that.premium && sideboarded == that.sideboarded
                    && name.equals(that.name) && rarity.equals(that.rarity) && expansion.equals(that.expansion)
                    && collectorNumber.equals(that.collectorNumber) && annotation.equals(that.annotation);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, quantity, id, rarity, expansion, collectorNumber, premium, sideboarded, annotation);
        }
    }

    /*
     * Can't use a real CSV parser because of the bugged way that MTGO handles 'Kongming, "Sleeping Dragon"'.
     */
    private static final Pattern MTGO_CSV_NAME = Pattern.compile("^\"(.*?)\",");
    private static final Splitter COMMA_SPLITTER = Splitter.on(',');

    public static ImmutableList<MtgoCsvEntry> parseCsvToRaw(Reader reader) throws IOException {
        try (BufferedReader bufferedReader = new BufferedReader(reader)) {
            ImmutableList.Builder<MtgoCsvEntry> entries = ImmutableList.builder();
            String topLine = bufferedReader.readLine();

            String nextLine;
            while ((nextLine = bufferedReader.readLine()) != null) {
                if (nextLine.isEmpty()) continue;
                Matcher matcher = MTGO_CSV_NAME.matcher(nextLine);
                if (!matcher.find()) throw new DeckDataException("CSV syntax: " + nextLine);
                List<String> rest = COMMA_SPLITTER.splitToList(nextLine.substring(matcher.end()));
                entries.add(new MtgoCsvEntry(matcher.group(1), rest));
            }
            return entries.build();
        } finally {
            reader.close();
        }
    }

    public static MtgoDeck parseCsv(Spoiler spoiler, Reader reader) throws IOException {
        MtgoDeck.Builder builder = new MtgoDeck.Builder();
        for (MtgoCsvEntry entry : parseCsvToRaw(reader)) {
            builder.add(spoiler, entry.name, entry.id, entry.quantity, entry.sideboarded);
        }
        return builder.build();
    }

    private static boolean parseCsvBoolean(String value) {
        return switch (value) {
            case "Yes" -> true;
            case "No" -> false;
            default -> throw new DeckDataException("Invalid CSV boolean: " + value);
        };
    }

    private static String formatCsvBoolean(boolean value) {
        return value ? "Yes" : "No";
    }

    public static void writeCsv(Writer writer, Deck<MtgoCard> deck) throws IOException {
        boolean hasSideboard = !deck.getLegalSideboard().isEmpty();
        Deck<MtgoDeck.CardEntry> transform = deck.transform(MtgoDeck.CardEntry::new);
        List<String> lines = streamEntries(transform)
                .map((DeckEntry<MtgoDeck.CardEntry> entry) -> {
                    StringBuilder builder = new StringBuilder(70);
                    MtgoCard card = entry.card.getVersion()
                            .orElseThrow(RuntimeException::new); // should be impossible because we made it from MtgoCards ourselves
                    CardEdition cardEdition = card.getEdition();
                    Expansion expansion = cardEdition.getExpansion();

                    builder.append('"').append(getMtgoName(cardEdition.getCard())).append('"')
                            .append(',').append(entry.quantity)
                            .append(',').append(card.getMtgoId())
                            .append(',').append(formatRarity(cardEdition.getRarity()))
                            .append(',').append(expansion.getMtgoCode().orElse(expansion.getProductCode()))
                            .append(',').append(cardEdition.getCollectorNumber().getNumber())
                            .append('/').append(expansion.getCardCount())
                            .append(',').append(formatCsvBoolean(card.getFinish() == Finish.FOIL));
                    if (hasSideboard) {
                        builder.append(',').append(formatCsvBoolean(entry.isInSideboard));
                    }
                    return builder.append('\n').toString();
                })
                .collect(Collectors.toList());
        try (BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
            bufferedWriter.write("Card Name,Quantity,ID #,Rarity,Set,Collector #,Premium,");
            if (hasSideboard) {
                bufferedWriter.write("Sideboarded,");
            }
            bufferedWriter.write("\n");
            for (String line : lines) {
                bufferedWriter.write(line);
            }
        }
    }

    private static String formatRarity(Word<Rarity> rarity) {
        String key = rarity.getKey();
        return Character.toUpperCase(key.charAt(0)) + key.substring(1).toLowerCase();
    }


    public static Deck<Card> createDeckFromCardNames(Spoiler spoiler, Deck<String> cardNames) {
        Set<String> missingNames = Collections.synchronizedSet(new TreeSet<>());
        Deck<Card> deck = createDeckFromCardNames(spoiler, cardNames, missingNames::add);
        if (missingNames.isEmpty()) {
            return deck;
        } else {
            throw new DeckDataException("Unrecognized card names: " + missingNames);
        }
    }

    public static Deck<Card> createDeckFromCardNames(Spoiler spoiler,
                                                     Deck<String> cardNames,
                                                     Consumer<? super String> missingNameHandler) {
        return cardNames.flatTransform((String name) -> {
            Optional<Card> card = spoiler.lookUpByName(name);
            if (!card.isPresent()) {
                name = name.replace('’', '\'');
                card = spoiler.lookUpByName(name);
            }
            if (!card.isPresent()) {
                int parenIndex = name.indexOf('(');
                if (parenIndex > 0) {
                    name = name.substring(0, parenIndex).trim();
                    card = spoiler.lookUpByName(name);
                }
            }
            if (!card.isPresent()) {
                missingNameHandler.accept(name);
            }
            return card;
        });
    }

}
