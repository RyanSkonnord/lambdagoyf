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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.MoreCollectors;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import io.github.ryanskonnord.lambdagoyf.Environment;
import io.github.ryanskonnord.lambdagoyf.card.AlchemyConverter;
import io.github.ryanskonnord.lambdagoyf.card.ArenaCard;
import io.github.ryanskonnord.lambdagoyf.card.Card;
import io.github.ryanskonnord.lambdagoyf.card.CardEdition;
import io.github.ryanskonnord.lambdagoyf.card.CardVersion;
import io.github.ryanskonnord.lambdagoyf.card.CardVersionExtractor;
import io.github.ryanskonnord.lambdagoyf.card.Color;
import io.github.ryanskonnord.lambdagoyf.card.DeckElement;
import io.github.ryanskonnord.lambdagoyf.card.Expansion;
import io.github.ryanskonnord.lambdagoyf.card.FinishedCardVersion;
import io.github.ryanskonnord.lambdagoyf.card.Spoiler;
import io.github.ryanskonnord.lambdagoyf.card.field.CardSupertype;
import io.github.ryanskonnord.lambdagoyf.card.field.ExpansionType;
import io.github.ryanskonnord.lambdagoyf.card.field.Finish;
import io.github.ryanskonnord.lambdagoyf.card.field.Rarity;
import io.github.ryanskonnord.lambdagoyf.deck.ArenaDeckEntry;
import io.github.ryanskonnord.lambdagoyf.deck.ArenaDeckFormatter;
import io.github.ryanskonnord.lambdagoyf.deck.ArenaDeckSeeker;
import io.github.ryanskonnord.lambdagoyf.deck.ArenaVersionId;
import io.github.ryanskonnord.lambdagoyf.deck.CommanderLegality;
import io.github.ryanskonnord.lambdagoyf.deck.CompanionLegality;
import io.github.ryanskonnord.lambdagoyf.deck.Deck;
import io.github.ryanskonnord.lambdagoyf.deck.DeckConstructor;
import io.github.ryanskonnord.lambdagoyf.deck.DeckRandomChoice;
import io.github.ryanskonnord.lambdagoyf.deck.preference.MinimalArtistGrouper;
import io.github.ryanskonnord.lambdagoyf.scryfall.ScryfallParser;
import io.github.ryanskonnord.util.MapCollectors;
import io.github.ryanskonnord.util.MultisetUtil;
import io.github.ryanskonnord.util.OrderingUtil;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.github.ryanskonnord.lambdagoyf.card.field.CardSupertype.BASIC;
import static io.github.ryanskonnord.lambdagoyf.card.field.CardSupertype.SNOW;
import static io.github.ryanskonnord.util.OrderingUtil.OptionalComparator.build;

;

public final class RyansMtgaDecks {

