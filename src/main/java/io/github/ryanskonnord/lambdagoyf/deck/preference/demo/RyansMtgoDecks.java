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

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MoreCollectors;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import io.github.ryanskonnord.lambdagoyf.card.Card;
import io.github.ryanskonnord.lambdagoyf.card.CardEdition;
import io.github.ryanskonnord.lambdagoyf.card.CardEditionFace;
import io.github.ryanskonnord.lambdagoyf.card.CardIllustration;
import io.github.ryanskonnord.lambdagoyf.card.CardNames;
import io.github.ryanskonnord.lambdagoyf.card.CardVersion;
import io.github.ryanskonnord.lambdagoyf.card.CardVersionExtractor;
import io.github.ryanskonnord.lambdagoyf.card.Color;
import io.github.ryanskonnord.lambdagoyf.card.DeckElement;
import io.github.ryanskonnord.lambdagoyf.card.Expansion;
import io.github.ryanskonnord.lambdagoyf.card.MtgoCard;
import io.github.ryanskonnord.lambdagoyf.card.Spoiler;
import io.github.ryanskonnord.lambdagoyf.card.TypeLine;
import io.github.ryanskonnord.lambdagoyf.card.field.BorderColor;
import io.github.ryanskonnord.lambdagoyf.card.field.CardSupertype;
import io.github.ryanskonnord.lambdagoyf.card.field.CardType;
import io.github.ryanskonnord.lambdagoyf.card.field.ExpansionType;
import io.github.ryanskonnord.lambdagoyf.card.field.Finish;
import io.github.ryanskonnord.lambdagoyf.card.field.Format;
import io.github.ryanskonnord.lambdagoyf.card.field.FrameStyle;
import io.github.ryanskonnord.lambdagoyf.card.field.Watermark;
import io.github.ryanskonnord.lambdagoyf.deck.CompanionLegality;
import io.github.ryanskonnord.lambdagoyf.deck.Deck;
import io.github.ryanskonnord.lambdagoyf.deck.DeckConstructor;
import io.github.ryanskonnord.lambdagoyf.deck.DeckRandomChoice;
import io.github.ryanskonnord.lambdagoyf.deck.DeckSeeker;
import io.github.ryanskonnord.lambdagoyf.deck.DeckSeeker.DeckFileFormat;
import io.github.ryanskonnord.lambdagoyf.deck.MtgoDeck;
import io.github.ryanskonnord.lambdagoyf.deck.MtgoDeckFormatter;
import io.github.ryanskonnord.lambdagoyf.deck.PreferenceBuilder;
import io.github.ryanskonnord.lambdagoyf.deck.preference.BasicLandPreferenceSequence;
import io.github.ryanskonnord.lambdagoyf.deck.preference.BasicLandReplacer;
import io.github.ryanskonnord.lambdagoyf.deck.preference.GroupReplacementWithAvailability;
import io.github.ryanskonnord.lambdagoyf.scryfall.ScryfallParser;
import io.github.ryanskonnord.util.MapCollectors;
import io.github.ryanskonnord.util.MultisetUtil;
import io.github.ryanskonnord.util.OrderingUtil;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static io.github.ryanskonnord.lambdagoyf.Environment.getDeckFilePath;
import static io.github.ryanskonnord.lambdagoyf.deck.preference.CardEditionPreferences.hasModernFrame;
import static io.github.ryanskonnord.lambdagoyf.deck.preference.CardEditionPreferences.hasWatermark;
import static io.github.ryanskonnord.lambdagoyf.deck.preference.CardEditionPreferences.isExpansionType;
import static io.github.ryanskonnord.lambdagoyf.deck.preference.CardEditionPreferences.isFromExpansionNamed;
import static io.github.ryanskonnord.lambdagoyf.deck.preference.CardEditionPreferences.newerFirst;
import static io.github.ryanskonnord.lambdagoyf.deck.preference.CardEditionPreferences.olderFirst;
import static io.github.ryanskonnord.lambdagoyf.deck.preference.CardEditionPreferences.onFrameStyle;
import static io.github.ryanskonnord.lambdagoyf.deck.preference.MtgoCardPreferences.hasBorderColor;
import static io.github.ryanskonnord.lambdagoyf.deck.preference.MtgoCardPreferences.hasFinish;
import static io.github.ryanskonnord.lambdagoyf.deck.preference.MtgoCardPreferences.hasMechanicalFrameUpdate;
import static io.github.ryanskonnord.lambdagoyf.deck.preference.MtgoCardPreferences.onEdition;
import static io.github.ryanskonnord.lambdagoyf.deck.preference.MtgoCardPreferences.onEditions;

