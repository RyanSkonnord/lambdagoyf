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

package io.github.ryanskonnord.lambdagoyf.deck;

import com.google.common.collect.ImmutableMultimap;
import io.github.ryanskonnord.lambdagoyf.card.CardNames;
import io.github.ryanskonnord.lambdagoyf.card.Spoiler;
import io.github.ryanskonnord.util.MapCollectors;

import java.util.Map;
import java.util.stream.Stream;

/**
 * A generalized means of resolving card names to card versions.
 * <p>
 * This class can be thought of as an extended spoiler. A Spoiler object provides base data for most card versions, and
 * it can be extended by additional entries. The main use case is that a parsed deck file may contain references to
 * digital versions of cards that are missing from the base Spoiler.
 */
public class CardDictionary<C> {

    private final Spoiler spoiler;
    private final ImmutableMultimap<String, C> extensions;

    public CardDictionary(Spoiler spoiler, Stream<Map.Entry<String, C>> extensions) {
        this.spoiler = spoiler;
        this.extensions = extensions.map(CardNames::normalizeMapEntry)
                .collect(MapCollectors.<String, C>collectingEntries().grouping().toImmutableSetMultimap());
    }
}