    private static final ImmutableList<String> CORE_SET_TAPLANDS = ImmutableList.of(
            "Meandering River (M19) 253", "Forsaken Sanctuary (M19) 250",
            "Submerged Boneyard (M19) 257", "Highland Lake (M19) 252",
            "Cinder Barrens (M19) 248", "Foul Orchard (M19) 251",
            "Timber Gorge (M19) 258", "Stone Quarry (M19) 256",
            "Tranquil Expanse (M19) 259", "Woodland Stream (M19) 260",
            "Tranquil Cove (M20) 259", "Scoured Barrens (M20) 251",
            "Dismal Backwater (M20) 245", "Swiftwater Cliffs (M20) 252",
            "Bloodfell Caves (M20) 242", "Jungle Hollow (M20) 248",
            "Rugged Highlands (M20) 250", "Wind-Scarred Crag (M20) 260",
            "Blossoming Sands (M20) 243", "Thornwood Falls (M20) 258");
    private static final ImmutableList<String> GUILDGATE_FRONT_DOORS = ImmutableList.of(
            "Azorius Guildgate (RNA) 243", "Orzhov Guildgate (RNA) 252",
            "Dimir Guildgate (GRN) 245", "Izzet Guildgate (GRN) 251",
            "Rakdos Guildgate (RNA) 255", "Golgari Guildgate (GRN) 248",
            "Gruul Guildgate (RNA) 249", "Boros Guildgate (GRN) 243",
            "Selesnya Guildgate (GRN) 255", "Simic Guildgate (RNA) 257");
    private static final ImmutableList<String> GUILDGATE_BACK_DOORS = ImmutableList.of(
            "Simic Guildgate (RNA) 258", "Selesnya Guildgate (GRN) 256",
            "Boros Guildgate (GRN) 244", "Gruul Guildgate (RNA) 250",
            "Golgari Guildgate (GRN) 249", "Rakdos Guildgate (RNA) 256",
            "Izzet Guildgate (GRN) 252", "Dimir Guildgate (GRN) 246",
            "Orzhov Guildgate (RNA) 253", "Azorius Guildgate (RNA) 244");
    private static final ImmutableList<String> UNHINGED_LANDS = buildBasicLandSet("ANA", 1, 2);
    private static final ImmutableList<String> BFZ_LANDS = buildBasicLandSet("BFZ", 250, 5);
    private static final ImmutableList<String> NYX_LANDS = buildBasicLandSet("THB", 250, 1);
    private static final ImmutableList<String> UNSANCTIONED_SQUIRREL_LANDS = buildBasicLandSet("UND", 87, 2);
    private static final ImmutableList<String> UNSANCTIONED_FULL_ART_LANDS = buildBasicLandSet("UND", 88, 2);
    private static final ImmutableList<String> GODZILLA_LANDS = buildBasicLandSet("SLD", 63, 1);
    private static final ImmutableList<String> UNSTABLE_LANDS = buildBasicLandSet("UST", 212, 1);
    private static final ImmutableList<String> M21_SHOWCASE_LANDS = buildBasicLandSet("M21", 309, 1);
    private static final ImmutableList<String> AMONKHET_FULL_ART_LANDS = ImmutableList.of(
            "Plains (AKR) 322", "Island (AKR) 308", "Swamp (AKR) 335", "Mountain (AKR) 315", "Forest (AKR) 297");
    private static final ImmutableList<String> HOUR_FULL_ART_LANDS = ImmutableList.of(
            "Plains (AKR) 324", "Island (AKR) 310", "Swamp (AKR) 337", "Mountain (AKR) 317", "Forest (AKR) 299");
    private static final ImmutableList<String> BOB_ROSS_FREE_LANDS = ImmutableList.of(
            "Plains (SLD) 101", "Island (SLD) 102", "Swamp (SLD) 105", "Mountain (SLD) 106", "Forest (SLD) 108");
    private static final ImmutableList<String> BOB_ROSS_PAID_LANDS = ImmutableList.of(
            "Plains (SLD) 100", "Island (SLD) 103", "Swamp (SLD) 104", "Mountain (SLD) 107", "Forest (SLD) 109", "Evolving Wilds (SLD) 538");
    private static final ImmutableList<String> DRACULA_LANDS = buildBasicLandSet("SLD", 359, 1);
    private static final ImmutableList<String> SNC_LIGHT_ART_DECO_LANDS = ImmutableList.of(
            "Plains (SNC) 273", "Island (SNC) 275", "Swamp (SNC) 276", "Mountain (SNC) 278", "Forest (SNC) 280");
    private static final ImmutableList<String> SNC_DARK_ART_DECO_LANDS = ImmutableList.of(
            "Plains (SNC) 272", "Island (SNC) 274", "Swamp (SNC) 277", "Mountain (SNC) 279", "Forest (SNC) 281");
    private static final ImmutableList<String> DMU_STAINED_GLASS_LANDS = buildBasicLandSet("DMU", 277, 1);
    private static final ImmutableList<String> ONE_FULL_ART_LANDS = buildBasicLandSet("ONE", 262, 1);
    private static final ImmutableList<String> ONE_PHYREXIAN_LANDS = buildBasicLandSet("ONE", 267, 1);
    private static final ImmutableList<String> UNF_ORBITAL = buildBasicLandSet("UNF", 240, 1);
    private static final ImmutableList<String> UNF_PLANETARY = buildBasicLandSet("UNF", 235, 1);
    private static final ImmutableList<String> MOM_BIG_SYMBOL_LANDS = ImmutableList.of(
            "Plains (MOM) 283", "Island (MOM) 284", "Swamp (MOM) 287", "Mountain (MOM) 289", "Forest (MOM) 291");
    private static final ImmutableList<String> MOM_INVASION_LANDS = ImmutableList.of(
            "Plains (MOM) 282", "Island (MOM) 285", "Swamp (MOM) 286", "Mountain (MOM) 288", "Forest (MOM) 290");

