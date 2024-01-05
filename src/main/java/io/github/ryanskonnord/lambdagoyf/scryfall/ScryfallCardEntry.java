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

package io.github.ryanskonnord.lambdagoyf.scryfall;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import com.google.common.primitives.ImmutableLongArray;
import io.github.ryanskonnord.util.MapCollectors;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ScryfallCardEntry implements ScryfallCardFace {

    private final Optional<ImmutableList<ImmutableMap<String, String>>> allParts;
    private final OptionalLong arenaId;
    private final String artist;
    private final Optional<ImmutableList<UUID>> artistIds;
    private final boolean booster;
    private final String borderColor;
    private final Optional<UUID> cardBackId;
    private final Optional<ImmutableList<ScryfallCardFaceEntry>> cardFaces;
    private final OptionalLong cardmarketId;
    private final double cmc;
    private final String collectorNumber;
    private final ImmutableList<String> colorIdentity;
    private final Optional<ImmutableList<String>> colorIndicator;
    private final Optional<ImmutableList<String>> colors;
    private final Optional<Boolean> contentWarning;
    private final boolean digital;
    private final OptionalLong edhrecRank;
    private final ImmutableList<String> finishes;
    private final Optional<String> flavorName;
    private final Optional<String> flavorText;
    private final boolean foil;
    private final String frame;
    private final ImmutableList<String> frameEffects;
    private final boolean fullArt;
    private final ImmutableList<String> games;
    private final Optional<String> handModifier;
    private final boolean highresImage;
    private final UUID id;
    private final Optional<UUID> illustrationId;
    private final String imageStatus;
    private final Optional<ImmutableMap<String, String>> imageUris;
    private final ImmutableList<String> keywords;
    private final String lang;
    private final String layout;
    private final ImmutableMap<String, String> legalities;
    private final Optional<String> lifeModifier;
    private final Optional<String> loyalty;
    private final Optional<String> manaCost;
    private final OptionalLong mtgoFoilId;
    private final OptionalLong mtgoId;
    private final ImmutableLongArray multiverseIds;
    private final String name;
    private final boolean nonfoil;
    private final String object;
    private final UUID oracleId;
    private final Optional<String> oracleText;
    private final boolean oversized;
    private final Optional<String> power;
    private final Optional<ImmutableMap<String, String>> preview;
    private final ImmutableMap<String, String> prices;
    private final Optional<String> printedName;
    private final Optional<String> printedText;
    private final Optional<String> printedTypeLine;
    private final URI printsSearchUri;
    private final ImmutableList<String> producedMana;
    private final boolean promo;
    private final ImmutableList<String> promoTypes;
    private final String rarity;
    private final ImmutableMap<String, String> relatedUris;
    private final LocalDate releasedAt;
    private final boolean reprint;
    private final boolean reserved;
    private final URI rulingsUri;
    private final URI scryfallSetUri;
    private final URI scryfallUri;
    private final Optional<String> securityStamp;
    private final String set;
    private final UUID setId;
    private final String setName;
    private final URI setSearchUri;
    private final String setType;
    private final URI setUri;
    private final boolean storySpotlight;
    private final OptionalLong tcgplayerId;
    private final OptionalLong tcgplayerEtchedId;
    private final boolean textless;
    private final Optional<String> toughness;
    private final String typeLine;
    private final URI uri;
    private final boolean variation;
    private final Optional<UUID> variationOf;
    private final Optional<String> watermark;

    private static final UUID CARD_BACK_FLYWEIGHT = new UUID(0x0aeebaf58c7d4636L, 0x9e828c27447861f7L);

    ScryfallCardEntry(Map<String, ?> data, Consumer<String> extraKeyConsumer) {
        data = mungeForHiwtyl(data);
        allParts = Optional.ofNullable((List<?>) data.remove("all_parts"))
                .map(ScryfallParser.parseObjectList(ScryfallParser::parseStringMap));
        arenaId = ScryfallParser.parseNumber((Double) data.remove("arena_id"));
        artist = Objects.requireNonNull((String) data.remove("artist"));
        artistIds = Optional.ofNullable((List<?>) data.remove("artist_ids")).map(value ->
                ScryfallParser.parseStrings(value).stream().map(UUID::fromString).collect(ImmutableList.toImmutableList()));
        booster = Objects.requireNonNull((Boolean) data.remove("booster"));
        borderColor = Objects.requireNonNull((String) data.remove("border_color"));
        cardBackId = Optional.ofNullable((String) data.remove("card_back_id"))
                .map(id -> CARD_BACK_FLYWEIGHT.toString().equals(id) ? CARD_BACK_FLYWEIGHT : UUID.fromString(id));
        cardFaces = Optional.ofNullable((List<?>) data.remove("card_faces"))
                .map(ScryfallParser.parseObjectList(ScryfallCardFaceEntry::new));
        cardmarketId = ScryfallParser.parseNumber(((Double) data.remove("cardmarket_id")));
        cmc = Objects.requireNonNull((Double) data.remove("cmc"));
        collectorNumber = Objects.requireNonNull((String) data.remove("collector_number"));
        colorIdentity = ScryfallParser.parseStrings((List<?>) data.remove("color_identity"));
        colorIndicator = Optional.ofNullable((List<?>) data.remove("color_indicator")).map(ScryfallParser::parseStrings);
        colors = Optional.ofNullable((List<?>) data.remove("colors")).map(ScryfallParser::parseStrings);
        contentWarning = Optional.ofNullable((Boolean) data.remove("content_warning"));
        digital = Objects.requireNonNull((Boolean) data.remove("digital"));
        edhrecRank = ScryfallParser.parseNumber((Double) data.remove("edhrec_rank"));
        finishes = ScryfallParser.parseStrings((List<?>) data.remove("finishes"));
        flavorText = Optional.ofNullable((String) data.remove("flavor_text"));
        foil = Objects.requireNonNull((Boolean) data.remove("foil"));
        frame = Objects.requireNonNull((String) data.remove("frame"));
        frameEffects = ScryfallParser.parseOptionalStrings((List<?>) data.remove("frame_effects"));
        fullArt = Objects.requireNonNull((Boolean) data.remove("full_art"));
        games = ScryfallParser.parseStrings((List<?>) data.remove("games"));
        handModifier = Optional.ofNullable((String) data.remove("hand_modifier"));
        highresImage = Objects.requireNonNull((Boolean) data.remove("highres_image"));
        id = UUID.fromString((String) data.remove("id"));
        illustrationId = Optional.ofNullable((String) data.remove("illustration_id")).map(UUID::fromString);
        imageStatus = Objects.requireNonNull((String) data.remove("image_status"));
        imageUris = Optional.ofNullable((Map<?, ?>) data.remove("image_uris")).map(ScryfallParser::parseStringMap);
        keywords = ScryfallParser.parseStrings((List<?>) data.remove("keywords"));
        lang = Objects.requireNonNull((String) data.remove("lang"));
        layout = Objects.requireNonNull((String) data.remove("layout"));
        legalities = ScryfallParser.parseStringMap((Map<?, ?>) data.remove("legalities"));
        lifeModifier = Optional.ofNullable((String) data.remove("life_modifier"));
        loyalty = Optional.ofNullable((String) data.remove("loyalty"));
        manaCost = Optional.ofNullable((String) data.remove("mana_cost"));
        mtgoFoilId = ScryfallParser.parseNumber((Double) data.remove("mtgo_foil_id"));
        mtgoId = ScryfallParser.parseNumber((Double) data.remove("mtgo_id"));
        multiverseIds = ScryfallParser.parseNumbers((List<?>) data.remove("multiverse_ids"));
        name = Objects.requireNonNull((String) data.remove("name"));
        nonfoil = Objects.requireNonNull((Boolean) data.remove("nonfoil"));
        object = Objects.requireNonNull((String) data.remove("object"));
        oracleId = UUID.fromString((String) data.remove("oracle_id"));
        oracleText = Optional.ofNullable((String) data.remove("oracle_text"));
        oversized = Objects.requireNonNull((Boolean) data.remove("oversized"));
        power = Optional.ofNullable((String) data.remove("power"));
        printedName = Optional.ofNullable((String) data.remove("printed_name"));
        flavorName = Optional.ofNullable((String) data.remove("flavor_name"));
        preview = Optional.ofNullable((Map<?, ?>) data.remove("preview")).map(ScryfallParser::parseStringMap);
        prices = ScryfallParser.parseStringMap((Map<?, ?>) data.remove("prices"));
        printedText = Optional.ofNullable((String) data.remove("printed_text"));
        printedTypeLine = Optional.ofNullable((String) data.remove("printed_type_line"));
        printsSearchUri = URI.create((String) data.remove("prints_search_uri"));
        producedMana = ScryfallParser.parseOptionalStrings((List<?>) data.remove("produced_mana"));
        promo = Objects.requireNonNull((Boolean) data.remove("promo"));
        promoTypes = ScryfallParser.parseOptionalStrings((List<?>) data.remove("promo_types"));
        rarity = Objects.requireNonNull((String) data.remove("rarity"));
        relatedUris = ScryfallParser.parseStringMap((Map<?, ?>) data.remove("related_uris"));
        releasedAt = LocalDate.parse((String) data.remove("released_at"));
        reprint = Objects.requireNonNull((Boolean) data.remove("reprint"));
        reserved = Objects.requireNonNull((Boolean) data.remove("reserved"));
        rulingsUri = URI.create((String) data.remove("rulings_uri"));
        scryfallSetUri = URI.create((String) data.remove("scryfall_set_uri"));
        scryfallUri = URI.create((String) data.remove("scryfall_uri"));
        securityStamp = Optional.ofNullable((String) data.remove("security_stamp"));
        set = Objects.requireNonNull((String) data.remove("set"));
        setId = UUID.fromString((String) data.remove("set_id"));
        setName = Objects.requireNonNull((String) data.remove("set_name"));
        setSearchUri = URI.create((String) data.remove("set_search_uri"));
        setType = Objects.requireNonNull((String) data.remove("set_type"));
        setUri = URI.create((String) data.remove("set_uri"));
        storySpotlight = Objects.requireNonNull((Boolean) data.remove("story_spotlight"));
        tcgplayerId = ScryfallParser.parseNumber((Double) data.remove("tcgplayer_id"));
        tcgplayerEtchedId = ScryfallParser.parseNumber((Double) data.remove("tcgplayer_etched_id"));
        textless = Objects.requireNonNull((Boolean) data.remove("textless"));
        toughness = Optional.ofNullable((String) data.remove("toughness"));
        typeLine = Objects.requireNonNull((String) data.remove("type_line"));
        uri = URI.create((String) data.remove("uri"));
        variation = Objects.requireNonNull((Boolean) data.remove("variation"));
        variationOf = Optional.ofNullable((String) data.remove("variation_of")).map(UUID::fromString);
        watermark = Optional.ofNullable((String) data.remove("watermark"));

        if (!data.isEmpty() && extraKeyConsumer != null) {
            for (String extraKey : data.keySet()) {
                extraKeyConsumer.accept(extraKey);
            }
        }
    }

    private static boolean isHiwtylDfc(Map<String, ?> data) {
        List<?> cardFaces = (List<?>) data.get("card_faces");
        if (cardFaces == null || cardFaces.size() != 2) return false;
        Set<Optional<String>> distinctOracleIds = cardFaces.stream()
                .map((Object part) -> Optional.ofNullable((String) ((Map<String, ?>) part).get("oracle_id")))
                .collect(Collectors.toSet());
        return distinctOracleIds.size() == 1 && Iterables.getOnlyElement(distinctOracleIds).isPresent();
    }

    private static Map<String, ?> mungeForHiwtyl(Map<String, ?> data) {
        if (!isHiwtylDfc(data)) return data;

        List<?> cardFaces = (List<?>) data.get("card_faces");
        ImmutableSetMultimap<String, Object> mergedValues = cardFaces.stream()
                .flatMap((Object part) -> ((Map<String, Object>) part).entrySet().stream())
                .collect(MapCollectors.<String, Object>collectingEntries().grouping().toImmutableSetMultimap());

        Map<String, Object> munged = Maps.newHashMapWithExpectedSize(data.size() + mergedValues.keySet().size());
        munged.putAll(data);
        for (Map.Entry<String, Set<Object>> entry : Multimaps.asMap(mergedValues).entrySet()) {
            munged.put(entry.getKey(), entry.getValue().iterator().next());
        }
        munged.remove("card_faces");
        return munged;
    }

    public Stream<ScryfallCardFace> getFaceStream() {
        return getCardFaces()
                .map(faceEntries -> faceEntries.stream().map(ScryfallCardFace.class::cast))
                .orElseGet(() -> Stream.of(ScryfallCardEntry.this));
    }


    public Optional<ImmutableList<ImmutableMap<String, String>>> getAllParts() {
        return allParts;
    }

    public OptionalLong getArenaId() {
        return arenaId;
    }

    @Override
    public Optional<String> getArtist() {
        return Optional.of(artist);
    }

    public Optional<ImmutableList<UUID>> getArtistIds() {
        return artistIds;
    }

    public boolean isBooster() {
        return booster;
    }

    public String getBorderColor() {
        return borderColor;
    }

    public Optional<UUID> getCardBackId() {
        return cardBackId;
    }

    public Optional<ImmutableList<ScryfallCardFaceEntry>> getCardFaces() {
        return cardFaces;
    }

    public OptionalLong getCardmarketId() {
        return cardmarketId;
    }

    public double getCmc() {
        return cmc;
    }

    public String getCollectorNumber() {
        return collectorNumber;
    }

    public ImmutableList<String> getColorIdentity() {
        return colorIdentity;
    }

    @Override
    public Optional<ImmutableList<String>> getColorIndicator() {
        return colorIndicator;
    }

    @Override
    public Optional<ImmutableList<String>> getColors() {
        return colors;
    }

    public Optional<Boolean> getContentWarning() {
        return contentWarning;
    }

    public boolean isDigital() {
        return digital;
    }

    public OptionalLong getEdhrecRank() {
        return edhrecRank;
    }

    public ImmutableList<String> getFinishes() {
        return finishes;
    }

    @Override
    public Optional<String> getFlavorName() {
        return flavorName;
    }

    @Override
    public Optional<String> getFlavorText() {
        return flavorText;
    }

    public boolean isFoil() {
        return foil;
    }

    public String getFrame() {
        return frame;
    }

    public ImmutableList<String> getFrameEffects() {
        return frameEffects;
    }

    public boolean isFullArt() {
        return fullArt;
    }

    public ImmutableList<String> getGames() {
        return games;
    }

    public Optional<String> getHandModifier() {
        return handModifier;
    }

    public boolean isHighresImage() {
        return highresImage;
    }

    public UUID getId() {
        return id;
    }

    @Override
    public Optional<UUID> getIllustrationId() {
        return illustrationId;
    }

    public String getImageStatus() {
        return imageStatus;
    }

    @Override
    public Optional<ImmutableMap<String, String>> getImageUris() {
        return imageUris;
    }

    public ImmutableList<String> getKeywords() {
        return keywords;
    }

    public String getLang() {
        return lang;
    }

    public String getLayout() {
        return layout;
    }

    public ImmutableMap<String, String> getLegalities() {
        return legalities;
    }

    public Optional<String> getLifeModifier() {
        return lifeModifier;
    }

    @Override
    public Optional<String> getLoyalty() {
        return loyalty;
    }

    @Override
    public Optional<String> getManaCost() {
        return manaCost;
    }

    public OptionalLong getMtgoFoilId() {
        return mtgoFoilId;
    }

    public OptionalLong getMtgoId() {
        return mtgoId;
    }

    public ImmutableLongArray getMultiverseIds() {
        return multiverseIds;
    }

    @Override
    public String getName() {
        return name;
    }

    public boolean isNonfoil() {
        return nonfoil;
    }

    @Override
    public String getObject() {
        return object;
    }

    public UUID getOracleId() {
        return oracleId;
    }

    @Override
    public Optional<String> getOracleText() {
        return oracleText;
    }

    public boolean isOversized() {
        return oversized;
    }

    @Override
    public Optional<String> getPower() {
        return power;
    }

    public Optional<ImmutableMap<String, String>> getPreview() {
        return preview;
    }

    public ImmutableMap<String, String> getPrices() {
        return prices;
    }

    @Override
    public Optional<String> getPrintedName() {
        return printedName;
    }

    public Optional<String> getPrintedText() {
        return printedText;
    }

    public Optional<String> getPrintedTypeLine() {
        return printedTypeLine;
    }

    public URI getPrintsSearchUri() {
        return printsSearchUri;
    }

    public ImmutableList<String> getProducedMana() {
        return producedMana;
    }

    public boolean isPromo() {
        return promo;
    }

    public ImmutableList<String> getPromoTypes() {
        return promoTypes;
    }

    public String getRarity() {
        return rarity;
    }

    public ImmutableMap<String, String> getRelatedUris() {
        return relatedUris;
    }

    public LocalDate getReleasedAt() {
        return releasedAt;
    }

    public boolean isReprint() {
        return reprint;
    }

    public boolean isReserved() {
        return reserved;
    }

    public URI getRulingsUri() {
        return rulingsUri;
    }

    public URI getScryfallSetUri() {
        return scryfallSetUri;
    }

    public URI getScryfallUri() {
        return scryfallUri;
    }

    public Optional<String> getSecurityStamp() {
        return securityStamp;
    }

    public String getSet() {
        return set;
    }

    public UUID getSetId() {
        return setId;
    }

    public String getSetName() {
        return setName;
    }

    public URI getSetSearchUri() {
        return setSearchUri;
    }

    public String getSetType() {
        return setType;
    }

    public URI getSetUri() {
        return setUri;
    }

    public boolean isStorySpotlight() {
        return storySpotlight;
    }

    public OptionalLong getTcgplayerId() {
        return tcgplayerId;
    }

    public OptionalLong getTcgplayerEtchedId() {
        return tcgplayerEtchedId;
    }

    public boolean isTextless() {
        return textless;
    }

    @Override
    public Optional<String> getToughness() {
        return toughness;
    }

    @Override
    public Optional<String> getTypeLine() {
        return Optional.of(typeLine);
    }

    public URI getUri() {
        return uri;
    }

    public boolean isVariation() {
        return variation;
    }

    public Optional<UUID> getVariationOf() {
        return variationOf;
    }

    @Override
    public Optional<String> getWatermark() {
        return watermark;
    }
}
