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

import au.com.bytecode.opencsv.CSVReader;
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.github.ryanskonnord.lambdagoyf.Environment;
import io.github.ryanskonnord.lambdagoyf.card.field.CardSupertype;
import io.github.ryanskonnord.lambdagoyf.deck.ArenaDeckEntry;
import io.github.ryanskonnord.lambdagoyf.deck.ArenaDeckFormatter;
import io.github.ryanskonnord.lambdagoyf.deck.ArenaVersionId;
import io.github.ryanskonnord.lambdagoyf.deck.Deck;
import io.github.ryanskonnord.lambdagoyf.scryfall.ScryfallParser;
import io.github.ryanskonnord.util.MapCollectors;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.github.ryanskonnord.lambdagoyf.deck.ArenaDeckFormatter.ARENA_COLLECTION_VIEW_ORDER;

public abstract class ArenaIdFix {

    public static final class OverrideVersionId extends ArenaIdFix {
        private final ArenaVersionId replacement;

        private OverrideVersionId(String expansionCode, int collectorNumber) {
            this.replacement = new ArenaVersionId(expansionCode,collectorNumber);
        }

        @Override
        public Optional<ArenaCard> setUpArenaCard(CardEdition edition, OptionalLong arenaId) {
            return Optional.of(new ArenaCard(edition, arenaId, replacement));
        }

        @Override
        protected void printAsYaml(PrintWriter printWriter) {
            printWriter.println("  expansion:  " + replacement.getExpansionCode());
            printWriter.println("  number:     " + replacement.getCollectorNumber());
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("expansionCode", replacement.getExpansionCode())
                    .add("collectorNumber", replacement.getCollectorNumber())
                    .toString();
        }
    }

    private static final class OverrideExistence extends ArenaIdFix {
        private final boolean exists;

        private OverrideExistence(boolean exists) {
            this.exists = exists;
        }

        @Override
        public Optional<ArenaCard> setUpArenaCard(CardEdition edition, OptionalLong arenaId) {
            return exists
                    ? Optional.of(new ArenaCard(edition, arenaId))
                    : Optional.empty();
        }

        @Override
        protected void printAsYaml(PrintWriter printWriter) {
            printWriter.println("  exists: " + exists);

        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("exists", exists)
                    .toString();
        }

        private static final ArenaIdFix FORCE_EXISTENCE = new OverrideExistence(true);
        private static final ArenaIdFix FORCE_NONEXISTENCE = new OverrideExistence(false);

        public static ArenaIdFix of(boolean value) {
            return value ? FORCE_EXISTENCE : FORCE_NONEXISTENCE;
        }
    }

    public abstract Optional<ArenaCard> setUpArenaCard(CardEdition edition, OptionalLong arenaId);

    protected abstract void printAsYaml(PrintWriter printWriter);


    private static Map.Entry<UUID, ArenaIdFix> readFromYaml(Map<?, ?> map) {
        UUID id = UUID.fromString((String) map.get("scryfallId"));
        ArenaIdFix fix;

        String expansion = ((String) map.get("expansion"));
        Number number = ((Number) map.get("number"));
        if (expansion == null && number == null) {
            Boolean exists = ((Boolean) map.get("exists"));
            if (exists == null) {
                throw new RuntimeException("data missing");
            } else {
                fix = OverrideExistence.of(exists);
            }
        } else if (expansion == null) {
            throw new RuntimeException("expansion missing");
        } else if (number == null) {
            throw new RuntimeException("number missing");
        } else {
            fix = new OverrideVersionId(expansion, number.intValue());
        }

        return Maps.immutableEntry(id, fix);
    }

    public static Registry loadFromResources() {
        Map<UUID, ArenaIdFix> fixes = Collections.synchronizedMap(new TreeMap<>());
        ResourceLoader.combineAllYamlLists("/fix/arena")
                .forEach((Object value) -> {
                    Map.Entry<UUID, ArenaIdFix> entry = readFromYaml((Map<?, ?>) value);
                    fixes.put(entry.getKey(), entry.getValue());
                });
        return new Registry(fixes);
    }

    public static final class Registry {
        private final ImmutableMap<UUID, ArenaIdFix> fixes;

        private Registry(Map<UUID, ArenaIdFix> fixes) {
            this.fixes = ImmutableMap.copyOf(fixes);
        }

        public Optional<ArenaIdFix> getFix(CardEdition edition) {
            return Optional.ofNullable(fixes.get(edition.getScryfallId()));
        }

    }


    /**
     * Internal utilities for generating updates to the YAML resources. Useful for calling in one-off programs from the
     * main method.
     */
    private static enum Maintenance {
        ;