    private static enum BloomburrowLandSeason {
        SPRING, SUMMER, FALL, WINTER;

        private ImmutableList<String> getEntries() {
            return buildBasicLandSet("BLB", 262 + ordinal(), 4);
        }

        public static ImmutableSet<ImmutableSet<ArenaDeckEntry>> getGroups() {
            return EnumSet.allOf(BloomburrowLandSeason.class).stream()
                    .map(BloomburrowLandSeason::getEntries)
                    .map(RyansMtgaDecks::parseEntries)
                    .collect(ImmutableSet.toImmutableSet());
        }
    }

    private static final ImmutableList<String> makeSnowBasicLandSet(String setCode, int startingNumber, int increment) {
        return buildBasicLandSet(setCode, startingNumber, increment)
                .stream().map(name -> "Snow-Covered " + name).collect(ImmutableList.toImmutableList());
    }

    private static final ImmutableList<String> MODERN_HORIZONS_SNOW_LANDS = makeSnowBasicLandSet("MH1", 250, 1);
    private static final ImmutableList<String> PIXEL_SNOW_LANDS = makeSnowBasicLandSet("SLD", 325, 1);
    private static final ImmutableList<String> ELK64_SNOW_LANDS = makeSnowBasicLandSet("SLD", 1473, 1);

    private static final ImmutableSet<String> ETERNAL_FAVORITES = ImmutableSet.<String>builder()
            .addAll(CORE_SET_TAPLANDS)
            .addAll(GUILDGATE_FRONT_DOORS)
            .add(new String[]{
                    "Duress (M19) 94",
                    "Lightning Strike (M19) 152",
                    "Gateway Plaza (WAR) 246",
                    "Murder (M20) 109",
                    "Opt (ELD) 59",
                    "Essence Scatter (M19) 54",
                    "Evolving Wilds (SLD) 538",
                    "Shock (M21) 159",
                    "Negate (ZNR) 71",
            })
            .build();


    private static ImmutableList<String> buildBasicLandSet(String setCode, int startingNumber, int increment) {
        List<String> basicLandNames = Color.getBasicLandTypes().asList();
        return IntStream.range(0, basicLandNames.size())
                .mapToObj((int index) -> String.format("%s (%s) %d",
                        basicLandNames.get(index), setCode, startingNumber + increment * index))
                .collect(ImmutableList.toImmutableList());
    }

    private static ImmutableSet<ArenaDeckEntry> parseEntries(Collection<String> rawEntries) {
        ImmutableSet<ArenaDeckEntry> parsed = rawEntries.stream()
                .map(ArenaDeckEntry::parse)
                .collect(ImmutableSet.toImmutableSet());
        for (ArenaDeckEntry entry : parsed) {
            Preconditions.checkArgument(entry.getVersionId().isPresent(),
                    "Entry must include set and collector number");
        }
        return parsed;
    }


    private static Set<ImmutableSet<ArenaDeckEntry>> standardLandChoices() {
        Set<ImmutableSet<ArenaDeckEntry>> groups = Sets.newHashSetWithExpectedSize(9);
        Stream.of(MOM_INVASION_LANDS, MOM_BIG_SYMBOL_LANDS, ONE_PHYREXIAN_LANDS, ONE_FULL_ART_LANDS, DMU_STAINED_GLASS_LANDS)
                .map(RyansMtgaDecks::parseEntries)
                .forEachOrdered(groups::add);
        groups.addAll(BloomburrowLandSeason.getGroups());
        return groups;
    }

    private static Set<ImmutableSet<ArenaDeckEntry>> explorerLandChoices() {
        Set<ImmutableSet<ArenaDeckEntry>> groups = Sets.newHashSetWithExpectedSize(16);
        Stream.of(SNC_DARK_ART_DECO_LANDS, SNC_LIGHT_ART_DECO_LANDS, HOUR_FULL_ART_LANDS, AMONKHET_FULL_ART_LANDS,
                        M21_SHOWCASE_LANDS, NYX_LANDS)
                .map(RyansMtgaDecks::parseEntries)
                .forEachOrdered(groups::add);
        groups.addAll(standardLandChoices());
        groups.removeAll(BloomburrowLandSeason.getGroups());
        return groups;
    }

