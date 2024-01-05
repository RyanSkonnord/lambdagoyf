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

package io.github.ryanskonnord.lambdagoyf.card.field;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import io.github.ryanskonnord.lambdagoyf.card.WordType;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;

public enum Watermark implements WordType {
    JUNIOR, SET, ARENA, FNM, JAPAN_JUNIOR, MTG10, DCI, FLAVOR, JUNIOR_EUROPE,

    AZORIUS, ORZHOV, DIMIR, IZZET, RAKDOS, GOLGARI, GRUUL, BOROS, SELESNYA, SIMIC,

    MTG, COLOR_PIE, JUNIOR_APAC, MPS, PRO_TOUR, WOTC, GRAND_PRIX, SCHOLARSHIP, MTG15, MIRRAN, PHYREXIAN, WPN,
    DENGEKI_MAOH, HEROS_PATH, CONSPIRACY,

    ABZAN, JESKAI, MARDU, SULTAI, TEMUR,
    OJUTAI, SILUMGAR, KOLAGHAN, ATARKA, DROMOKA,

    PLANESWALKER,

    ORDER_OF_THE_WIDGET, AGENTS_OF_SNEAK, LEAGUE_OF_DASTARDLY_DOOM, GOBLIN_EXPLOSIONEERS, CROSSBREED_LABS,

    DND("d&d"), NERF, TRANSFORMERS, MLP_GEMS, MLP_SPARKLE, MLP_WANING_MOON, MLP_WAXING_MOON,

    JUDGE_ACADEMY, TRUMP_KATSUMAI, COROCORO,

    FORETELL,

    LOREHOLD, PRISMARI, QUANDRIX, SILVERQUILL, WITHERBLOOM;

    private final String key;

    Watermark() {
        key = name().toLowerCase().replace("_", "");
    }

    Watermark(String key) {
        this.key = Objects.requireNonNull(key);
    }

    @Override
    public String getKey() {
        return key;
    }

    public static final ImmutableSet<Watermark> RAVNICA_GUILDS = Sets.immutableEnumSet(
            AZORIUS, ORZHOV, DIMIR, IZZET, RAKDOS, GOLGARI, GRUUL, BOROS, SELESNYA, SIMIC);
    public static final ImmutableSet<Watermark> TARKIR_KHAN_CLANS = Sets.immutableEnumSet(
            ABZAN, JESKAI, MARDU, SULTAI, TEMUR);
    public static final ImmutableSet<Watermark> TARKIR_DRAGON_CLANS = Sets.immutableEnumSet(
            OJUTAI, SILUMGAR, KOLAGHAN, ATARKA, DROMOKA);
    public static final ImmutableSet<Watermark> TARKIR_CLANS = Stream.of(TARKIR_KHAN_CLANS, TARKIR_DRAGON_CLANS)
            .flatMap(Collection::stream).collect(Sets.toImmutableEnumSet());
    public static final ImmutableSet<Watermark> UNSTABLE_FACTIONS = Sets.immutableEnumSet(
            ORDER_OF_THE_WIDGET, AGENTS_OF_SNEAK, LEAGUE_OF_DASTARDLY_DOOM, GOBLIN_EXPLOSIONEERS, CROSSBREED_LABS);
    public static final ImmutableSet<Watermark> HASBRO_WATERMARKS = Sets.immutableEnumSet(
            DND, NERF, TRANSFORMERS, MLP_GEMS, MLP_SPARKLE, MLP_WANING_MOON, MLP_WAXING_MOON);
    public static final ImmutableSet<Watermark> STRIXHAVEN_COLLEGES = Sets.immutableEnumSet(
            LOREHOLD, PRISMARI, QUANDRIX, SILVERQUILL, WITHERBLOOM);

}
