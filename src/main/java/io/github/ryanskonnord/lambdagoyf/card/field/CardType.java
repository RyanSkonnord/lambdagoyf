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

import io.github.ryanskonnord.lambdagoyf.card.WordType;

public enum CardType implements WordType {
    TRIBAL, ENCHANTMENT, ARTIFACT, LAND, CREATURE, PLANESWALKER, BATTLE, SORCERY, INSTANT,
    EMBLEM, CONSPIRACY, DUNGEON,
    VANGUARD, PLANE, PHENOMENON, SCHEME, CARD, STICKERS;

    private final String key = name().charAt(0) + name().substring(1).toLowerCase();

    @Override
    public String getKey() {
        return key;
    }

    public boolean isConventionalType() {
        return switch (this) {
            case TRIBAL, ENCHANTMENT, ARTIFACT, LAND, CREATURE, PLANESWALKER, BATTLE, SORCERY, INSTANT -> true;
            default -> false;
        };
    }

    public boolean isPermanentType() {
        return switch (this) {
            case ENCHANTMENT, ARTIFACT, LAND, CREATURE, PLANESWALKER, BATTLE -> true;
            default -> false;
        };
    }
}