    private static Set<ImmutableSet<ArenaDeckEntry>> timelessLandChoices() {
        Set<ImmutableSet<ArenaDeckEntry>> groups = Sets.newHashSetWithExpectedSize(22);
        Stream.of(DRACULA_LANDS, BOB_ROSS_PAID_LANDS, BOB_ROSS_FREE_LANDS, GODZILLA_LANDS, UNF_PLANETARY, UNF_ORBITAL)
                .map(RyansMtgaDecks::parseEntries)
                .forEachOrdered(groups::add);
        groups.addAll(explorerLandChoices());
        return groups;
    }

    private static Set<ImmutableSet<ArenaDeckEntry>> snowLandChoices() {
        Set<Collection<String>> groups = Sets.newHashSetWithExpectedSize(5);
        groups.add(ImmutableList.of( // by Adam Paquette
                "Snow-Covered Plains (KHM) 277", "Snow-Covered Island (KHM) 279", "Snow-Covered Swamp (KHM) 280",
                "Snow-Covered Mountain (KHM) 282", "Snow-Covered Forest (KHM) 284"));
        groups.add(ImmutableList.of( // by other artists
                "Snow-Covered Plains (KHM) 276", "Snow-Covered Island (KHM) 278", "Snow-Covered Swamp (KHM) 281",
                "Snow-Covered Mountain (KHM) 283", "Snow-Covered Forest (KHM) 285"));
        Collections.addAll(groups, MODERN_HORIZONS_SNOW_LANDS, PIXEL_SNOW_LANDS, ELK64_SNOW_LANDS);
        return groups.stream().map(RyansMtgaDecks::parseEntries).collect(ImmutableSet.toImmutableSet());
    }


    private static <V extends CardVersion> UnaryOperator<Deck<V>> ifMixesSnowAndNonsnow(UnaryOperator<Deck<V>> delegate) {
        Objects.requireNonNull(delegate);
        Predicate<Card> isSnow = c -> c.getMainTypeLine().is(SNOW);
        return deck -> {
            Collection<Card> basicLands = deck.getAllCards().elementSet().stream()
                    .map(CardVersion::getCard).distinct()
                    .filter(c -> c.getMainTypeLine().is(CardSupertype.BASIC))
                    .collect(Collectors.toList());
            return basicLands.stream().anyMatch(isSnow) && basicLands.stream().anyMatch(isSnow.negate()) ? delegate.apply(deck) : deck;
        };
    }

    private static boolean isSubrareAfter(CardVersion version, String date) {
        CardEdition edition = version.getEdition();
        if (edition.getRarity().getEnum().filter(r -> r.compareTo(Rarity.RARE) < 0).isEmpty()) {
            return false;
        }
        Expansion expansion = edition.getExpansion();
        return !expansion.getReleaseDate().isBefore(LocalDate.parse(date))
                && expansion.getType().getEnum().filter(ExpansionType::isStandardRelease).isPresent();
    }


