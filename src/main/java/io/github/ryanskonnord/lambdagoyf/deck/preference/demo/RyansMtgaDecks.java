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
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.MoreCollectors;
import com.google.common.collect.Multiset;
import io.github.ryanskonnord.lambdagoyf.Environment;
import io.github.ryanskonnord.lambdagoyf.card.AlchemyConverter;
import io.github.ryanskonnord.lambdagoyf.card.ArenaCard;
import io.github.ryanskonnord.lambdagoyf.card.Card;
import io.github.ryanskonnord.lambdagoyf.card.CardEdition;
import io.github.ryanskonnord.lambdagoyf.card.CardVersion;
import io.github.ryanskonnord.lambdagoyf.card.CardVersionExtractor;
import io.github.ryanskonnord.lambdagoyf.card.CollectorNumber;
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
import io.github.ryanskonnord.lambdagoyf.deck.CommanderLegality;
import io.github.ryanskonnord.lambdagoyf.deck.CompanionLegality;
import io.github.ryanskonnord.lambdagoyf.deck.Deck;
import io.github.ryanskonnord.lambdagoyf.deck.DeckConstructor;
import io.github.ryanskonnord.lambdagoyf.deck.DeckRandomChoice;
import io.github.ryanskonnord.lambdagoyf.deck.MtgoDeckFormatter;
import io.github.ryanskonnord.lambdagoyf.deck.preference.MinimalArtistGrouper;
import io.github.ryanskonnord.lambdagoyf.scryfall.ScryfallParser;
import io.github.ryanskonnord.util.MapCollectors;
import io.github.ryanskonnord.util.MultisetUtil;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
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
    private static final ImmutableList<String> SNC_ART_DECO_LANDS = ImmutableList.<String>builder()
            .addAll(buildBasicLandSet("SNC", 272, 1))
            .addAll(buildBasicLandSet("SNC", 273, 1))
            .build();
    private static final ImmutableList<String> DMU_STAINED_GLASS_LANDS = buildBasicLandSet("DMU", 277, 1);
    private static final ImmutableList<String> ONE_FULL_ART_LANDS = buildBasicLandSet("ONE", 262, 1);
    private static final ImmutableList<String> ONE_PHYREXIAN_LANDS = buildBasicLandSet("ONE", 267, 1);
    private static final ImmutableList<String> UNF_ORBITAL = buildBasicLandSet("UNF", 240, 1);
    private static final ImmutableList<String> UNF_PLANETARY = buildBasicLandSet("UNF", 235, 1);
    private static final ImmutableList<String> MOM_BIG_SYMBOL_LANDS = ImmutableList.of(
            "1 Plains (MOM) 283", "Island (MOM) 284", "Swamp (MOM) 287", "Mountain (MOM) 289", "Forest (MOM) 291)");
    private static final ImmutableList<String> MOM_INVASION_LANDS = ImmutableList.of(
            "Plains (MOM) 282", "Island (MOM) 285", "Swamp (MOM) 286", "Mountain (MOM) 288", "Forest (MOM) 290");

    private static final ImmutableList<String> MODERN_HORIZONS_SNOW_LANDS = buildBasicLandSet("MH1", 250, 1)
            .stream().map(name -> "Snow-Covered " + name).collect(ImmutableList.toImmutableList());
    private static final ImmutableList<String> PIXEL_SNOW_LANDS = buildBasicLandSet("SLD", 325, 1)
            .stream().map(name -> "Snow-Covered " + name).collect(ImmutableList.toImmutableList());


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

    public static UnaryOperator<Deck<ArenaCard>> chooseHappyAccidents(Spoiler spoiler) {
        ImmutableSetMultimap<Card, ArenaCard> versionMap = CardVersionExtractor.getArenaCard().getAll(spoiler)
                .filter(c -> c.getEdition().hasArtist("Bob Ross"))
                .collect(MapCollectors.<ArenaCard>collecting()
                        .indexing(ArenaCard::getCard)
                        .grouping().toImmutableSetMultimap());
        return (Deck<ArenaCard> deck) -> {
            DeckRandomChoice accident = DeckRandomChoice.withSalt(0x8f7b52a7737a1410L).forDeck(deck);
            ImmutableMap<Card, ArenaCard> choices = versionMap.asMap().entrySet().stream()
                    .map((Map.Entry<Card, Collection<ArenaCard>> entry) -> {
                        List<ArenaCard> versions = ((ImmutableCollection<ArenaCard>) entry.getValue()).asList();
                        ArenaCard choice = accident.forCard(entry.getKey()).choose(versions);
                        return Maps.immutableEntry(entry.getKey(), choice);
                    })
                    .collect(MapCollectors.<Card, ArenaCard>collectingEntries().unique().toImmutableMap());
            return deck.transform((ArenaCard arenaCard) -> choices.getOrDefault(arenaCard.getCard(), arenaCard));
        };
    }

    private static UnaryOperator<Deck<ArenaCard>> chooseArtDecoGroup(Spoiler spoiler) {
        Expansion snc = spoiler.getExpansion("SNC").orElseThrow();
        int[][] collectorGroups = {
                {272, 274, 277, 279, 281}, // dark
                {273, 275, 276, 278, 280}, // light
        };
        ImmutableList<ImmutableMap<Card, ArenaCard>> groupMaps = Arrays.stream(collectorGroups)
                .map(collectorGroup -> Arrays.stream(collectorGroup)
                        .mapToObj(collectorNumber -> spoiler.getByCollectorNumber(snc, CollectorNumber.of(collectorNumber)).orElseThrow())
                        .collect(MapCollectors.<CardEdition>collecting()
                                .withKey(CardEdition::getCard)
                                .withValue(e -> e.getArenaCard().orElseThrow())
                                .unique().toImmutableMap()))
                .collect(ImmutableList.toImmutableList());
        return (Deck<ArenaCard> deck) -> {
            ImmutableMap<Card, ArenaCard> groupMap = DeckRandomChoice.withSalt(0xde74e0c24692dfb6L).forDeck(deck).choose(groupMaps);
            return deck.transform((UnaryOperator<ArenaCard>) arenaCard -> groupMap.getOrDefault(arenaCard.getCard(), arenaCard));
        };
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

        UnaryOperator<Deck<ArenaCard>> artDecoGroups = chooseArtDecoGroup(spoiler);
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

        generate(spoiler, rootDirectory.resolve("Standard"), new Consumer<DeckConstructor.Builder<ArenaCard, ArenaDeckEntry>>() {
                    @Override
                    public void accept(DeckConstructor.Builder<ArenaCard, ArenaDeckEntry> builder) {
                        builder
                                .addDeckTransformation(fromAlchemy)
                                .withPreferenceOrder().override(Comparator.comparing((CardVersion version) -> isSubrareAfter(version, "2021-09-24")).reversed())
                                .withPreferenceOrder().override(preferArenaEntries(DMU_STAINED_GLASS_LANDS));
                    }
                }
//                .addVersionTransformation(broGroups)
        );
        generate(spoiler, rootDirectory.resolve("Explorer"), builder -> builder
                .addDeckTransformation(fromAlchemy)
                .withPreferenceOrder().override(preferArenaEntries(ETERNAL_FAVORITES, UNSTABLE_LANDS, MODERN_HORIZONS_SNOW_LANDS))
                .withPreferenceOrder().override(Comparator.comparing(c -> c.getEdition().getExpansion().isNamed("Strixhaven Mystical Archive")))
                .addDeckTransformation(deck -> CompanionLegality.addMissingCompanion(spoiler, deck, c -> true, Function.identity())));

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
                .addDeckTransformation(deck -> CompanionLegality.addMissingCompanion(spoiler, deck, c -> true, Function.identity())));
        generate(spoiler, rootDirectory.resolve("Historic Brawl"), builder -> builder
                .addDeckTransformation(toAlchemy)
                .withPreferenceOrder().override(Comparator.comparing(c -> !c.getEdition().getExpansion().isNamed("Strixhaven Mystical Archive")))
                .withPreferenceOrder().override(preferArenaEntries(ETERNAL_FAVORITES, UNHINGED_LANDS, MODERN_HORIZONS_SNOW_LANDS))
                .addDeckTransformation(CommanderLegality::inferCommander)
                .addOutputTransformation(useBasicLandsMatchingCommander(CardVersionExtractor.getArenaCard(), ArenaDeckEntry::new, c -> c.getEdition().isFullArt())));
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
        Map<String, ArenaDeckEntry> parsedEntries = new HashMap<>();
        for (Collection<String> step : favoriteSteps) {
            ImmutableMap<String, ArenaDeckEntry> mapForStep = step.stream()
                    .map(entry -> ArenaDeckEntry.parse(entry).orElseThrow(() ->
                            new IllegalArgumentException("Arena deck entry not recognized: " + entry)))
                    .collect(MapCollectors.<ArenaDeckEntry>collecting()
                            .indexing(ArenaDeckEntry::getCardName)
                            .uniqueOrElseThrow((x1, x2) -> new RuntimeException(String.format("Collision: %s; %s", x1, x2)))
                            .toImmutableMap());

            parsedEntries.putAll(mapForStep); // later collections override earlier ones
        }

        ImmutableSet<ArenaDeckEntry> favorites = ImmutableSet.copyOf(parsedEntries.values());
        return Comparator.comparing((ArenaCard arenaCard) -> !favorites.contains(arenaCard.getDeckEntry()));
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

    private static Optional<Deck<Card>> readDeck(Spoiler spoiler, Path source) throws IOException {
        Deck<Card> deck;
        try (Reader reader = Files.newBufferedReader(source)) {
            Deck<String> cardNames = MtgoDeckFormatter.parseTxt(reader);
            deck = MtgoDeckFormatter.createDeckFromCardNames(spoiler, cardNames);
        } catch (MtgoDeckFormatter.DeckDataException mtgoException) {
            try (Reader reader = Files.newBufferedReader(source)) {
                Deck<ArenaCard> arenaDeck = ArenaDeckFormatter.readDeck(spoiler, reader);
                deck = arenaDeck.transform(CardVersion::getCard);
            } catch (ArenaDeckFormatter.DeckDataException arenaException) {
                System.err.println(mtgoException.getMessage());
                System.err.println(arenaException.getMessage());
                return Optional.empty();
            }
        }
        return Optional.of(deck);
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
                Optional<Deck<Card>> deck = readDeck(spoiler, source);
                if (deck.isPresent()) {
                    Deck<ArenaDeckEntry> arenaDeck = deckConstructor.createDeck(deck.get());
                    arenaDeck = arenaDeck.sortCards(ArenaDeckFormatter.orderArenaCards());
                    arenaDeck = ArenaDeckFormatter.prioritizeBestOfOneSideboard(arenaDeck);
                    try (Writer writer = Files.newBufferedWriter(destination)) {
                        ArenaDeckFormatter.write(writer, arenaDeck);
                    }
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