        private static void printYaml(PrintWriter printWriter, Map<CardEdition, ArenaIdFix> fixes, boolean includeArtists) {
            Iterator<Map.Entry<CardEdition, ArenaIdFix>> entryIterator = fixes.entrySet().iterator();
            while (entryIterator.hasNext()) {
                Map.Entry<CardEdition, ArenaIdFix> entry = entryIterator.next();
                CardEdition edition = entry.getKey();
                ArenaIdFix fix = entry.getValue();

                String itemStart = "- # " + edition;
                if (includeArtists) {
                    itemStart += " by " + Joiner.on(" & ").join(edition.getArtists().iterator());
                }
                printWriter.println(itemStart);

                printWriter.println("  scryfallId: " + edition.getScryfallId());
                fix.printAsYaml(printWriter);
                if (entryIterator.hasNext()) {
                    printWriter.println();
                }
            }
        }

        private static Map<CardEdition, ArenaIdFix> recoverYaml(Spoiler spoiler, String resourceName) {
            ImmutableMap<UUID, CardEdition> allEditions = Maps.uniqueIndex(CardVersionExtractor.getCardEditions().getAll(spoiler).iterator(), CardEdition::getScryfallId);

            Map<CardEdition, ArenaIdFix> fixes = new LinkedHashMap<>();
            List<?> values = (List<?>) ResourceLoader.readYamlResource(resourceName);
            for (Object value : values) {
                Map.Entry<UUID, ArenaIdFix> entry = readFromYaml((Map<?, ?>) value);
                UUID scryfallId = entry.getKey();
                CardEdition edition = allEditions.get(scryfallId);
                fixes.put(edition, entry.getValue());
            }

            return fixes;
        }

        private static void mergeWithNewEntries(Spoiler spoiler,
                                                String existingResourceName,
                                                Predicate<CardEdition> newEntryCondition,
                                                Function<CardEdition, ArenaIdFix> fixFunction,
                                                PrintWriter output,
                                                boolean includeArtists) {
            Map<CardEdition, ArenaIdFix> existingEntries = recoverYaml(spoiler, existingResourceName);
            Map<CardEdition, ArenaIdFix> newEntries = CardVersionExtractor.getCardEditions().getAll(spoiler)
                    .filter(newEntryCondition)
                    .collect(MapCollectors.<CardEdition>collecting().memoizing(fixFunction).unique().toImmutableMap());

            Map<CardEdition, ArenaIdFix> merged = new TreeMap<>();
            merged.putAll(existingEntries);
            merged.putAll(newEntries);
            printYaml(output, merged, includeArtists);
        }

        private static Map<CardEdition, ArenaIdFix> buildAnthologyFixes(Spoiler spoiler, String expansionName, String deckExport) {
            Deck<ArenaDeckEntry> entries;
            try {
                entries = ArenaDeckFormatter.readEntries(new StringReader(deckExport));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return entries.getAllCards().elementSet().stream()
                    .map((ArenaDeckEntry entry) -> {
                        Card card = spoiler.lookUpByName(entry.getCardName())
                                .orElseThrow(() -> new RuntimeException("Unrecognized name: " + entry.getCardName()));
                        if (card.getMainTypeLine().is(CardSupertype.BASIC)) {
                            return Optional.<Map.Entry<CardEdition, ArenaIdFix>>empty();
                        }
                        CardEdition anthologyEdition = card.getEditions().stream().filter(e -> e.getExpansion().isNamed(expansionName))
                                .min(Comparator.comparing(CardEdition::getCollectorNumber))
                                .orElseThrow(() -> {
                                    return new RuntimeException(String.format("Did not find %s edition for %s", expansionName, card.getMainName()));
                                });
                        ArenaVersionId versionId = entry.getVersionId().orElseThrow(() -> new RuntimeException("Must parse a versioned deck file"));
                        ArenaIdFix fix = new OverrideVersionId(versionId.getExpansionCode(), versionId.getCollectorNumber());
                        return Optional.of(Maps.immutableEntry(anthologyEdition, fix));
                    })
                    .flatMap(Optional::stream)
                    .sorted(Map.Entry.comparingByKey(ARENA_COLLECTION_VIEW_ORDER))
                    .collect(MapCollectors.<CardEdition, ArenaIdFix>collectingEntries().unique().toImmutableMap());
        }

        private static Map<CardEdition, ArenaIdFix> buildEuroApacFixes(Spoiler spoiler) throws Exception {
            Path path = Environment.getInternalResourcePath().resolve("MTGA Euro and APAC lands.csv");
            List<String[]> lines;
            try (Reader reader = Files.newBufferedReader(path)) {
                CSVReader csvReader = new CSVReader(reader);
                lines = csvReader.readAll();
            }

            Map<ArenaIdFix, CardEdition> cards = CardVersionExtractor.getCardEditions().getAll(spoiler)
                    .filter(e -> {
                        Expansion expansion = e.getExpansion();
                        return expansion.isNamed("PELP") || expansion.isNamed("PALP");
                    })
                    .collect(MapCollectors.<CardEdition>collecting()
                            .indexing(edition -> (ArenaIdFix) new OverrideVersionId(edition.getExpansion().getProductCode(), edition.getCollectorNumber().getNumber()))
                            .unique().toImmutableMap());

            Map<CardEdition, ArenaIdFix> fixes = new TreeMap<>();
            for (String[] line : lines.subList(1, lines.size())) {
                String name = line[0];
                String anaNumber = line[1];
                String artist = line[2];
                String origSet = line[3];
                String origNum = line[4];

                CardEdition edition = cards.get(new OverrideVersionId(origSet, Integer.parseInt(origNum)));
                List<String> editionArtists = edition.getArtists().collect(Collectors.toList());
                if (!edition.getCard().getMainName().equals(name)) {
                    throw new RuntimeException("Name mismatch");
                }
                if (editionArtists.size() != 1 || !editionArtists.get(0).equals(artist)) {
                    throw new RuntimeException("Artist mismatch");
                }
                fixes.put(edition, new OverrideVersionId("ANA", Integer.parseInt(anaNumber)));
            }

            return fixes;
        }
    }


