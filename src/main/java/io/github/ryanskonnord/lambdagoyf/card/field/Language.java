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

public enum Language implements WordType {
    ENGLISH("en"),
    ITALIAN("it"),
    FRENCH("fr"),
    SPANISH("es"),
    GERMAN("de"),
    PORTUGUESE("pt"),
    JAPANESE("ja"),
    SIMPLIFIED_CHINESE("zhs"),
    TRADITIONAL_CHINESE("zht"),
    RUSSIAN("ru"),
    KOREAN("ko"),
    LATIN("la"),
    ANCIENT_GREEK("grc"),
    SANSKRIT("sa"),
    ARABIC("ar"),
    HEBREW("he"),
    PHYREXIAN("ph");

    private final String key;

    Language(String key) {
        this.key = key;
    }

    @Override
    public String getKey() {
        return key;
    }
}
