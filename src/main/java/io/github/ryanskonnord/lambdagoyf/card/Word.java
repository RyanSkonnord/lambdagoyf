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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import io.github.ryanskonnord.util.MapCollectors;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Stream;

public abstract class Word<E extends Enum<E> & WordType> implements Comparable<Word<E>> {

    public abstract String getKey();

    public abstract Optional<E> getEnum();

    public abstract boolean is(E value);

    public final boolean is(String key) {
        return key.equalsIgnoreCase(getKey());
    }

    public final boolean isOneOf(E... value) {
        return Arrays.stream(value).anyMatch(this::is);
    }

    public final boolean isOneOf(String... value) {
        return Arrays.stream(value).anyMatch(this::is);
    }

    @Override
    public int compareTo(Word<E> that) {
        Optional<E> thisEnum = this.getEnum();
        Optional<E> thatEnum = that.getEnum();
        if (thisEnum.isPresent() && thatEnum.isPresent()) {
            return thisEnum.get().compareTo(thatEnum.get());
        } else if (thisEnum.isPresent()) {
            return -1;
        } else if (thatEnum.isPresent()) {
            return 1;
        } else {
            return this.getKey().compareTo(that.getKey());
        }
    }

    public static final class Factory {
        private final Map<Class<? extends Enum<?>>, FactoryForType<?>> subfactories = new HashMap<>();

        private <E extends Enum<E> & WordType> FactoryForType<E> getSubfactory(Class<E> type) {
            FactoryForType<?> subfactory = subfactories.get(Objects.requireNonNull(type));
            if (subfactory == null) {
                synchronized (subfactories) {
                    subfactory = subfactories.get(type);
                    if (subfactory == null) {
                        subfactory = new FactoryForType<>(type);
                        subfactories.put(type, subfactory);
                    }
                }
            }
            return (FactoryForType<E>) subfactory;
        }

        public <E extends Enum<E> & WordType> Word<E> get(E value) {
            return getSubfactory(value.getDeclaringClass()).get(value);
        }

        public <E extends Enum<E> & WordType> Word<E> get(Class<E> type, String key) {
            return getSubfactory(type).get(key);
        }
    }

    private static final Factory GLOBAL = new Factory();

    public static <E extends Enum<E> & WordType> Word<E> of(E value) {
        return GLOBAL.get(value);
    }

    public static <E extends Enum<E> & WordType> Word<E> of(Class<E> type, String key) {
        return GLOBAL.get(type, key);
    }

    public static <E extends Enum<E> & WordType> Function<String, Word<E>> constructor(Class<E> type) {
        return (String key) -> of(type, key);
    }

    private static class FactoryForType<E extends Enum<E> & WordType> {
        private final Class<E> type;
        private final ImmutableMap<E, FromEnum<E>> enumValues;
        private final ImmutableMap<String, FromEnum<E>> enumStringValues;
        private final Map<String, FromString<E>> newStringValues = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        private FactoryForType(Class<E> type) {
            this.type = Objects.requireNonNull(type);
            this.enumValues = EnumSet.allOf(this.type).stream().collect(Maps.toImmutableEnumMap(
                    Function.<E>identity(),
                    FromEnum::new));
            this.enumStringValues = this.enumValues.values().stream()
                    .collect(MapCollectors.<FromEnum<E>>collecting()
                            .indexing((FromEnum<E> v) -> v.value.getKey())
                            .unique().toImmutableSortedMap(String.CASE_INSENSITIVE_ORDER));
        }

        private Word<E> get(E value) {
            return enumValues.get(Objects.requireNonNull(value));
        }

        private Word<E> get(String key) {
            FromEnum<E> enumStringValue = enumStringValues.get(Objects.requireNonNull(key));
            if (enumStringValue != null) return enumStringValue;

            FromString<E> newStringValue = newStringValues.get(key);
            if (newStringValue == null) {
                synchronized (newStringValues) {
                    newStringValue = newStringValues.get(key);
                    if (newStringValue == null) {
                        System.err.println(String.format("No %s value for: %s", type.getSimpleName(), key));
                        newStringValue = new FromString<>(type, key);
                        newStringValues.put(key, newStringValue);
                    }
                }
            }
            return newStringValue;
        }
    }

    private Word() {
    }

    @Override
    public String toString() {
        return getKey();
    }

    private static final class FromEnum<E extends Enum<E> & WordType> extends Word<E> {
        private final E value;

        private FromEnum(E value) {
            this.value = Objects.requireNonNull(value);
        }

        @Override
        public String getKey() {
            return value.getKey();
        }

        @Override
        public Optional<E> getEnum() {
            return Optional.of(value);
        }

        @Override
        public boolean is(E value) {
            return this.value == Objects.requireNonNull(value);
        }

        @Override
        public boolean equals(Object o) {
            return this == o || !(o == null || getClass() != o.getClass()) && value.equals(((FromEnum<?>) o).value);
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }
    }

    private static final class FromString<E extends Enum<E> & WordType> extends Word<E> {
        private final Class<E> type;
        private final String key;

        private FromString(Class<E> type, String key) {
            this.type = Objects.requireNonNull(type);
            this.key = Objects.requireNonNull(key);
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public boolean is(E value) {
            return value.getKey().equalsIgnoreCase(key);
        }

        @Override
        public Optional<E> getEnum() {
            return Optional.empty();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FromString<?> that = (FromString<?>) o;
            return type.equals(that.type) && key.equals(that.key);
        }

        @Override
        public int hashCode() {
            return 31 * type.hashCode() + key.hashCode();
        }
    }

    public static <T, W extends Word<?>> ImmutableList<W> determineOrder(
            Stream<? extends T> elements,
            Function<? super T, Stream<? extends W>> wordExtractor,
            Comparator<? super T> elementOrder) {
        SetMultimap<W, T> groups = elements
                .flatMap((T element) -> wordExtractor.apply(element)
                        .map((W word) -> Maps.immutableEntry(word, element)))
                .collect(MapCollectors.<W, T>collectingEntries().grouping().toImmutableSetMultimap());

        ImmutableMap<W, T> firstOfEach = groups.asMap().entrySet().stream()
                .collect(MapCollectors.<Map.Entry<W, Collection<T>>>collecting()
                        .withKey(Map.Entry::getKey)
                        .withValue(groupEntry -> groupEntry.getValue().stream().min(elementOrder).orElseThrow())
                        .unique().toImmutableMap());

        return firstOfEach.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getValue, elementOrder))
                .map(Map.Entry::getKey)
                .collect(ImmutableList.toImmutableList());
    }

}