    public static void main(String[] args) throws Exception {
        Spoiler spoiler = ScryfallParser.createSpoiler();
//        Map<CardEdition, ArenaIdFix> fixes = CardVersionExtractor.getCardEditions().getAll(spoiler)
//                .filter(e -> e.getCard().getMainTypeLine().is(BASIC) && e.getExpansion().isNamed("SLD") && e.getCard().getMainTypeLine().is(BASIC) && e.getCollectorNumber().getNumber() >= 325)
//                .collect(MapCollectors.<CardEdition>collecting().memoizing(e -> (ArenaIdFix) new OverrideExistence(true)).unique().toImmutableMap());
        String expansionName = "Historic Anthology 7";
        Map<CardEdition, ArenaIdFix> fixes = Maintenance.buildAnthologyFixes(
                spoiler,
                expansionName,
                "Deck\n" +
                        "1 Giver of Runes (MH1) 13\n" +
                        "3 Mountain (LTR) 279\n" +
                        "1 Sun Titan (M11) 35\n" +
                        "1 Vendilion Clique (A25) 76\n" +
                        "5 Forest (LTR) 281\n" +
                        "1 Frost Titan (M11) 55\n" +
                        "1 Repeal (IMA) 70\n" +
                        "1 Unearth (2X2) 96\n" +
                        "4 Swamp (LTR) 277\n" +
                        "1 Bloodghast (IMA) 82\n" +
                        "1 Echoing Decay (DST) 41\n" +
                        "1 Grave Titan (M11) 97\n" +
                        "1 Tribal Flames (MMA) 138\n" +
                        "4 Island (LTR) 275\n" +
                        "1 Inferno Titan (M11) 146\n" +
                        "1 Wild Nacatl (ALA) 152\n" +
                        "3 Plains (LTR) 273\n" +
                        "1 Acidic Slime (M12) 161\n" +
                        "1 Primeval Titan (M11) 192\n" +
                        "1 Tooth and Nail (MMA) 170\n" +
                        "1 Silent Clearing (MH1) 246\n" +
                        "1 Fiery Islet (MH1) 238\n" +
                        "1 Nurturing Peatland (MH1) 243\n" +
                        "1 Bloodbraid Elf (2X2) 184\n" +
                        "1 Sunbaked Canyon (MH1) 247\n" +
                        "1 Waterlogged Grove (MH1) 249\n" +
                        "1 Wayfarer's Bauble (CM2) 229\n" +
                        "1 Mortarpod (MBS) 115\n" +
                        "1 Sword of Fire and Ice (DST) 148\n" +
                        "1 Worn Powerstone (USG) 318\n");

        Path path = Environment.getInternalResourcePath().resolve(String.format("fix/arena/%s.yaml", expansionName));
        try (Writer writer = new FileWriter(path.toFile())) {
            Maintenance.printYaml(
                    new PrintWriter(writer),
                    fixes,
                    true);
        }
    }

}
