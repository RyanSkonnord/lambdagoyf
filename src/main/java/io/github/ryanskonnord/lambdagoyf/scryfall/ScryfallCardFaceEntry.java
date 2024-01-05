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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ScryfallCardFaceEntry implements ScryfallCardFace {

    private final Optional<String> artist;
    private final Optional<ImmutableList<String>> colorIndicator;
    private final Optional<ImmutableList<String>> colors;
    private final Optional<String> flavorText;
    private final Optional<UUID> illustrationId;
    private final Optional<ImmutableMap<String, String>> imageUris;
    private final Optional<String> loyalty;
    private final String manaCost;
    private final String name;
    private final String object;
    private final String oracleText;
    private final Optional<String> power;
    private final Optional<String> printedName;
    private final Optional<String> flavorName;
    private final Optional<String> toughness;
    private final Optional<String> typeLine;
    private final Optional<String> watermark;

    ScryfallCardFaceEntry(Map<?, ?> data) {
        artist = Optional.ofNullable((String) data.get("artist"));
        colorIndicator = Optional.ofNullable((List<?>) data.get("color_indicator")).map(ScryfallParser::parseStrings);
        colors = Optional.ofNullable((List<?>) data.get("colors")).map(ScryfallParser::parseStrings);
        flavorText = Optional.ofNullable((String) data.get("flavor_text"));
        illustrationId = Optional.ofNullable((String) data.get("illustration_id")).map(UUID::fromString);
        imageUris = Optional.ofNullable((Map<?, ?>) data.get("image_uris")).map(ScryfallParser::parseStringMap);
        loyalty = Optional.ofNullable((String) data.get("loyalty"));
        manaCost = Objects.requireNonNull((String) data.get("mana_cost"));
        name = Objects.requireNonNull((String) data.get("name"));
        object = Objects.requireNonNull((String) data.get("object"));
        oracleText = Objects.requireNonNull((String) data.get("oracle_text"));
        power = Optional.ofNullable((String) data.get("power"));
        printedName = Optional.ofNullable((String) data.get("printed_name"));
        flavorName = Optional.ofNullable((String) data.get("flavor_name"));
        toughness = Optional.ofNullable((String) data.get("toughness"));
        typeLine = Optional.ofNullable((String) data.get("type_line"));
        watermark = Optional.ofNullable((String) data.get("watermark"));
    }

    @Override
    public Optional<String> getArtist() {
        return artist;
    }

    @Override
    public Optional<ImmutableList<String>> getColorIndicator() {
        return colorIndicator;
    }

    @Override
    public Optional<ImmutableList<String>> getColors() {
        return colors;
    }

    @Override
    public Optional<String> getFlavorText() {
        return flavorText;
    }

    @Override
    public Optional<UUID> getIllustrationId() {
        return illustrationId;
    }

    @Override
    public Optional<ImmutableMap<String, String>> getImageUris() {
        return imageUris;
    }

    @Override
    public Optional<String> getLoyalty() {
        return loyalty;
    }

    @Override
    public Optional<String> getManaCost() {
        return Optional.of(manaCost);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getObject() {
        return object;
    }

    @Override
    public Optional<String> getOracleText() {
        return Optional.of(oracleText);
    }

    @Override
    public Optional<String> getPower() {
        return power;
    }

    @Override
    public Optional<String> getPrintedName() {
        return printedName;
    }

    @Override
    public Optional<String> getFlavorName() {
        return flavorName;
    }

    @Override
    public Optional<String> getToughness() {
        return toughness;
    }

    @Override
    public Optional<String> getTypeLine() {
        return typeLine;
    }

    @Override
    public Optional<String> getWatermark() {
        return watermark;
    }
}