    public static void main(String[] args) throws Exception {
        Spoiler spoiler = ScryfallParser.createSpoiler();
        Path rootDirectory = Environment.getDeckFilePath().resolve("Constructed");

        AlchemyConverter alchemyConverter = new AlchemyConverter(spoiler);
        UnaryOperator<Deck<Card>> toAlchemy = deck -> deck.transform(alchemyConverter.toAlchemyCardTransformer());
        UnaryOperator<Deck<Card>> fromAlchemy = deck -> deck.transform(alchemyConverter.fromAlchemyCardTransformer());

        List<CardEdition> vowBasics = CardVersionExtractor.getCardEditions().getAll(spoiler).filter(e -> e.getExpansion().isNamed("Crimson Vow") && e.getCard().getMainTypeLine().is(BASIC))
                .collect(Collectors.toList());

        UnaryOperator<Deck<ArenaCard>> znrGroups = new MinimalArtistGrouper.Builder<>(spoiler, CardVersionExtractor.getArenaCard())
                .addPredicate(e -> e.isFullArt() && e.getExpansion().isNamed("Zendikar Rising"))
                .getModifier();
        UnaryOperator<Deck<ArenaCard>> geoPromoGroups = new MinimalArtistGrouper.Builder<>(spoiler, CardVersionExtractor.getArenaCard())
                .addPredicate(e -> Stream.of("PALP", "PELP").anyMatch(name -> e.getExpansion().isNamed(name)))
                .defaultToAnyArtist()
                .getModifier();
        UnaryOperator<Deck<ArenaCard>> kaldheimGroups = new MinimalArtistGrouper.Builder<>(spoiler, CardVersionExtractor.getArenaCard())
                .addPredicate(e -> e.getExpansion().isNamed("Kaldheim"))
                .setScope(MinimalArtistGrouper.Scope.ALL_BASIC_LANDS)
                .getModifier();
        UnaryOperator<Deck<ArenaCard>> eternalNight = new MinimalArtistGrouper.Builder<>(spoiler, CardVersionExtractor.getArenaCard())
                .addPredicate((CardEdition edition) -> {
                    Expansion expansion = edition.getExpansion();
                    boolean expansionCheck = expansion.isNamed("Midnight Hunt") || expansion.isNamed("Crimson Vow");
                    return expansionCheck && edition.getCollectorNumber().getNumber() <= 277;
                })
                .getModifier();
        UnaryOperator<Deck<ArenaCard>> broGroups = new MinimalArtistGrouper.Builder<>(spoiler, CardVersionExtractor.getArenaCard())
                .addPredicate(e -> e.isFullArt() && e.getExpansion().isNamed("BRO"))
                .defaultToAnyArtist()
                .getModifier();

        generate(spoiler, rootDirectory.resolve("Standard"), builder -> builder
                        .addDeckTransformation(fromAlchemy)
                        .withPreferenceOrder().override(Comparator.comparing((CardVersion version) -> isSubrareAfter(version, "2022-09-09")).reversed())
                        .addOutputTransformation(randomArenaReplacement(BloomburrowLandSeason.getGroups()))
//                .addVersionTransformation(broGroups)
        );
        generate(spoiler, rootDirectory.resolve("Explorer"), builder -> builder
                .addDeckTransformation(fromAlchemy)
                .withPreferenceOrder().override(preferArenaEntries(ETERNAL_FAVORITES, UNSTABLE_LANDS, MODERN_HORIZONS_SNOW_LANDS))
                .withPreferenceOrder().override(Comparator.comparing(c -> c.getEdition().getExpansion().isNamed("Strixhaven Mystical Archive")))
                .addDeckTransformation(deck -> CompanionLegality.addMissingCompanion(spoiler, deck, c -> true, Function.identity()))
                .addOutputTransformation(randomArenaReplacement(explorerLandChoices())));

        generate(spoiler, rootDirectory.resolve("Alchemy"), builder -> builder
                .addDeckTransformation(toAlchemy)
                .withPreferenceOrder().override(Comparator.comparing((CardVersion version) -> isSubrareAfter(version, "2022-09-22")).reversed())
                .withPreferenceOrder().override(Comparator.comparing(c -> !c.getEdition().getExpansion().isNamed("Strixhaven Mystical Archive")))
                .withPreferenceOrder().override(preferArenaEntries(ETERNAL_FAVORITES, UNF_PLANETARY, PIXEL_SNOW_LANDS)));
        generate(spoiler, rootDirectory.resolve("Historic"), builder -> builder
                .addDeckTransformation(toAlchemy)
                .withPreferenceOrder().override(Comparator.comparing(c -> !c.getEdition().getExpansion().isNamed("Strixhaven Mystical Archive")))
                .withPreferenceOrder().override(preferArenaEntries(ETERNAL_FAVORITES, UNF_ORBITAL, MODERN_HORIZONS_SNOW_LANDS))
                .addDeckTransformation(deck -> CompanionLegality.addMissingCompanion(spoiler, deck, c -> true, Function.identity())));
        generate(spoiler, rootDirectory.resolve("Timeless"), builder -> builder
                .withPreferenceOrder().override(Comparator.comparing(c -> !c.getEdition().getExpansion().isNamed("Strixhaven Mystical Archive")))
                .withPreferenceOrder().override(preferArenaEntries(ETERNAL_FAVORITES, M21_SHOWCASE_LANDS, MODERN_HORIZONS_SNOW_LANDS))
                .addDeckTransformation(deck -> CompanionLegality.addMissingCompanion(spoiler, deck, c -> true, Function.identity()))
                .addOutputTransformation(randomArenaReplacement(timelessLandChoices())));
        generate(spoiler, rootDirectory.resolve("Historic Brawl"), builder -> builder
                .addDeckTransformation(toAlchemy)
                .withPreferenceOrder().override(Comparator.comparing(c -> !c.getEdition().getExpansion().isNamed("Strixhaven Mystical Archive")))
                .withPreferenceOrder().override(preferArenaEntries(ETERNAL_FAVORITES, UNHINGED_LANDS, MODERN_HORIZONS_SNOW_LANDS))
                .addDeckTransformation(CommanderLegality::inferCommander)
                .addOutputTransformation(useBasicLandsMatchingCommander(CardVersionExtractor.getArenaCard(), ArenaDeckEntry::new, c -> c.getEdition().isFullArt()))
                .addOutputTransformation(randomArenaReplacement(snowLandChoices())));
    }

