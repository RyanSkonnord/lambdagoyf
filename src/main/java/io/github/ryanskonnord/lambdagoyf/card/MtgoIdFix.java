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

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.MoreCollectors;
import com.google.common.collect.Multimaps;
import io.github.ryanskonnord.lambdagoyf.Environment;
import io.github.ryanskonnord.lambdagoyf.card.field.Finish;
import io.github.ryanskonnord.lambdagoyf.deck.MtgoDeck;
import io.github.ryanskonnord.lambdagoyf.deck.MtgoDeckFormatter;
import io.github.ryanskonnord.lambdagoyf.scryfall.ScryfallCardEntry;
import io.github.ryanskonnord.lambdagoyf.scryfall.ScryfallParser;
import io.github.ryanskonnord.util.MapCollectors;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public final class MtgoIdFix implements Comparable<MtgoIdFix> {

    private final Optional<String> source;
    private final Long normalId;
    private final Long foilId;

    MtgoIdFix(String source, Long normalId, Long foilId) {
        this.source = Optional.ofNullable(source);
        this.normalId = normalId;
        this.foilId = foilId;
    }

    public Optional<String> getSource() {
        return source;
    }

    public OptionalLong getNormalId() {
        return normalId == null ? OptionalLong.empty() : OptionalLong.of(normalId);
    }

    public OptionalLong getFoilId() {
        return foilId == null ? OptionalLong.empty() : OptionalLong.of(foilId);
    }

    private static final Comparator<MtgoIdFix> COMPARATOR = Comparator.nullsFirst(Comparator.comparing((MtgoIdFix f) -> f.normalId))
            .thenComparing(Comparator.nullsFirst(Comparator.comparing((MtgoIdFix f) -> f.foilId)));

    @Override
    public int compareTo(MtgoIdFix that) {
        return COMPARATOR.compare(this, that);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MtgoIdFix)) return false;
        MtgoIdFix mtgoIdFix = (MtgoIdFix) o;
        if (normalId != null ? !normalId.equals(mtgoIdFix.normalId) : mtgoIdFix.normalId != null) {
            return false;
        }
        return foilId != null ? foilId.equals(mtgoIdFix.foilId) : mtgoIdFix.foilId == null;
    }

    @Override
    public int hashCode() {
        int result = normalId != null ? normalId.hashCode() : 0;
        result = 31 * result + (foilId != null ? foilId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("normalId", normalId)
                .add("foilId", foilId)
                .toString();
    }


    private static String trimSlash(String collectorStr) {
        int slashIndex = collectorStr.indexOf("/");
        return slashIndex < 0 ? collectorStr : collectorStr.substring(0, slashIndex);
    }

    private static final class MtgoCsvKey {
        private final String name;
        private final String expansionCode;
        private final String collector;

        MtgoCsvKey(String name, String expansionCode, String collector) {
            this.name = Objects.requireNonNull(name);
            this.expansionCode = expansionCode.equals("SL2") ? "SLD" : expansionCode;
            this.collector = Objects.requireNonNull(collector);
        }

        MtgoCsvKey(CardEdition edition) {
            this(
                    edition.getCard().getMainName(),
                    edition.getExpansion().getProductCode(),
                    edition.getCollectorNumber().toString());
        }

        private static String trimSlash(String collectorStr) {
            int slashIndex = collectorStr.indexOf("/");
            return slashIndex < 0 ? collectorStr : collectorStr.substring(0, slashIndex);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MtgoCsvKey)) return false;
            MtgoCsvKey that = (MtgoCsvKey) o;
            return name.equals(that.name) && expansionCode.equals(that.expansionCode) && collector.equals(that.collector);
        }

        @Override
        public int hashCode() {
            return 31 * (31 * name.hashCode() + expansionCode.hashCode()) + collector.hashCode();
        }
    }


    public static Registry loadFromResources() {
        Map<UUID, MtgoIdFix> fixes = ResourceLoader.readAllYamlResources("/fix/mtgo")
                .flatMap((ResourceLoader.NamedYamlResource namedResource) -> {
                    String source = namedResource.getResourceName();
                    Collection<?> fixEntries = (Collection<?>) namedResource.getResource();
                    return fixEntries.stream().map((Object entry) -> {
                        Map<?, ?> map = (Map<?, ?>) entry;
                        UUID id = UUID.fromString((String) map.get("scryfallId"));
                        Long normalId = toNullableLong((Number) map.get("normalId"));
                        Long foilId = toNullableLong((Number) map.get("foilId"));
                        return Maps.immutableEntry(id, new MtgoIdFix(source, normalId, foilId));
                    });
                })
                .sorted(Map.Entry.comparingByValue())
                .collect(MapCollectors.<UUID, MtgoIdFix>collectingEntries().unique().toImmutableMap());
        return new Registry(fixes);
    }

    private static Long toNullableLong(Number number) {
        return (number == null) ? null : number.longValue();
    }

    public static final class Registry {
        private final ImmutableMap<UUID, MtgoIdFix> fixes;

        private Registry(Map<UUID, MtgoIdFix> fixes) {
            this.fixes = ImmutableMap.copyOf(fixes);
        }

        private static final MtgoIdFix EMPTY = new MtgoIdFix(null, null, null);
        private static final ImmutableSet<String> ICE_AGE_BLOCK_EXPANSION_NAMES = ImmutableSet.of("ice", "all");

        public Optional<MtgoIdFix> getFix(ScryfallCardEntry scryfallCardEntry) {
            if (ICE_AGE_BLOCK_EXPANSION_NAMES.contains(scryfallCardEntry.getSet())) {
                return Optional.of(EMPTY);
            }
            return Optional.ofNullable(fixes.get(scryfallCardEntry.getId()));
        }
    }

    /**
     * Internal utilities for generating updates to the YAML resources. Useful for calling in one-off programs from the
     * main method.
     */
    private static class MaintenanceOperation {
        private final Spoiler spoiler;
        private final Predicate<? super CardEdition> editionFilter;
        private final UnaryOperator<String> collectorTransformation;

        public MaintenanceOperation(Spoiler spoiler,
                                    Predicate<? super CardEdition> editionFilter,
                                    UnaryOperator<String> collectorTransformation) {
            this.spoiler = Objects.requireNonNull(spoiler);
            this.editionFilter = Optional.ofNullable(editionFilter).orElse(x -> true);
            this.collectorTransformation = Optional.ofNullable(collectorTransformation).orElse(MtgoIdFix::trimSlash);
        }

        public void produce(
                Reader mtgoCsvExport,
                PrintWriter out)
                throws IOException {
            Collection<MtgoDeckFormatter.MtgoCsvEntry> csvEntries = MtgoDeckFormatter.parseCsvToRaw(mtgoCsvExport);
            Map<MtgoCsvKey, Long> normals = getKeysFromCsvEntries(csvEntries, false);
            Map<MtgoCsvKey, Long> foils = getKeysFromCsvEntries(csvEntries, true);

            List<CardEdition> editions = spoiler.getCards().stream().flatMap(c -> c.getEditions().stream())
                    .filter(editionFilter)
                    .filter((CardEdition cardEdition) -> {
                        MtgoCsvKey key = new MtgoCsvKey(cardEdition);
                        Long nonfoilFix = normals.get(key);
                        Long foilFix = foils.get(key);
                        return (nonfoilFix != null || foilFix != null) && !isFixRedundant(cardEdition, nonfoilFix, foilFix);
                    })
                    .sorted()
                    .collect(Collectors.toList());

            Set<Card> duplicateNames = Multimaps.index(editions, CardEdition::getCard)
                    .asMap().entrySet().stream()
                    .filter((Map.Entry<Card, Collection<CardEdition>> entry) -> entry.getValue().size() > 1)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());

            Iterator<CardEdition> iterator = editions.iterator();
            while (iterator.hasNext()) {
                CardEdition cardEdition = iterator.next();

                String comment = cardEdition.toString();
                if (duplicateNames.contains(cardEdition.getCard())) {
                    comment += " by " + Joiner.on(" & ").join(cardEdition.getArtists().iterator());
                }
                out.println("- # " + comment);

                MtgoCsvKey key = new MtgoCsvKey(cardEdition);
                out.println("  scryfallId: " + cardEdition.getScryfallId().toString());
                out.println("  normalId:   " + normals.get(key));
                out.println("  foilId:     " + foils.get(key));
                if (iterator.hasNext()) {
                    out.println();
                }
            }
        }

        private boolean isFixRedundant(CardEdition cardEdition, Long nonfoilFix, Long foilFix) {
            Optional<Long> nonfoilId = cardEdition.getMtgoCard(Finish.NONFOIL).map(MtgoCard::getMtgoId);
            Optional<Long> foilId = cardEdition.getMtgoCard(Finish.FOIL).map(MtgoCard::getMtgoId);
            if (foilId.isEmpty()) {
                foilId = nonfoilId.map(id -> id + 1);
            }
            boolean result = Objects.equals(nonfoilFix, nonfoilId.orElse(null)) && Objects.equals(foilFix, foilId.orElse(null));
            if (result) {
                System.out.println(String.format("Skipping redundant fix: " + cardEdition));
            }
            return result;
        }

        private ImmutableMap<MtgoCsvKey, Long> getKeysFromCsvEntries(Collection<MtgoDeckFormatter.MtgoCsvEntry> csvEntries, boolean getPremium) {
            Predicate<MtgoDeckFormatter.MtgoCsvEntry> predicate = e -> getPremium == e.premium;
            return csvEntries.stream()
                    .filter(predicate)
                    .sorted(Comparator.comparing(entry -> CollectorNumber.parse(entry.collectorNumber)))
                    .collect(MapCollectors.<MtgoDeckFormatter.MtgoCsvEntry>collecting()
                            .withKey(entry -> new MtgoCsvKey(entry.name, entry.expansion, collectorTransformation.apply(entry.collectorNumber)))
                            .withValue(MtgoDeckFormatter.MtgoCsvEntry::getId)
                            .unique().toImmutableMap());
        }
    }

    private static Expansion readExpansionOfSetDump(Spoiler spoiler, Path setDump) throws IOException {
        try (Reader reader = Files.newBufferedReader(setDump)) {
            return MtgoDeckFormatter.parseCsv(spoiler, reader).asDeckObject()
                    .transform(MtgoDeck.CardEntry::getVersion)
                    .getAllCards().elementSet().stream()
                    .flatMap(Optional::stream)
                    .map(c -> c.getEdition().getExpansion())
                    .distinct()
                    .collect(MoreCollectors.onlyElement());
        }
    }

    public static void main(String[] args) throws Exception {
        Spoiler spoiler = ScryfallParser.createSpoiler();
        Path setDirPath = Environment.getDeckFilePath().resolve("Sets");
        for (Path setDump : Files.list(setDirPath).collect(Collectors.toList())) {
            Expansion expansion = readExpansionOfSetDump(spoiler, setDump);
            Path writePath = Environment.getInternalResourcePath().resolve(String.format("fix/mtgo/%s.yaml",
                    expansion.getName().replace(":", "")));
            try (Reader reader = Files.newBufferedReader(setDump);
                 Writer writer = Files.newBufferedWriter(writePath)) {
                MaintenanceOperation op = new MaintenanceOperation(spoiler,
                        null,
                        n -> Integer.toString(CollectorNumber.parse(trimSlash(n)).getNumber()));
                op.produce(reader, new PrintWriter(writer));
            }
        }
    }

}
