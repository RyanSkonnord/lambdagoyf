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
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Ordering;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import io.github.ryanskonnord.lambdagoyf.card.field.Format;
import io.github.ryanskonnord.lambdagoyf.card.field.Legality;
import io.github.ryanskonnord.util.MapCollectors;

import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class CardLegality {

    private CardLegality() {
    }

    public abstract Optional<Legality> get(Word<Format> format);

    public abstract Map<Word<Format>, Legality> asMap();

    public final boolean isIn(Legality legality, Word<Format> format) {
        return get(format).filter(l -> l == legality).isPresent();
    }

    public final boolean isPermittedIn(Word<Format> format) {
        return get(format).filter(Legality::isPermitted).isPresent();
    }

    public Stream<Word<Format>> getPermittedFormats() {
        return asMap().entrySet().stream()
                .filter(e -> e.getValue().isPermitted())
                .map(Map.Entry::getKey);
    }


    public static final class Factory {
        private final Cache<Map<Word<Format>, Legality>, CardLegality> cache = CacheBuilder.newBuilder()
                .initialCapacity(256)
                .build();

        public CardLegality create(Map<Word<Format>, Legality> map) {
            try {
                return cache.get(map, () -> construct(map));
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        private static final Comparator<Legality> MERGING_PRIORITY = Ordering.explicit(Legality.LEGAL, Legality.RESTRICTED, Legality.BANNED, Legality.NOT_LEGAL);

        public CardLegality merge(Stream<CardLegality> cardLegalityStream) {
            Collection<CardLegality> distinctMaps = cardLegalityStream.distinct().collect(Collectors.toList());
            Preconditions.checkArgument(!distinctMaps.isEmpty());
            if (distinctMaps.size() == 1) return distinctMaps.iterator().next();

            SetMultimap<Word<Format>, Legality> mergedMap = distinctMaps.stream()
                    .flatMap(cl -> cl.asMap().entrySet().stream())
                    .collect(MapCollectors.<Word<Format>, Legality>collectingEntries()
                            .grouping().toMultimap(Factory::createEmptyMergeMap));
            Map<Word<Format>, Legality> prioritizedMap = Maps.transformValues(mergedMap.asMap(),
                    legalitySet -> legalitySet.stream().min(MERGING_PRIORITY).orElseThrow());
            return create(prioritizedMap);
        }

        private static SetMultimap<Word<Format>, Legality> createEmptyMergeMap() {
            return MultimapBuilder.hashKeys(ALL_ENUMERATED_FORMATS.size()).enumSetValues(Legality.class).build();
        }
    }

    private static CardLegality construct(Map<Word<Format>, Legality> map) {
        Map<Format, Legality> legalities = new EnumMap<>(Format.class);
        boolean isComplex = false;
        for (Map.Entry<Word<Format>, Legality> entry : map.entrySet()) {
            Optional<Format> formatWord = entry.getKey().getEnum();
            if (formatWord.isEmpty()) {
                return new ExtensibleCardLegality(map);
            }
            Format format = formatWord.get();
            Legality legality = entry.getValue();
            if (legality != Legality.NOT_LEGAL) {
                if (legality != Legality.LEGAL) {
                    isComplex = true;
                }
                legalities.put(format, legality);
            }
        }

        if (isComplex) {
            return new ComplexCardLegality(legalities);
        } else {
            return new SimpleCardLegality(legalities.keySet());
        }
    }


    private static final ImmutableSet<Word<Format>> ALL_ENUMERATED_FORMATS = EnumSet.allOf(Format.class).stream()
            .map(Word::of).collect(ImmutableSet.toImmutableSet());

    /**
     * An optimized class containing keys for all {@code Word&lt;Format&gt;} values, and only those values, that
     * correspond to a {@link Format} enum.
     */
    private static abstract class EnumeratedCardLegality extends CardLegality {
        abstract Legality getForEnum(Format format);

        @Override
        public final Optional<Legality> get(Word<Format> format) {
            return format.getEnum().map(this::getForEnum);
        }

        @Override
        public final Map<Word<Format>, Legality> asMap() {
            return Maps.asMap(ALL_ENUMERATED_FORMATS,
                    (Word<Format> format) -> getForEnum(format.getEnum().orElseThrow()));
        }
    }

    private static final class SimpleCardLegality extends EnumeratedCardLegality {
        private final ImmutableSet<Format> legal;

        private SimpleCardLegality(Set<Format> legal) {
            this.legal = Sets.immutableEnumSet(legal);
        }

        protected Legality getForEnum(Format format) {
            return legal.contains(format) ? Legality.LEGAL : Legality.NOT_LEGAL;
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj ||
                    (obj instanceof SimpleCardLegality
                            ? legal.equals(((SimpleCardLegality) obj).legal)
                            : super.equals(obj));
        }
    }

    private static final class ComplexCardLegality extends EnumeratedCardLegality {
        private final ImmutableMap<Format, Legality> legalities;

        private ComplexCardLegality(Map<Format, Legality> legalities) {
            this.legalities = Maps.immutableEnumMap(legalities);
        }

        @Override
        Legality getForEnum(Format format) {
            return legalities.getOrDefault(format, Legality.NOT_LEGAL);
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj ||
                    (obj instanceof ComplexCardLegality
                            ? legalities.equals(((ComplexCardLegality) obj).legalities)
                            : super.equals(obj));
        }
    }

    /**
     * Tolerates {@code Word&lt;Format&gt;} values that don't correspond to a {@link Format} enum.
     */
    private static final class ExtensibleCardLegality extends CardLegality {
        private final ImmutableMap<Word<Format>, Legality> map;

        private ExtensibleCardLegality(Map<Word<Format>, Legality> map) {
            this.map = ImmutableMap.copyOf(map);
        }

        @Override
        public Optional<Legality> get(Word<Format> format) {
            return Optional.ofNullable(map.get(format));
        }

        @Override
        public Map<Word<Format>, Legality> asMap() {
            return map;
        }
    }


    @Override
    public String toString() {
        return asMap().toString();
    }

    private transient int hash;

    @Override
    public int hashCode() {
        return hash != 0 ? hash : (hash = asMap().hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || obj instanceof CardLegality && asMap().equals(((CardLegality) obj).asMap());
    }
}