    private static final Comparator<Collection<ArenaDeckEntry>> ARBITRARY_GROUP_ORDER = new Comparator<>() {
        private final Comparator<ArenaVersionId> versionIdComparator = Comparator
                .comparing(ArenaVersionId::getExpansionCode) // alphabetical, not by release date
                .thenComparing(ArenaVersionId::getCollectorNumber);
        private final Comparator<ArenaDeckEntry> entryComparator = build(ArenaDeckEntry::getVersionId, versionIdComparator).emptyKeysFirst()
                .thenComparing(ArenaDeckEntry::getCardName);

        @Override
        public int compare(Collection<ArenaDeckEntry> o1, Collection<ArenaDeckEntry> o2) {
            if (o1 == o2) return 0;

            // Groups generally have no overlap, so try comparing only one element first
            Optional<ArenaDeckEntry> m1 = o1.stream().min(entryComparator);
            Optional<ArenaDeckEntry> m2 = o2.stream().min(entryComparator);
            if (m1.isPresent() && m2.isPresent()) {
                int minCompare = entryComparator.compare(m1.get(), m2.get());
                if (minCompare != 0) return minCompare;
            }

            // Fall back to comparing all elements
            Iterator<ArenaDeckEntry> s1 = o1.stream().sorted(entryComparator).iterator();
            Iterator<ArenaDeckEntry> s2 = o2.stream().sorted(entryComparator).iterator();
            return OrderingUtil.compareLexicographically(entryComparator, s1, s2);
        }
    };

    private static UnaryOperator<Deck<ArenaDeckEntry>> randomArenaReplacement(Collection<? extends Collection<ArenaDeckEntry>> groups) {
        for (Collection<ArenaDeckEntry> group : groups) {
            for (ArenaDeckEntry entry : group) {
                Preconditions.checkArgument(entry.getVersionId().isPresent());
            }
        }

        // Impose a consistent order so that random yields are stable even if a group has inconsistent iteration order
        List<Collection<ArenaDeckEntry>> sortedGroups = ImmutableList.sortedCopyOf(ARBITRARY_GROUP_ORDER, groups);

        return randomReplacement(sortedGroups);
    }

    private static <V extends CardVersion, T extends DeckElement<V>> UnaryOperator<Deck<T>> randomReplacement(Collection<? extends Collection<T>> groups) {
        Preconditions.checkArgument(!groups.isEmpty(), "Groups must not be empty");
        return (Deck<T> deck) -> {
            Deck<Card> unversionedDeck = deck.flatTransform(card -> card.getVersion().map(CardVersion::getCard));
            DeckRandomChoice rng = DeckRandomChoice.withSalt(0x2f9dfb3a39bb54ccL).forDeck(unversionedDeck);
            Collection<T> chosenGroup = rng.choose(groups);
            ImmutableMap<String, T> replacements = Maps.uniqueIndex(chosenGroup, DeckElement::getCardName);
            return deck.transform((T element) -> replacements.getOrDefault(element.getCardName(), element));
        };
    }

