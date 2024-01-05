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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import io.github.ryanskonnord.util.MapCollectors;

import java.util.EnumSet;
import java.util.Optional;
import java.util.stream.Stream;

public enum Color {
    WHITE('W', "Plains"), BLUE('U', "Island"), BLACK('B', "Swamp"), RED('R', "Mountain"), GREEN('G', "Forest");

    private final char symbol;
    private final String basicLandType;

    Color(char symbol, String basicLandType) {
        this.symbol = symbol;
        this.basicLandType = basicLandType;
    }

    public char getSymbol() {
        return symbol;
    }

    private static final ImmutableMap<Character, Color> BY_SYMBOL = Maps.uniqueIndex(
            EnumSet.allOf(Color.class), Color::getSymbol);

    public static Color fromSymbol(char symbol) {
        Color color = BY_SYMBOL.get(Character.toUpperCase(symbol));
        Preconditions.checkArgument(color != null, "Not a color symbol: " + symbol);
        return color;
    }

    private static final ImmutableMap<String, Color> BY_STRING = EnumSet.allOf(Color.class).stream()
            .flatMap((Color color) -> Stream.of(color.name(), Character.toString(color.getSymbol()))
                    .map(key -> Maps.immutableEntry(key, color)))
            .collect(MapCollectors.<String, Color>collectingEntries().unique().toImmutableMap());

    public static Color fromString(String s) {
        Color color = BY_STRING.get(s.toUpperCase());
        Preconditions.checkArgument(color != null, "Not a color string: " + s);
        return color;
    }

    public String getBasicLandType() {
        return basicLandType;
    }

    private static final ImmutableMap<String, Color> BASIC_LAND_TYPES = Maps.uniqueIndex(
            EnumSet.allOf(Color.class), Color::getBasicLandType);

    public static ImmutableSet<String> getBasicLandTypes() {
        return BASIC_LAND_TYPES.keySet();
    }

    public static Optional<Color> fromBasicLandType(String basicLandType) {
        return Optional.ofNullable(BASIC_LAND_TYPES.get(basicLandType));
    }

}
