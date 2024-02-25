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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSetMultimap;
import io.github.ryanskonnord.util.MapCollectors;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CardNames {
    private static final ImmutableMap<Character, String> LATIN_CHARACTERS = ImmutableSetMultimap
            .<String, Character>builder()
            .putAll("a", '\u00e0', '\u00e1', '\u00e2', '\u00e3', '\u00e4', '\u00e5')
            .putAll("ae", '\u00e6')
            .putAll("c", '\u00e7')
            .putAll("e", '\u00e8', '\u00e9', '\u00ea', '\u00eb')
            .putAll("i", '\u00ec', '\u00ed', '\u00ee', '\u00ef')
            .putAll("n", '\u00f1')
            .putAll("o", '\u00f2', '\u00f3', '\u00f4', '\u00f5', '\u00f6')
            .putAll("u", '\u00f9', '\u00fa', '\u00fb', '\u00fc')
            .build().entries().stream()
            .collect(MapCollectors.<Map.Entry<String, Character>>collecting()
                    .withKey(Map.Entry::getValue).withValue(Map.Entry::getKey)
                    .unique().toImmutableMap());
    private static final Pattern COMPOUND_NAME_PATTERN = Pattern.compile("(?<first>.*?)\\s*/+\\s*(?<second>.*?)");

    public static String normalize(String name) {
        Matcher matcher = COMPOUND_NAME_PATTERN.matcher(name);
        if (matcher.matches()) {
            name = matcher.group("first") + " // " + matcher.group("second");
        }
        return name.toLowerCase().chars().mapToObj(i -> {
            char c = (char) i;
            String s = LATIN_CHARACTERS.get(c);
            return s != null ? s : Character.toString(c);
        }).collect(Collectors.joining());
    }
}