    private static <V extends CardVersion, T extends DeckElement<V>> UnaryOperator<Deck<T>> useBasicLandsMatchingCommander(
            CardVersionExtractor<V> extractor,
            Function<V, T> outputCtor,
            Predicate<V> preference) {
        Objects.requireNonNull(extractor);
        return (Deck<T> deck) -> {
            Set<Expansion> commanderExpansions = deck.get(Deck.Section.COMMANDER).stream()
                    .flatMap(c -> c.getVersion().map(v -> v.getEdition().getExpansion()).stream())
                    .collect(Collectors.toSet());
            if (commanderExpansions.size() != 1) return deck;
            Expansion expansion = Iterables.getOnlyElement(commanderExpansions);
            DeckRandomChoice shuffler = DeckRandomChoice.withSalt(0xb2c39c0f5cc4954aL).forDeck(deck.flatTransform(DeckElement::getVersion));
            return deck.transformCards((Multiset.Entry<T> entry) -> {
                Optional<V> version = entry.getElement().getVersion();
                if (version.isEmpty()) return null;
                Card card = version.get().getCard();
                if (!card.getMainTypeLine().is(CardSupertype.BASIC)) return null;
                List<V> versions = extractor.fromCard(card).filter(e -> e.getEdition().getExpansion().equals(expansion))
                        .collect(Collectors.toCollection(ArrayList::new));
                if (preference != null) {
                    List<V> preferredVersions = versions.stream().filter(preference)
                            .collect(Collectors.toCollection(ArrayList::new));
                    if (!preferredVersions.isEmpty()) {
                        versions = preferredVersions;
                    }
                }
                if (versions.isEmpty()) return null;
                versions = shuffler.forCard(card).shuffle(versions);
                return evenDistribution(entry.getCount(), versions.stream().map(outputCtor).collect(Collectors.toList()));
            });
        };
    }

    private static <V extends CardVersion> UnaryOperator<Deck<V>> useMidnightLands(CardVersionExtractor<V> extractor) {
        Objects.requireNonNull(extractor);
        return (Deck<V> deck) -> {
            boolean useMumford = false;//DeckRandomChoice.withSalt(0x58ef6d32172d82c8L).forDeck(deck).chooseBoolean();
            return deck.transformCards((Multiset.Entry<V> entry) -> {
                Card card = entry.getElement().getCard();
                if (!Color.getBasicLandTypes().contains(card.getMainName())) return null;
                V chosenVersion = extractor.fromCard(card)
                        .filter((V version) -> {
                                    if (version instanceof FinishedCardVersion && ((FinishedCardVersion) version).getFinish() != Finish.NONFOIL) {
                                        return false;
                                    }
                                    CardEdition edition = version.getEdition();
                                    return edition.getExpansion().isNamed("Midnight Hunt")
                                            && edition.getCollectorNumber().getNumber() <= 277
                                            && edition.getArtists().anyMatch(a -> a.equals("Dan Mumford") == useMumford);
                                }
                        )
                        .collect(MoreCollectors.onlyElement());
                return MultisetUtil.ofSingleEntry(chosenVersion, entry.getCount());
            });
        };
    }

    private static <T> ImmutableMultiset<T> evenDistribution(int count, List<T> elements) {
        Preconditions.checkArgument(count >= 0);
        if (elements.isEmpty()) {
            if (count == 0) {
                return ImmutableMultiset.of();
            } else {
                throw new IllegalArgumentException();
            }
        }

        ImmutableMultiset.Builder<T> builder = ImmutableMultiset.builder();
        int quotient = count / elements.size();
        int remainder = count % elements.size();
        for (ListIterator<T> iterator = elements.listIterator(); iterator.hasNext(); ) {
            int index = iterator.nextIndex();
            T element = iterator.next();
            builder.addCopies(element, quotient + (index < remainder ? 1 : 0));
        }
        return builder.build();
    }

