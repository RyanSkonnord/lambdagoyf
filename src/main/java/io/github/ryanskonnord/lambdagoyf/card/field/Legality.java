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

import com.google.common.collect.ImmutableSortedMap;
import io.github.ryanskonnord.util.MapCollectors;

import java.util.EnumSet;
import java.util.Optional;

public enum Legality {
    NOT_LEGAL, LEGAL, BANNED, RESTRICTED;

    private static final ImmutableSortedMap<String, Legality> BY_NAME = EnumSet.allOf(Legality.class).stream()
            .collect(MapCollectors.<Legality>collecting()
                    .indexing(Legality::name)
                    .unique().toImmutableSortedMap(String.CASE_INSENSITIVE_ORDER));

    public static Optional<Legality> fromName(String name) {
        return Optional.ofNullable(BY_NAME.get(name));
    }

    public boolean isPermitted() {
        return switch (this) {
            case LEGAL, RESTRICTED -> true;
            case NOT_LEGAL, BANNED -> false;
        };
    }
}
