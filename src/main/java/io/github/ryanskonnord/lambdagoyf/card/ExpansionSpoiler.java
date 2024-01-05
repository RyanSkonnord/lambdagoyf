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
import com.google.common.collect.ImmutableSet;
import io.github.ryanskonnord.lambdagoyf.scryfall.ScryfallSet;
import io.github.ryanskonnord.util.MapCollectors;

import java.util.Collection;
import java.util.Optional;

public final class ExpansionSpoiler {

    private final ImmutableSet<Expansion> expansions;
    private final ImmutableMap<String, Expansion> byCode;

    public ExpansionSpoiler(Collection<ScryfallSet> setData) {
        expansions = setData.stream().map(Expansion::new).sorted().collect(ImmutableSet.toImmutableSet());
        byCode = expansions.stream().collect(MapCollectors.<Expansion>collecting()
                .indexing(Expansion::getProductCode)
                .unique().toImmutableSortedMap(String.CASE_INSENSITIVE_ORDER));
    }

    public ImmutableSet<Expansion> getAll() {
        return expansions;
    }

    public Optional<Expansion> lookUpByProductCode(String productCode) {
        return Optional.ofNullable(byCode.get(productCode));
    }

    @Override
    public boolean equals(Object o) {
        return this == o || !(o == null || getClass() != o.getClass())
                && expansions.equals(((ExpansionSpoiler) o).expansions);
    }

    @Override
    public int hashCode() {
        return expansions.hashCode();
    }
}