    private static Comparator<ArenaCard> preferArenaEntries(Collection<String>... favoriteSteps) {
        Map<String, ArenaVersionId> parsedEntries = new HashMap<>();
        for (Collection<String> step : favoriteSteps) {
            ImmutableMap<String, ArenaVersionId> mapForStep = step.stream()
                    .map(ArenaDeckEntry::parse)
                    .collect(MapCollectors.<ArenaDeckEntry>collecting()
                            .withKey(ArenaDeckEntry::getCardName)
                            .withValue(e -> e.getVersionId().orElseThrow(() ->
                                    new IllegalArgumentException("Provided entries must contain version IDs")))
                            .uniqueOrElseThrow((x1, x2) -> new RuntimeException(String.format("Collision: %s; %s", x1, x2)))
                            .toImmutableMap());

            parsedEntries.putAll(mapForStep); // later collections override earlier ones
        }

        ImmutableSet<ArenaVersionId> favorites = ImmutableSet.copyOf(parsedEntries.values());
        return Comparator.comparing((ArenaCard arenaCard) -> {
            Optional<ArenaVersionId> versionId = arenaCard.getDeckEntry().getVersionId();
            return versionId.isEmpty() || !favorites.contains(versionId.get());
        });
    }

    public static void stripMtgGoldfishPrefixes(Path sourceDirectory) throws IOException {
        final String mtgGoldfishPrefix = "Deck - ";
        List<Path> prefixed = Files.walk(sourceDirectory)
                .filter(p -> p.getFileName().toString().startsWith(mtgGoldfishPrefix))
                .collect(Collectors.toList());
        for (Path path : prefixed) {
            Files.move(path, path.getParent().resolve(path.getFileName().toString().substring(mtgGoldfishPrefix.length())));
        }
    }

    private static void generate(Spoiler spoiler, Path sourceDirectory,
                                 Consumer<DeckConstructor.Builder<ArenaCard, ArenaDeckEntry>> modifier)
            throws IOException {
        DeckConstructor.Builder<ArenaCard, ArenaDeckEntry> builder = DeckConstructor.createForArena()
                .withPreferenceOrder().override(Comparator.comparing((ArenaCard arenaCard) -> arenaCard.getEdition().getReleaseDate()))
                .addDeckTransformation(CompanionLegality::extractCompanion);
        modifier.accept(builder);
        builder.withPreferenceOrder().override(Comparator.comparing((ArenaCard arenaCard) ->
                arenaCard.getEdition().getExpansion().isNamed("PANA")));
        DeckConstructor<ArenaCard, ArenaDeckEntry> deckConstructor = builder.build();

        Path destinationDirectory = sourceDirectory.resolve("Arena");
        Files.createDirectories(destinationDirectory);

        stripMtgGoldfishPrefixes(sourceDirectory);

        List<ArenaDeckSeeker.Entry> entries = new ArenaDeckSeeker(spoiler, deckConstructor)
                .seek(sourceDirectory, destinationDirectory)
                .collect(Collectors.toList());
        for (ArenaDeckSeeker.Entry entry : entries) {
            Path source = entry.getSource();
            Path destination = entry.getDestination();
            if (Files.exists(destination)) continue;

            try {
                Deck<Card> deck;
                try (Reader reader = Files.newBufferedReader(source)) {
                    deck = ArenaDeckFormatter.readDeck(spoiler, reader);
                }
                Deck<ArenaDeckEntry> arenaDeck = deckConstructor.createDeck(deck);
                arenaDeck = arenaDeck.sortCards(ArenaDeckFormatter.orderArenaCards());
                arenaDeck = ArenaDeckFormatter.prioritizeBestOfOneSideboard(arenaDeck);
                try (Writer writer = Files.newBufferedWriter(destination)) {
                    ArenaDeckFormatter.write(writer, arenaDeck);
                }
            } catch (Exception e) {
                System.err.println("Error on " + entry.getSource());
                e.printStackTrace();
            }
        }
    }

    private static void printVaultFillerDeck(Writer destination, Spoiler spoiler,
                                             Expansion expansion, Rarity rarity, int count) {
        Multiset<ArenaCard> cards = CardVersionExtractor.getArenaCard().getAll(spoiler)
                .sorted()
                .filter(card -> {
                    CardEdition edition = card.getEdition();
                    return edition.getExpansion().equals(expansion) && edition.getRarity().is(rarity);
                })
                .collect(ImmutableMultiset.toImmutableMultiset(Function.identity(), e -> count));
        Deck<ArenaCard> deck = new Deck.Builder<ArenaCard>().addTo(Deck.Section.MAIN_DECK, cards).build();
        ArenaDeckFormatter.write(destination, deck.transform(ArenaCard::getDeckEntry));
    }

}
