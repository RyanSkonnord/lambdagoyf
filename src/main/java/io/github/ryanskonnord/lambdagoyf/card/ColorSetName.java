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

import com.google.common.collect.Comparators;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public enum ColorSetName {

    AZORIUS(Color.WHITE, Color.BLUE),
    ORZHOV(Color.WHITE, Color.BLACK),
    DIMIR(Color.BLUE, Color.BLACK),
    IZZET(Color.BLUE, Color.RED),
    RAKDOS(Color.BLACK, Color.RED),
    GOLGARI(Color.BLACK, Color.GREEN),
    GRUUL(Color.RED, Color.GREEN),
    BOROS(Color.RED, Color.WHITE),
    SELESNYA(Color.GREEN, Color.WHITE),
    SIMIC(Color.GREEN, Color.BLUE),

    ESPER(Color.WHITE, Color.BLUE, Color.BLACK),
    GRIXIS(Color.BLUE, Color.BLACK, Color.RED),
    JUND(Color.BLACK, Color.RED, Color.GREEN),
    NAYA(Color.RED, Color.GREEN, Color.WHITE),
    BANT(Color.GREEN, Color.WHITE, Color.BLUE),

    ABZAN(Color.WHITE, Color.BLACK, Color.GREEN),
    JESKAI(Color.BLUE, Color.RED, Color.WHITE),
    MARDU(Color.BLACK, Color.WHITE, Color.RED),
    TEMUR(Color.RED, Color.BLUE, Color.GREEN),
    SULTAI(Color.GREEN, Color.BLACK, Color.BLUE);

    private final String displayName;
    private final ColorSet colors;

    private ColorSetName(Color... colors) {
        this.displayName = name().charAt(0) + name().substring(1).toLowerCase();
        this.colors = ColorSet.copyOf(colors);
    }

    public String getDisplayName() {
        return displayName;
    }

    public ColorSet getColors() {
        return colors;
    }

    private static final ImmutableMap<ColorSet, ColorSetName> BY_SET = Maps.uniqueIndex(
            EnumSet.allOf(ColorSetName.class), ColorSetName::getColors);

    public static Optional<ColorSetName> of(ColorSet colors) {
        return Optional.ofNullable(BY_SET.get(colors));
    }

    static {
        assert Objects.equals(
                EnumSet.allOf(ColorSetName.class).stream().map(ColorSetName::getColors).collect(Collectors.toSet()),
                ColorSet.getPowerSet().stream().filter(s -> switch (s.size()) {
                    case 2, 3 -> true;
                    default -> false;
                }).collect(Collectors.toSet()));
        assert Comparators.isInOrder(Arrays.asList(values()), Comparator.comparing(ColorSetName::getColors));
    }

}
