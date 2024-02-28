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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import io.github.ryanskonnord.lambdagoyf.card.field.Finish;
import io.github.ryanskonnord.lambdagoyf.scryfall.ScryfallCardEntry;
import io.github.ryanskonnord.util.MapCollectors;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public final class CardFactory {

    private final ExpansionSpoiler expansions;
    private final ImmutableMultimap<UUID, ScryfallCardEntry> entries;
    private final ArenaCard.Factory arenaFactory;

    private final CardLegality.Factory legalityFactory = new CardLegality.Factory();
    private final MtgoIdFix.Registry mtgoFixes = MtgoIdFix.loadFromResources();
    private final TypeLineCache typeLineCache = new TypeLineCache();
    private final Cache<ImmutableSet<Finish>, ImmutableSet<Finish>> finishSetCache = CacheBuilder.newBuilder()
            .maximumSize(9).build();
    private final Cache<String, Optional<String>> manaCostCache = CacheBuilder.newBuilder()
            .maximumSize(800).build();

    public CardFactory(ExpansionSpoiler expansions, Collection<ScryfallCardEntry> entries) {
        this.expansions = Objects.requireNonNull(expansions);
        this.entries = entries.stream().collect(MapCollectors.<ScryfallCardEntry>collecting()
                .indexing(ScryfallCardEntry::getOracleId)
                .grouping().toImmutableListMultimap());
        this.arenaFactory = new ArenaCard.Factory(ArenaIdFix.loadFromResources(), this.expansions);
    }

    public Spoiler createSpoiler() {
        List<Card> parsed = entries.asMap().values().parallelStream()
                .map((Collection<ScryfallCardEntry> entryGroup) -> new Card(this, entryGroup))
                .collect(Collectors.toList());
        Spoiler spoiler = new Spoiler(parsed);
        return spoiler;
    }

    public CardLegality.Factory getLegalityFactory() {
        return legalityFactory;
    }

    ExpansionSpoiler getExpansions() {
        return expansions;
    }

    MtgoIdFix.Registry getMtgoFixes() {
        return mtgoFixes;
    }

    ArenaCard.Factory getArenaFactory() {
        return arenaFactory;
    }

    TypeLine getCachedTypeLine(TypeLine value) {
        return typeLineCache.get(value);
    }

    ImmutableSet<Finish> cacheFinishSet(ImmutableSet<Finish> finishSet) {
        try {
            return finishSetCache.get(finishSet, () -> finishSet);
        } catch (ExecutionException e) {
            return finishSet;
        }
    }

    Optional<String> cacheManaCost(Optional<String> manaCost) {
        if (manaCost.isPresent()) {
            try {
                return manaCostCache.get(manaCost.get(), () -> manaCost);
            } catch (ExecutionException e) {
                return manaCost;
            }
        } else {
            return Optional.empty();
        }
    }
}