public final class RyansMtgoDecks {

    private static Multiset<MtgoDeck.CardEntry> parseMyCollection(Spoiler spoiler) throws IOException {
        Path myCollectionPath = getDeckFilePath().resolve("Magic Online Collection.csv");
        try (Reader reader = Files.newBufferedReader(myCollectionPath)) {
            return MtgoDeckFormatter.parseCsv(spoiler, reader).asDeckObject().getAllCards();
        }
    }

    private static Multiset<Long> parseFavorites(Path path) {
        try (InputStream inputStream = Files.newInputStream(path)) {
            return MtgoDeckFormatter.parseDek(inputStream).getAllCards();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Comparator<MtgoCard> usingFavorites(Multiset<Long> favorites) {
        Objects.requireNonNull(favorites);
        return Comparator.comparingInt((MtgoCard card) -> -favorites.count(card.getMtgoId()));
    }

    private static final ImmutableSet<ExpansionType> NORMAL_RELEASES = Sets.immutableEnumSet(
            ExpansionType.CORE, ExpansionType.EXPANSION);

    private static final Comparator<MtgoCard> DEFAULT_OVERFLOW = new PreferenceBuilder<MtgoCard>()
            .prefer(hasFinish(Finish.NONFOIL))
            .prefer(onEdition(edition -> ExpansionType.isStandardRelease(edition.getExpansion().getType())))
            .prefer(hasBorderColor(BorderColor.BLACK))
            .addRule(onEditions(newerFirst()))
            .build();


    private static final ImmutableSet<String> FAVORITE_ARTISTS = ImmutableSet.of(
            "John Avon", "Steve Argyle", "Titus Lunter", "Alayna Danner");

    private static final ImmutableSet<String> ARTIST_BLACKLIST = ImmutableSet.of(
            "Harold McNeill", "Terese Nielsen", "Noah Bradley", "Seb McKinnon");

    public static Predicate<CardEdition> hasArtist(Iterable<String> artistNames) {
        ImmutableSet<String> artistNameSet = ImmutableSet.copyOf(artistNames);
        return (CardEdition edition) -> edition.getFaces().stream()
                .map(CardEditionFace::getArtist).flatMap(Optional::stream)
                .anyMatch(artistNameSet::contains);
    }

    private static Collection<MtgoCard> fromMtgoIds(Spoiler spoiler, long... ids) {
        return LongStream.of(ids)
                .mapToObj(id -> spoiler.lookUpByMtgoId(id)
                        .orElseThrow(() -> new IllegalArgumentException("Unmatched ID: " + id)))
                .collect(Collectors.toList());
    }

    private static Predicate<MtgoCard> hasMtgoId(long id) {
        return card -> id == card.getMtgoId();
    }

    private static Predicate<MtgoCard> hasMtgoId(long... ids) {
        Set<Long> idSet = ImmutableSet.copyOf(Longs.asList(ids));
        return card -> idSet.contains(card.getMtgoId());
    }

    private static Predicate<MtgoCard> isInIdCycle(long start) {
        return (MtgoCard card) -> {
            long diff = card.getMtgoId() - start;
            return (0 <= diff) & (diff <= 8) & (diff % 2 == 0);
        };
    }

    private static Predicate<MtgoCard> hasMtgoIdInRange(long startInclusive, long endInclusive) {
        Preconditions.checkArgument(startInclusive <= endInclusive);
        return (MtgoCard card) -> {
            long id = card.getMtgoId();
            return (startInclusive <= id) & (id <= endInclusive);
        };
    }

    private static Predicate<MtgoCard> isInNormalReleasesOrMasters() {
        return onEdition(isExpansionType(ImmutableSet.<ExpansionType>builder()
                .addAll(NORMAL_RELEASES)
                .add(ExpansionType.MASTERS)
                .build()));
    }

    private static Predicate<CardEdition> hasCollectorNumber(int... numbers) {
        ImmutableSet<Integer> numberSet = ImmutableSet.copyOf(Ints.asList(numbers));
        return edition -> numberSet.contains(edition.getCollectorNumber().getNumber());
    }

    private static Comparator<MtgoCard> orderForNormalReleases() {
        return onEditions(new PreferenceBuilder<CardEdition>()
                .prefer(hasWatermark())
                .addRule(olderFirst())
                .build());
    }

    public static Predicate<CardEdition> hasIllustrationThatDebutedIn(Predicate<Expansion> expansionPredicate) {
        return (CardEdition edition) -> {
            CardIllustration illustration = edition.getIllustration();
            for (CardEdition otherEdition : edition.getCard().getEditions()) {
                Expansion expansion = otherEdition.getExpansion();
                ExpansionType expansionType = expansion.getType().getEnum().orElse(null);
                if (illustration.equals(otherEdition.getIllustration())) {
                    if (NORMAL_RELEASES.contains(expansionType)) {
                        return false;
                    } else if (expansionPredicate.test(expansion)) {
                        return true;
                    }
                }
            }
            return false;
        };
    }

    public static Predicate<CardEdition> hasDebutIllustration() {
        return hasDebutIllustration(e -> true);
    }

    public static Predicate<CardEdition> hasDebutIllustration(Predicate<CardEdition> editionsToConsider) {
        return (CardEdition edition) -> {
            CardIllustration illustration = edition.getIllustration();
            for (CardEdition earlierEdition : edition.getEarlierReleases()) {
                if (editionsToConsider.test(earlierEdition) && earlierEdition.getIllustration().equals(illustration)) {
                    return false;
                }
            }
            return true;
        };
    }

    private static Predicate<Expansion> isMastersExceptMastersEdition() {
        return (Expansion expansion) ->
                expansion.getType().is(ExpansionType.MASTERS) && !expansion.getName().startsWith("Masters Edition");
    }

    private static final LocalDate PIONEER_THRESHOLD = LocalDate.parse("2012-10-05");


    // Personal taste
    private static final Predicate<MtgoCard> isClassicLand = hasMtgoId(27791, 27799, 27809, 27823, 27831);

    // Full-arts
    private static final Predicate<MtgoCard> isUnhingedLand = hasMtgoIdInRange(302, 311);
    private static final Predicate<MtgoCard> isUnstableLand = hasMtgoIdInRange(68066, 68075);
    private static final Predicate<MtgoCard> isNyxLand = hasMtgoIdInRange(79624, 79633);
    private static final Predicate<MtgoCard> isBfzLandWithZenArt = hasMtgoIdInRange(58788, 58797);

    // Promos
    private static final Predicate<MtgoCard> isApacPromoLand = hasMtgoIdInRange(231, 260);
    private static final Predicate<MtgoCard> isEuroPromoLand = hasMtgoIdInRange(261, 300);
    private static final Predicate<MtgoCard> isFirstMirrodinPromoLand = hasMtgoIdInRange(31991, 32000);
    private static final Predicate<MtgoCard> isKamigawaPromoLand = hasMtgoIdInRange(32003, 32012);
    private static final Predicate<MtgoCard> isRavnicaPromoLand = hasMtgoIdInRange(32019, 32028);
    private static final Predicate<MtgoCard> isMpsAlaraPromoLand = hasMtgoIdInRange(40040, 40049);
    private static final Predicate<MtgoCard> isMpsZendikarPromoLand = hasMtgoIdInRange(40050, 40059);
    private static final Predicate<MtgoCard> isMpsMirrodinPromoLand = hasMtgoIdInRange(40060, 40069);
    private static final Predicate<MtgoCard> isDominariaPromoLand = hasMtgoIdInRange(40092, 40101);
    private static final Predicate<MtgoCard> isJumpstartLand = hasMtgoIdInRange(53875, 81909);

    private static final BasicLandReplacer<MtgoCard> mh3LandscapeBasics(Spoiler spoiler) {
        return BasicLandReplacer.fromVersionsChosenRandomly(fromMtgoIds(spoiler,
                79044, 79046, 79048, 79050, 79052, 126477, 126593, 126595, 126597,
                126599, 126601, 126603, 126605, 126607, 126609, 126611, 127361));
    }

    private static final BasicLandReplacer<MtgoCard> mh3FullFrameBasics(Spoiler spoiler) {
        return BasicLandReplacer.fromVersions(fromMtgoIds(spoiler,
                59323, 59605, 72872, 72874, 72876, 72878, 72880, 126613, 126615, 126617, 126619, 126621, 126623));
    }

    private static Predicate<MtgoCard> isBasicLandFromSet(String expansionName, String artist) {
        return onEdition(e -> e.getCard().getMainTypeLine().is(CardSupertype.BASIC) && e.getExpansion().isNamed(expansionName)
                && e.hasArtist(artist));
    }

    private static Predicate<CardEdition> fromExpansionWithIllustrationFrom(String editionExpansion, String debutExpansion) {
        Predicate<CardEdition> hasIllustrationFrom = (CardEdition edition) -> {
            CardIllustration illustration = edition.getIllustration();
            return edition.getCard().getEditions().stream().anyMatch((CardEdition otherEdition) ->
                    otherEdition.getIllustration().equals(illustration) && otherEdition.getExpansion().isNamed(debutExpansion));
        };
        return isFromExpansionNamed(editionExpansion).and(hasIllustrationFrom);
    }

    private static BasicLandPreferenceSequence<MtgoCard> getBasicLandPreference(
            BasicLandPreferenceSequence.Context<MtgoCard> context,
            DeckFormatDirectory format) {
        BasicLandPreferenceSequence.Builder<MtgoCard> builder = context.builder();
        builder.add(usePromoBasicLands(format).and(hasFinish(Finish.NONFOIL)));
        builder.add(isUnstableLand.and(hasFinish(Finish.NONFOIL)))
                .add(isUnhingedLand.and(hasFinish(Finish.NONFOIL)));
        return builder.build();
    }

    private static Predicate<MtgoCard> useModernHorizonsSnowLands() {
        return (MtgoCard version) -> {
            TypeLine typeLine = version.getCard().getMainTypeLine();
            return typeLine.is(CardSupertype.BASIC) && typeLine.is(CardSupertype.SNOW)
                    && version.getEdition().getExpansion().isNamed("MH1");
        };
    }

    private static Predicate<MtgoCard> useCoreSetBasicLands(DeckFormatDirectory format) {
        Predicate<CardEdition> predicate = switch (format) {
            case PIONEER -> isFromExpansionNamed("M15").and(hasDebutIllustration());
            case MODERN -> isFromExpansionNamed("M14").and(e -> e.hasArtist("Jonas De Ro"));
            case LEGACY -> fromExpansionWithIllustrationFrom("10E", "Invasion");
            case VINTAGE -> fromExpansionWithIllustrationFrom("10E", "Urza's Saga");
            case PAUPER ->
                // isFromExpansionNamed("M10").and(hasCollectorNumber(230, 235, 238, 243, 247));
                // isFromExpansionNamed("M10").and(hasCollectorNumber(231, 237, 240, 244, 248));
                    isFromExpansionNamed("M12").and(hasCollectorNumber(232, 235, 240, 242, 247));
        };
        return onEdition(predicate).and(hasFinish(Finish.NONFOIL));
    }

    private static Predicate<MtgoCard> usePromoBasicLands(DeckFormatDirectory format) {
        Predicate<MtgoCard> predicate = switch (format) {
            case PIONEER -> isRavnicaPromoLand;
            case MODERN -> isMpsAlaraPromoLand;
            case LEGACY -> isFirstMirrodinPromoLand;
            case VINTAGE -> isMpsMirrodinPromoLand;
            case PAUPER -> isMpsZendikarPromoLand;
        };
        return predicate.and(hasFinish(Finish.NONFOIL));
    }

    private static Comparator<MtgoCard> modernPreference(DeckFormatDirectory format) {
        return new PreferenceBuilder<MtgoCard>()
                .addRule(usingFavorites(format.getFavorites()))
                .prefer(onEdition(hasArtist(ARTIST_BLACKLIST).negate()))
                .prefer(onEdition(isFromExpansionNamed("Amonkhet Invocations", "Tempest Remastered", "Coldsnap Theme Decks")).negate())
                .prefer(hasBorderColor(BorderColor.BLACK).or(hasBorderColor(BorderColor.BORDERLESS)))
                .prefer(hasFinish(Finish.NONFOIL))
                .prefer(onEdition(onFrameStyle(frameStyle -> frameStyle != FrameStyle.FUTURE)))
                .prefer(hasMechanicalFrameUpdate())
                .prefer(onEdition(c -> format == DeckFormatDirectory.PIONEER && c.getReleaseDate().compareTo(PIONEER_THRESHOLD) >= 0))
                .prefer(onEdition(hasModernFrame()))
                .prefer(onEdition(isExpansionType(ExpansionType.PROMO).negate()))
                .prefer(onEdition(hasWatermark(w -> w.is(Watermark.SET))))
                .prefer(onEdition(hasIllustrationThatDebutedIn(isMastersExceptMastersEdition())))
                .prefer(onEdition(hasArtist(FAVORITE_ARTISTS)))
                .prefer(onEdition(isExpansionType(ExpansionType.MASTERS).negate()))
                .preferWithRule(isInNormalReleasesOrMasters(), orderForNormalReleases())
                .addRule(onEditions(newerFirst()))
                .build();
    }

    private static Comparator<CardEdition> withExpansionPrestige(String... names) {
        Map<String, Integer> indexMap = OrderingUtil.toIndexMap(Arrays.asList(names));
        return Comparator.comparingInt((CardEdition cardEdition) -> {
            Integer index = indexMap.get(cardEdition.getExpansion().getName());
            return index == null ? Integer.MAX_VALUE : index;
        });
    }


    private static Comparator<MtgoCard> eternalPreference(DeckFormatDirectory format) {
        return new PreferenceBuilder<MtgoCard>()
                .addRule(usingFavorites(format.getFavorites()))
                .prefer(onEdition(hasArtist(ARTIST_BLACKLIST).negate()))
                .prefer(onEdition(hasModernFrame()).negate().and(hasFinish(Finish.FOIL)).negate())
                .prefer(onEdition(isFromExpansionNamed("Amonkhet Invocations", "Coldsnap Theme Decks")).negate())
                .prefer(onEdition(isFromExpansionNamed("Kaladesh Inventions", "Ultimate Box Topper")))
                .prefer(hasBorderColor(BorderColor.BLACK).or(hasBorderColor(BorderColor.BORDERLESS)))
                .prefer(onEdition(onFrameStyle(frameStyle -> frameStyle != FrameStyle.FUTURE)))
                .prefer(hasMechanicalFrameUpdate())
                .prefer(onEdition(hasArtist(FAVORITE_ARTISTS)))
                .addRule(onEditions(withExpansionPrestige("Masters 25", "Eternal Masters", "Modern Horizons", "Modern Horizons 2", "Vintage Masters")))
                .prefer(onEdition(hasIllustrationThatDebutedIn(isMastersExceptMastersEdition()
                        .or(expansion -> expansion.getType().is(ExpansionType.PROMO)))))
                .prefer(hasFinish(Finish.FOIL).and(onEdition(hasModernFrame())))
                .preferWithRule(isInNormalReleasesOrMasters(), orderForNormalReleases())
                .build();
    }

    private static <V extends CardVersion> UnaryOperator<Deck<V>> createTransformationFrom(
            CardVersionExtractor<V> extractor,
            Predicate<V> predicate) {
        Objects.requireNonNull(extractor);
        Objects.requireNonNull(predicate);
        return (Deck<V> deck) -> deck.transform((V oldVersion) -> {
            List<V> matchedVersions = extractor.fromCard(oldVersion.getCard())
                    .filter(predicate).collect(ImmutableList.toImmutableList());
            return switch (matchedVersions.size()) {
                case 0 -> oldVersion;
                case 1 -> matchedVersions.get(0);
                default -> throw new RuntimeException("Predicate matched multiple versions: " + matchedVersions);
            };
        });
    }

    private static UnaryOperator<Deck<MtgoCard>> adjustForAbnormalBasics() {
        List<String> bfzArtists = ImmutableList.of("Sam Burley", "Adam Paquette", "Tianhua X");
        List<UnaryOperator<Deck<MtgoCard>>> modifiers = bfzArtists.stream()
                .map((String artist) -> {
                    Predicate<MtgoCard> replacement = isBasicLandFromSet("BFZ", artist).and(hasFinish(Finish.NONFOIL));
                    return createTransformationFrom(CardVersionExtractor.getMtgoCards(), replacement);
                })
                .collect(Collectors.toList());

        return (Deck<MtgoCard> deck) -> {
            boolean deckContainsAbnormalBasics = deck.getAllCards().elementSet().stream()
                    .map(MtgoCard::getCard).distinct()
                    .anyMatch((Card c) -> c.getMainTypeLine().is(CardSupertype.BASIC)
                            && !Color.getBasicLandTypes().contains(c.getMainName()));
            if (!deckContainsAbnormalBasics) return deck;

            DeckRandomChoice chooser = DeckRandomChoice.withSalt(0x80445847e1d2f2c3L).forDeck(deck);
            UnaryOperator<Deck<MtgoCard>> modifier = chooser.choose(modifiers);
            return modifier.apply(deck);
        };
    }

    private static UnaryOperator<Deck<MtgoDeck.CardEntry>> useKaldheimSnowLands(Spoiler spoiler) {
        ImmutableMap<Card, MtgoCard> kaldheimNonsnowBasics = Color.getBasicLandTypes().stream()
                .map(n -> spoiler.lookUpByName(n).orElseThrow())
                .collect(MapCollectors.<Card>collecting()
                        .memoizing(c -> c.getEditions().stream().filter(e -> e.getExpansion().isNamed("Kaldheim"))
                                .flatMap(e -> e.getMtgoCard(Finish.NONFOIL).stream())
                                .collect(MoreCollectors.onlyElement()))
                        .unique().toImmutableMap());
        ImmutableMultimap<Card, MtgoCard> kaldheimSnowBasics = Color.getBasicLandTypes().stream()
                .map(n -> spoiler.lookUpByName("Snow-Covered " + n).orElseThrow())
                .collect(MapCollectors.<Card>collecting()
                        .flattening(c -> c.getEditions().stream().filter(e -> e.getExpansion().isNamed("Kaldheim"))
                                .flatMap(e -> e.getMtgoCard(Finish.NONFOIL).stream()))
                        .toImmutableListMultimap());

        return (Deck<MtgoDeck.CardEntry> deck) -> {
            Collection<MtgoCard> snowLandsInDeck = deck.getAllCards().elementSet().stream()
                    .flatMap(e -> e.getVersion().stream())
                    .filter((MtgoCard c) -> kaldheimSnowBasics.containsKey(c.getCard()))
                    .collect(Collectors.toList());
            if (snowLandsInDeck.isEmpty()) return deck;
            return deck.transform((MtgoDeck.CardEntry entry) -> {
                Optional<MtgoCard> version = entry.getVersion();
                if (version.isEmpty()) return entry;
                Card card = version.get().getCard();
                MtgoCard nonsnow = kaldheimNonsnowBasics.get(card);
                if (nonsnow != null) return new MtgoDeck.CardEntry(nonsnow);
                Collection<MtgoCard> snow = kaldheimSnowBasics.get(card);
                if (snow.isEmpty()) return entry;
                return new MtgoDeck.CardEntry(snow.stream()
                        .filter(s -> s.getEdition().hasArtist("Adam Paquette") == (snowLandsInDeck.size() > 1))
                        .collect(MoreCollectors.onlyElement()));
            });
        };
    }

    private static UnaryOperator<Deck<MtgoDeck.CardEntry>> useSecretLairLands(ToIntFunction<MtgoDeck.CardEntry> collectionAvailability,
                                                                              String... artists) {
        ImmutableSet<String> artistSet = ImmutableSet.copyOf(artists);
        Predicate<MtgoCard> secretLairLands = (MtgoCard mtgoCard) -> {
            CardEdition edition = mtgoCard.getEdition();
            return mtgoCard.getFinish() == Finish.NONFOIL
                    && edition.getCard().getMainTypeLine().is(CardType.LAND)
                    && edition.getExpansion().isNamed("SLD")
                    && edition.getArtists().anyMatch(artistSet::contains);
        };


        return new GroupReplacementWithAvailability<>(
                CardVersionExtractor.getMtgoCards(), MtgoDeck.CardEntry::new, secretLairLands, collectionAvailability, 0x727088cf424f3b18L);
    }

    private static UnaryOperator<Deck<MtgoDeck.CardEntry>> transformTo(Predicate<MtgoCard> predicate,
                                                                       ToIntFunction<MtgoDeck.CardEntry> collectionAvailability) {
        Objects.requireNonNull(predicate);
        Objects.requireNonNull(collectionAvailability);
        return (Deck<MtgoDeck.CardEntry> deck) -> {
            DeckRandomChoice choice = DeckRandomChoice.withSalt(0x5b114dfa7866f512L)
                    .forDeck(deck.flatTransform(DeckElement::getVersion));
            return deck.transformCards((Multiset.Entry<MtgoDeck.CardEntry> entry) -> {
                int count = entry.getCount();
                MtgoDeck.CardEntry element = entry.getElement();
                Optional<MtgoCard> version = element.getVersion();
                if (version.isEmpty()) return MultisetUtil.ofSingleEntry(element, count);
                Card card = version.get().getCard();
                List<MtgoCard> versions = CardVersionExtractor.getMtgoCards().fromCard(card)
                        .filter(c -> collectionAvailability.applyAsInt(new MtgoDeck.CardEntry(c)) >= count && predicate.test(c))
                        .collect(Collectors.toList());
                if (versions.isEmpty()) return null;
                MtgoCard chosenVersion = choice.forCard(card).choose(versions);
                return MultisetUtil.ofSingleEntry(new MtgoDeck.CardEntry(chosenVersion), count);
            });
        };
    }


    private static final Path FAVORITE_ETERNAL = getDeckFilePath().resolve("Favorite Eternal.dek");

    private static enum DeckFormatDirectory {
        PIONEER("Pioneer", "P", Format.PIONEER),
        MODERN("Modern", "M", Format.MODERN),
        PAUPER("Pauper", null, Format.PAUPER),
        LEGACY("Legacy", "L", Format.LEGACY),
        VINTAGE("Vintage", "V", Format.VINTAGE);

        private final String directoryName;
        private final String formatTag;
        private final Format format;

        DeckFormatDirectory(String directoryName, String formatTag, Format format) {
            this.directoryName = Objects.requireNonNull(directoryName);
            this.formatTag = Optional.ofNullable(formatTag).orElse(directoryName.substring(0, 3));
            this.format = Objects.requireNonNull(format);
        }

        public Multiset<Long> getFavorites() {
            Path favoritesPath = getDeckFilePath().resolve(String.format("Favorite %s.dek",
                    switch (this) {
                        case MODERN -> "Modern";
                        case PIONEER -> "Pioneer";
                        default -> "Eternal";
                    }));
            return parseFavorites(favoritesPath);
        }

        public Comparator<? super MtgoCard> getPreference() {
            return switch (this) {
                case LEGACY, VINTAGE -> eternalPreference(this);
                default -> modernPreference(this);
            };
        }

        public boolean allowsSnow() {
            return true;
        }
    }

    private static boolean isInNetdeckFolder(Path path) {
        for (Path part : path) {
            if (part.toString().equals("Netdecks")) {
                return false;
            }
        }
        return true;
    }

    private static String asScryfallLink(Card card) {
        String encodedName;
        try {
            encodedName = URLEncoder.encode(card.getMainName(), Charsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return String.format("https://scryfall.com/search?q=!\"%s\"", encodedName);
    }

    private static void debug(Spoiler spoiler, Comparator<? super MtgoCard> comparator, String cardName) {
        Card card = spoiler.lookUpByName(cardName).orElseThrow(IllegalArgumentException::new);
        card.getEditions().stream()
                .flatMap(CardEdition::getMtgoCards).sorted(comparator)
                .forEachOrdered(System.out::println);
    }

    private static final CardEdition findIllustrationDebut(CardEdition edition) {
        CardIllustration illustration = edition.getIllustration();
        return edition.getCard().getEditions().stream()
                .filter(e -> e.getIllustration().equals(illustration))
                .findFirst().orElseThrow();
    }

    private static Function<Card, Stream<MtgoDeck.CardEntry>> getFallbackFunction(Multiset<MtgoDeck.CardEntry> collection) {
        Multimap<String, MtgoDeck.CardEntry> fallbackVersions = collection.elementSet().stream()
                .filter(e -> e.getVersion().isEmpty())
                .sorted(Comparator.comparing(MtgoDeck.CardEntry::getId))
                .collect(MapCollectors.<MtgoDeck.CardEntry>collecting()
                        .indexing(e -> CardNames.normalize(e.getName()))
                        .grouping().toImmutableSetMultimap());
        return (Card card) -> fallbackVersions.get(CardNames.normalize(card.getMainName())).stream();
    }

    private static UnaryOperator<Deck<MtgoDeck.CardEntry>> adaptTransformation(UnaryOperator<Deck<MtgoCard>> transformation) {
        Objects.requireNonNull(transformation);
        return (Deck<MtgoDeck.CardEntry> deck) -> {
            Deck<MtgoCard> coerced = MtgoDeck.coerce(deck);
            Deck<MtgoCard> transformed = transformation.apply(coerced);
            return transformed.transform(MtgoDeck.CardEntry::new);
        };
    }

    public static void main(String[] args) throws Exception {
        Spoiler spoiler = ScryfallParser.createSpoiler();

        SnowConversion snowConversion = new SnowConversion(spoiler);
        WinterCheer winterCheer = new WinterCheer(Clock.systemDefaultZone(), snowConversion);
        Multiset<MtgoDeck.CardEntry> myCollection = parseMyCollection(spoiler);
        ToIntFunction<MtgoDeck.CardEntry> collectionAvailability = myCollection::count;

        BasicLandPreferenceSequence.Context<MtgoCard> basicLandPreferenceContext = new BasicLandPreferenceSequence.Context<>(
                spoiler, CardVersionExtractor.getMtgoCards(), c -> myCollection.count(new MtgoDeck.CardEntry(c)));

        Path constructedDirectory = getDeckFilePath().resolve("Constructed");
        Path allDirectory = constructedDirectory.resolve("All");
        Files.createDirectories(allDirectory);
        for (DeckFormatDirectory directory : EnumSet.allOf(DeckFormatDirectory.class)) {
            DeckConstructor<MtgoCard, MtgoDeck.CardEntry> deckConstructor = DeckConstructor.createForMtgo()
                    .setAvailableCollection(myCollection)
                    .withFallback(getFallbackFunction(myCollection))

                    .withPreferenceOrder().override(directory.getPreference())
                    .withOverflowOver().override(DEFAULT_OVERFLOW)
                    .addDeckTransformation(deck -> CompanionLegality.addMissingCompanion(spoiler, deck, directory.format))
//                    .addDeckTransformation(snowConversion::convertWithFieldBluff)
//                    .addDeckTransformation(snowConversion::removeSuperfluousSnowLands)
                    .addDeckTransformation(directory.allowsSnow() ? winterCheer::convertSeasonally : UnaryOperator.identity())
                    .addOutputTransformation(d -> getBasicLandPreference(basicLandPreferenceContext, directory).apply(d, MtgoDeck.CardEntry::new))
//                    .addVersionTransformation(adjustForAbnormalBasics())
//                    .addVersionTransformation(useKaldheimSnowLands(spoiler))
                    .addOutputTransformation(transformTo(useCoreSetBasicLands(directory), collectionAvailability))
                    .addOutputTransformation(directory == DeckFormatDirectory.PIONEER
                            ? useKaldheimSnowLands(spoiler)
                            : transformTo(useModernHorizonsSnowLands(), collectionAvailability))
                    .addOutputTransformation(useSecretLairLands(collectionAvailability, "Alayna Danner"))

                    .addOutputTransformation(directory == DeckFormatDirectory.MODERN
                            ? adaptTransformation(mh3LandscapeBasics(spoiler))
                            : UnaryOperator.identity())
                    .build();
            Path root = constructedDirectory.resolve(directory.directoryName);
            Files.createDirectories(root);
            RyansMtgaDecks.stripMtgGoldfishPrefixes(root);

            for (DeckSeeker.Entry deckEntry : DeckSeeker.seekAll(root, directory.formatTag)) {
                if (!deckEntry.hasFile(DeckFileFormat.SOURCE) || deckEntry.hasFile(DeckFileFormat.DEK)) {
                    continue;
                }
                try {
                    Deck<String> deckWithCardNames;
                    try (Reader reader = Files.newBufferedReader(deckEntry.getPath(DeckFileFormat.SOURCE))) {
                        deckWithCardNames = MtgoDeckFormatter.parseTxt(reader);
                    }
                    Deck<Card> unversionedDeck = MtgoDeckFormatter.createDeckFromCardNames(spoiler, deckWithCardNames);
                    Deck<MtgoDeck.CardEntry> versionedDeck = deckConstructor.createDeck(unversionedDeck);
                    Path outputPath = deckEntry.getPath(DeckFileFormat.DEK);
                    Files.createDirectories(outputPath.getParent());
                    try (OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(outputPath))) {
                        MtgoDeckFormatter.writeDek(outputStream, versionedDeck);
                    }
                    Files.copy(outputPath, allDirectory.resolve(outputPath.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception e) {
                    System.err.println("\nError on deck file: " + deckEntry.getName());
                    e.printStackTrace(System.err);
                }
            }
        }
    }

}
