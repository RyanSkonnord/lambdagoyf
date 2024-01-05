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

package io.github.ryanskonnord.util;


import com.google.common.annotations.Beta;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedMultiset;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.common.collect.TreeMultiset;

import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.Collector;
import java.util.stream.Collector.Characteristics;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class MapCollectors {
    private MapCollectors() {
        throw new AssertionError();
    }

    /**
     * Begin building a map collector.
     *
     * @param <T> the stream's element type
     */
    public static <T> KeyStep<T> collecting() {
        return new KeyStep<>();
    }

    /**
     * Begin mapping each element in a stream to itself.
     * <p>
     * This is useful for building a set of flyweight objects. It also is useful for normalizing
     * values into the form they appeared in the original stream, in cases where values with
     * distinct states are treated as equal, either by their class's {@code hashCode} and {@code
     * equals} methods, or by the {@link Comparator} used to build a {@link java.util.SortedMap}.
     *
     * @param <T> the stream's element type
     */
    public static <T> MergeStep<T, T, T> collectingIdentities() {
        return MapCollectors.<T>collecting().identityKey().identityValue();
    }

    /**
     * Begin building a map whose keys and values are defined by existing {@link Map.Entry}
     * objects.
     * <p>
     * This is useful when a sophisticated filtering or sorting step needs to precede the
     * collection step in a stream.  In such a case, there may be a {@link Stream#map} or {@link
     * Stream#flatMap(Function)} step with returned values constructed by {@link
     * com.google.common.collect.Maps#immutableEntry(Object, Object)}.
     *
     * @param <K> the map's key type
     * @param <U> the map's value type
     */
    public static <K, U> MergeStep<Map.Entry<K, U>, K, U> collectingEntries() {
        return MapCollectors.<Map.Entry<K, U>>collecting()
                .withKey(Map.Entry::getKey)
                .withValue(Map.Entry::getValue);
    }

    /**
     * Begin building a multiset whose elements and counts are defined by existing {@link
     * Multiset.Entry} objects.
     * <p>
     * This is useful when a sophisticated filtering or sorting step needs to precede the
     * collection step in a stream.  In such a case, there may be a {@link Stream#map} or {@link
     * Stream#flatMap(Function)} step with returned values constructed by {@link
     * Multisets#immutableEntry(Object, int)}.
     *
     * @param <T> the stream's element type
     */
    public static <T> MultisetStep<Multiset.Entry<T>, T> collectingMultisetEntries() {
        return MapCollectors.<Multiset.Entry<T>>collecting()
                .withKey(Multiset.Entry::getElement)
                .countingBy(Multiset.Entry::getCount);
    }


    /**
     * Choose the map's keys.
     *
     * @param <T> the stream's element type
     */
    public static final class KeyStep<T> {
        private KeyStep() {
        }

        /**
         * Apply a function to transform the stream's elements into the map's keys.
         *
         * @param <K> the map's key type
         */
        public <K> ValueStep<T, K> withKey(Function<? super T, ? extends K> keyMapper) {
            return new ValueStep<>(Objects.requireNonNull(keyMapper));
        }

        /**
         * Use the stream's elements as the map's keys.
         */
        public ValueStep<T, T> identityKey() {
            return withKey(Function.identity());
        }

        /**
         * Use the stream's elements as the map's values and extract the keys with a function.
         *
         * @param <K> the map's key type
         */
        public <K> MergeStep<T, K, T> indexing(Function<? super T, ? extends K> keyMapper) {
            return this.<K>withKey(keyMapper).identityValue();
        }

        /**
         * Use the stream's elements as the map's keys, applying a function to transform them into
         * the map's values.
         *
         * @param <U> the map's value type
         */
        public <U> MergeStep<T, T, U> memoizing(Function<? super T, ? extends U> valueMapper) {
            return identityKey().withValue(valueMapper);
        }

        /**
         * Use the stream's elements as the map's keys, applying a function to transform them each
         * into multiple values belonging to the same key.
         *
         * @param <U> the map's value type
         */
        public <U> MultimapStep<T, T, U> flattening(Function<? super T, Stream<? extends U>> valueMapper) {
            return identityKey().withFlatValues(valueMapper);
        }
    }

    /**
     * Choose the map's values.
     *
     * @param <T> the stream's element type
     * @param <K> the map's key type
     */
    public static final class ValueStep<T, K> {
        private final Function<? super T, ? extends K> keyMapper;

        private ValueStep(Function<? super T, ? extends K> keyMapper) {
            this.keyMapper = keyMapper;
        }

        /**
         * Apply a function to transform the stream's elements into the map's values.
         *
         * @param <U> the map's value type
         */
        public <U> MergeStep<T, K, U> withValue(Function<? super T, ? extends U> valueMapper) {
            return new MergeStep<>(keyMapper, Objects.requireNonNull(valueMapper));
        }

        /**
         * Use the stream's elements as the map's values.
         */
        public MergeStep<T, K, T> identityValue() {
            return withValue(Function.identity());
        }

        /**
         * Apply a function to transform each stream element into multiple values belonging to the
         * same key.
         *
         * @param <U> the map's value type
         */
        public <U> MultimapStep<T, K, U> withFlatValues(Function<? super T, Stream<? extends U>> valueMapper) {
            return new MultimapStep<>(
                    keyMapper,
                    new MultimapStreamValueMapper<>(Objects.requireNonNull(valueMapper)));
        }

        /**
         * Build a map that counts the number of times each key appears.
         */
        public MultisetStep<T, K> counting() {
            return countingBy(value -> 1);
        }

        /**
         * Build a map that counts a number for each key, applying a function to extract the
         * corresponding count from each stream element.
         */
        public MultisetStep<T, K> countingBy(ToIntFunction<? super T> countMapper) {
            return new MultisetStep<>(keyMapper, Objects.requireNonNull(countMapper));
        }
    }

    /**
     * Choose how to handle more than one stream element having the same key.
     *
     * @param <T> the stream's element type
     * @param <K> the map's key type
     * @param <U> the map's value type
     */
    public static final class MergeStep<T, K, U> {
        private final Function<? super T, ? extends K> keyMapper;
        private final Function<? super T, ? extends U> valueMapper;

        private MergeStep(Function<? super T, ? extends K> keyMapper,
                          Function<? super T, ? extends U> valueMapper) {
            this.keyMapper = keyMapper;
            this.valueMapper = valueMapper;
        }

        /**
         * Apply an operation to merge a pair of values with the same key.
         */
        public MapStep<T, K, U> mergingBy(BinaryOperator<U> mergeFunction) {
            return new MapStep<>(new MapStepWithMergeFunction<>(
                    keyMapper, valueMapper, Objects.requireNonNull(mergeFunction)));
        }

        /**
         * Assert that each key is unique. The collection operation will throw a {@link
         * RuntimeException} if not.
         */
        public MapStep<T, K, U> unique() {
            return mergingBy(AssertingUniqueKeys.instance());
        }

        /**
         * Assert that each key is unique, constructing a custom exception if not.
         */
        public MapStep<T, K, U> uniqueOrElseThrow(BiFunction<U, U, ? extends RuntimeException> exceptionFactory) {
            Objects.requireNonNull(exceptionFactory);
            return mergingBy((U u1, U u2) -> {
                throw exceptionFactory.apply(u1, u2);
            });
        }

        /**
         * Assert that each key-value entry is distinct. Duplicate entries will be discarded. The
         * collection operation will throw a {@link RuntimeException} if two unequal values share a
         * key.
         */
        public MapStep<T, K, U> distinctEntries() {
            return mergingBy((u1, u2) -> {
                if (Objects.equals(u1, u2)) {
                    return u1;
                } else {
                    throw new IllegalStateException();
                }
            });
        }

        /**
         * Group values with a common key into compound values. The compound values are described
         * in the next step.
         */
        public MultimapStep<T, K, U> grouping() {
            return new MultimapStep<>(keyMapper, new MultimapSingleValueMapper<>(valueMapper));
        }

        /**
         * Build an {@link ImmutableBiMap}, which asserts that each key and value is unique. The
         * collection operation will throw an {@link IllegalStateException} if not.
         */
        public Collector<T, ?, ImmutableBiMap<K, U>> toImmutableBiMap() {
            return ImmutableBiMap.toImmutableBiMap(keyMapper, valueMapper);
        }
    }

    /**
     * Choose how to build a {@link Map} object.
     *
     * @param <T> the stream's element type
     * @param <K> the map's key type
     * @param <U> the map's value type
     */
    public static final class MapStep<T, K, U> {
        private final MapStepImpl<T, K, U> impl;

        private MapStep(MapStepImpl<T, K, U> impl) {
            this.impl = impl;
        }

        /**
         * Build a {@link Map} of the type supplied from a factory. The factory should supply a
         * new, empty, mutable map.
         *
         * @param <M> the type of map to build
         */
        public <M extends Map<K, U>> Collector<T, ?, M> toMap(Supplier<M> mapFactory) {
            return impl.toMap(mapFactory);
        }

        /**
         * Build a {@link HashMap}.
         */
        public Collector<T, ?, HashMap<K, U>> toHashMap() {
            return toMap(HashMap::new);
        }

        /**
         * Build a {@link LinkedHashMap}.
         */
        public Collector<T, ?, LinkedHashMap<K, U>> toLinkedHashMap() {
            return toMap(LinkedHashMap::new);
        }

        /**
         * Build a {@link TreeMap}.
         */
        public Collector<T, ?, TreeMap<K, U>> toTreeMap(Comparator<? super K> comparator) {
            return toMap(() -> new TreeMap<>(comparator));
        }

        /**
         * Build an {@link ImmutableMap}.
         */
        public Collector<T, ?, ImmutableMap<K, U>> toImmutableMap() {
            return impl.toImmutableMap();
        }

        /**
         * Build an {@link ImmutableSortedMap}.
         */
        public Collector<T, ?, ImmutableSortedMap<K, U>> toImmutableSortedMap(Comparator<? super K> comparator) {
            return impl.toImmutableSortedMap(comparator);
        }

        /**
         * Build an {@link EnumMap} using a provided enum type token. The type token must match the
         * key type (<code>K</code>), which cannot be checked at compile time.
         *
         * @param <E> an enum type that is equal to <code>K</code>
         */
        @Beta
        public <E extends Enum<E>> Collector<T, ?, EnumMap<E, U>> toEnumMap(Class<E> enumKeyType) {
            Objects.requireNonNull(enumKeyType);
            return ((MapStepImpl<T, E, U>) impl).toMap(() -> new EnumMap<>(enumKeyType));
        }

        /**
         * Build an {@link ImmutableMap} that wraps an efficient {@link EnumMap} using a provided
         * enum type token. The type token must match the key type (<code>K</code>), which cannot
         * be checked at compile time.
         *
         * @param <E> an enum type that is equal to <code>K</code>
         */
        @Beta
        public <E extends Enum<E>> Collector<T, ?, ImmutableMap<E, U>> toImmutableEnumMap(Class<E> enumKeyType) {
            return Collectors.collectingAndThen(toEnumMap(enumKeyType), Maps::immutableEnumMap);
        }
    }

    /**
     * Choose how to build a map where each key may be associated with more than one value.
     *
     * @param <T> the stream's element type
     * @param <K> the map's key type
     * @param <U> the map's value type
     */
    public static final class MultimapStep<T, K, U> {
        private final Function<? super T, ? extends K> keyMapper;
        private final MultimapValueMapper<T, U> valueMapper;

        private MultimapStep(Function<? super T, ? extends K> keyMapper,
                             MultimapValueMapper<T, U> valueMapper) {
            this.keyMapper = keyMapper;
            this.valueMapper = valueMapper;
        }

        /**
         * Build a map where each group of values belonging to the same key is passed to a {@link
         * Collector}. Each object built by the collector becomes a map value.
         *
         * @param <A> the collector's accumulation type
         * @param <D> the compound type to become the map's value, replacing {@code U}
         */
        public <A, D> MapStep<T, K, D> into(Collector<? super U, A, D> downstream) {
            return new MapStep<>(new MapStepWithDownstreamGrouping<>(
                    keyMapper, valueMapper, Objects.requireNonNull(downstream)));
        }

        /**
         * Build a map where each group of values belonging to the same key is assembled into a
         * {@link Collection} object. The factory must return an empty, mutable collection.
         *
         * @param <C> the collection type to become the map's value, replacing {@code U}
         */
        public <C extends Collection<U>> MapStep<T, K, C> intoCollections(Supplier<C> collectionFactory) {
            return into(Collectors.toCollection(Objects.requireNonNull(collectionFactory)));
        }

        /**
         * Build a {@link Multimap} of the type supplied from a factory. The factory should supply
         * a new, empty, mutable multimap.
         *
         * @param <M> the type of multimap to build
         */
        public <M extends Multimap<K, U>> Collector<T, ?, M> toMultimap(Supplier<M> multimapFactory) {
            return valueMapper.toMultimap(keyMapper, Objects.requireNonNull(multimapFactory));
        }

        /**
         * Build a {@link ImmutableListMultimap}.
         */
        public Collector<T, ?, ImmutableListMultimap<K, U>> toImmutableListMultimap() {
            return valueMapper.toImmutableListMultimap(keyMapper);
        }

        /**
         * Build a {@link ImmutableSetMultimap}.
         */
        public Collector<T, ?, ImmutableSetMultimap<K, U>> toImmutableSetMultimap() {
            return valueMapper.toImmutableSetMultimap(keyMapper);
        }
    }

    /**
     * Choose how to build a {@link Multiset} or map where each key is associated with a count or
     * other integer.
     *
     * @param <T> the stream's element type
     * @param <E> the multiset's or map's element type
     */
    public static final class MultisetStep<T, E> {
        private final Function<? super T, ? extends E> elementMapper;
        private final ToIntFunction<? super T> countMapper;

        private MultisetStep(Function<? super T, ? extends E> elementMapper, ToIntFunction<? super T> countMapper) {
            this.elementMapper = elementMapper;
            this.countMapper = countMapper;
        }

        /**
         * Build a {@link Map} object with {@link Integer}s as keys.
         */
        public MapStep<T, E, Integer> toCountMap() {
            return new MapStep<>(new MapStepWithMergeFunction<>(
                    elementMapper, countMapper::applyAsInt, Integer::sum));
        }

        /**
         * Build a {@link Multiset} of the type supplied from a factory. The factory should supply
         * a new, empty, mutable multiset.
         *
         * @param <M> the type of multiset to build
         */
        public <M extends Multiset<E>> Collector<T, ?, M> toMultiset(Supplier<M> multisetFactory) {
            return Multisets.toMultiset(this.elementMapper::apply, countMapper,
                    Objects.requireNonNull(multisetFactory));
        }

        /**
         * Build a {@link HashMultiset}.
         */
        public Collector<T, ?, HashMultiset<E>> toHashMultiset() {
            return toMultiset(HashMultiset::create);
        }

        /**
         * Build a {@link LinkedHashMultiset}.
         */
        public Collector<T, ?, LinkedHashMultiset<E>> toLinkedHashMultiset() {
            return toMultiset(LinkedHashMultiset::create);
        }

        /**
         * Build a {@link TreeMultiset}.
         */
        public Collector<T, ?, TreeMultiset<E>> toTreeMultiset(Comparator<? super E> comparator) {
            return toMultiset(() -> TreeMultiset.create(comparator));
        }

        /**
         * Build an {@link ImmutableMultiset}.
         */
        public Collector<T, ?, ImmutableMultiset<E>> toImmutableMultiset() {
            return ImmutableMultiset.toImmutableMultiset(elementMapper, countMapper);
        }

        /**
         * Build an {@link ImmutableSortedMultiset}.
         */
        public Collector<T, ?, ImmutableSortedMultiset<E>> toImmutableSortedMultiset(Comparator<? super E> comparator) {
            return ImmutableSortedMultiset.toImmutableSortedMultiset(
                    comparator, elementMapper, countMapper);
        }
    }


    /*
     * Define a type for this rather than using a lambda, so that
     * MapStepWithMergeFunction.toImmutableMap() can peek back at it.
     */
    private static final class AssertingUniqueKeys<U> implements BinaryOperator<U> {
        @Override
        public U apply(U u1, U u2) {
            throw new IllegalStateException();
        }

        private static final AssertingUniqueKeys<Void> INSTANCE = new AssertingUniqueKeys<>();

        @SuppressWarnings("unchecked") // safe because the arguments are ignored
        public static <U> BinaryOperator<U> instance() {
            return (BinaryOperator<U>) INSTANCE;
        }
    }


    private interface MapStepImpl<T, K, U> {
        // General case for mutable maps
        <M extends Map<K, U>> Collector<T, ?, M> toMap(Supplier<M> mapFactory);

        // Special handling for Guava's immutable maps. They have custom collectors because they
        // need mutable accumulations (and also have some cool optimizations).

        Collector<T, ?, ImmutableMap<K, U>> toImmutableMap();

        Collector<T, ?, ImmutableSortedMap<K, U>> toImmutableSortedMap(Comparator<? super K> comparator);
    }

    private static final class MapStepWithMergeFunction<T, K, U> implements MapStepImpl<T, K, U> {
        private final Function<? super T, ? extends K> keyMapper;
        private final Function<? super T, ? extends U> valueMapper;
        private final BinaryOperator<U> mergeFunction;

        private MapStepWithMergeFunction(Function<? super T, ? extends K> keyMapper,
                                         Function<? super T, ? extends U> valueMapper,
                                         BinaryOperator<U> mergeFunction) {
            this.keyMapper = keyMapper;
            this.valueMapper = valueMapper;
            this.mergeFunction = mergeFunction;
        }

        @Override
        public <M extends Map<K, U>> Collector<T, ?, M> toMap(Supplier<M> mapFactory) {
            return Collectors.toMap(keyMapper, valueMapper, mergeFunction, Objects.requireNonNull(mapFactory));
        }

        @Override
        public Collector<T, ?, ImmutableMap<K, U>> toImmutableMap() {
            // If there is no merge function, ImmutableMap.toImmutableMap uses an
            // ImmutableMap.Builder as its accumulation, which allows the key collision check to be
            // combined with the process of building the hashtable. So, we peek back to the
            // MergeStep and, if it was `unique()`, preserve the optimization.
            return mergeFunction instanceof AssertingUniqueKeys
                    ? ImmutableMap.toImmutableMap(keyMapper, valueMapper)
                    : ImmutableMap.toImmutableMap(keyMapper, valueMapper, mergeFunction);
        }

        @Override
        public Collector<T, ?, ImmutableSortedMap<K, U>> toImmutableSortedMap(Comparator<? super K> comparator) {
            return ImmutableSortedMap.toImmutableSortedMap(
                    comparator, keyMapper, valueMapper, mergeFunction);
        }

    }

    private static final class MapStepWithDownstreamGrouping<T, K, U, A, D> implements MapStepImpl<T, K, D> {
        private final Function<? super T, ? extends K> keyMapper;
        private final MultimapValueMapper<T, U> valueMapper;
        private final Collector<? super U, A, D> downstream;

        private MapStepWithDownstreamGrouping(Function<? super T, ? extends K> keyMapper,
                                              MultimapValueMapper<T, U> valueMapper,
                                              Collector<? super U, A, D> downstream) {
            this.keyMapper = keyMapper;
            this.valueMapper = valueMapper;
            this.downstream = downstream;
        }

        /*
         * Does much the same thing as Collectors.groupingBy(Function, Supplier, Collector), but
         * with the possibility of additional behavior from the valueMapper.
         */
        @SuppressWarnings("unchecked")
        @Override
        public <M extends Map<K, D>> Collector<T, ?, M> toMap(Supplier<M> mapFactory) {
            Supplier<A> downstreamSupplier = downstream.supplier();
            BiConsumer<A, ? super U> downstreamAccumulator = downstream.accumulator();
            BinaryOperator<A> downstreamCombiner = downstream.combiner();

            Supplier<Map<K, A>> accumulatingMapFactory = (Supplier<Map<K, A>>) Objects.requireNonNull(mapFactory);
            BiConsumer<Map<K, A>, T> accumulator = (map, element) -> {
                A accumulation = map.computeIfAbsent(keyMapper.apply(element),
                        k -> downstreamSupplier.get());
                valueMapper.accumulate(downstreamAccumulator, accumulation, element);
            };
            BinaryOperator<Map<K, A>> combiner = (map1, map2) -> {
                for (Map.Entry<K, A> entry : map2.entrySet()) {
                    map1.merge(entry.getKey(), entry.getValue(), downstreamCombiner);
                }
                return map1;
            };

            if (downstream.characteristics().contains(Characteristics.IDENTITY_FINISH)) {
                return (Collector<T, ?, M>) Collector.of(
                        accumulatingMapFactory, accumulator, combiner);
            } else {
                Function<A, D> downstreamFinisher = downstream.finisher();
                Function<Map<K, A>, M> finisher = map -> {
                    Map<K, Object> mangledMap = (Map<K, Object>) map;
                    mangledMap.replaceAll((key, value) -> downstreamFinisher.apply((A) value));
                    return (M) mangledMap;
                };
                return Collector.of(accumulatingMapFactory, accumulator, combiner, finisher);
            }
        }

        @Override
        public Collector<T, ?, ImmutableMap<K, D>> toImmutableMap() {
            return Collectors.collectingAndThen(
                    toMap(LinkedHashMap::new),
                    ImmutableMap::copyOf);
        }

        @Override
        public Collector<T, ?, ImmutableSortedMap<K, D>> toImmutableSortedMap(Comparator<? super K> comparator) {
            Objects.requireNonNull(comparator);
            return Collectors.collectingAndThen(
                    toMap(() -> new TreeMap<>(comparator)),
                    m -> ImmutableSortedMap.copyOf(m, comparator));
        }
    }


    private interface MultimapValueMapper<T, U> {
        <A> void accumulate(BiConsumer<A, ? super U> accumulator, A accumulation, T element);

        <K, M extends Multimap<K, U>> Collector<T, ?, M> toMultimap(Function<? super T, ? extends K> keyMapper, Supplier<M> multimapFactory);

        <K> Collector<T, ?, ImmutableListMultimap<K, U>> toImmutableListMultimap(Function<? super T, ? extends K> keyMapper);

        <K> Collector<T, ?, ImmutableSetMultimap<K, U>> toImmutableSetMultimap(Function<? super T, ? extends K> keyMapper);
    }

    private static final class MultimapSingleValueMapper<T, U> implements MultimapValueMapper<T, U> {
        private final Function<? super T, ? extends U> valueMapper;

        private MultimapSingleValueMapper(Function<? super T, ? extends U> valueMapper) {
            this.valueMapper = valueMapper;
        }

        @Override
        public <A> void accumulate(BiConsumer<A, ? super U> accumulator, A accumulation, T element) {
            accumulator.accept(accumulation, valueMapper.apply(element));
        }

        @Override
        public <K, M extends Multimap<K, U>> Collector<T, ?, M> toMultimap(Function<? super T, ? extends K> keyMapper, Supplier<M> multimapFactory) {
            return Multimaps.toMultimap(keyMapper, valueMapper, multimapFactory);
        }

        @Override
        public <K> Collector<T, ?, ImmutableListMultimap<K, U>> toImmutableListMultimap(Function<? super T, ? extends K> keyMapper) {
            return ImmutableListMultimap.toImmutableListMultimap(keyMapper, valueMapper);
        }

        @Override
        public <K> Collector<T, ?, ImmutableSetMultimap<K, U>> toImmutableSetMultimap(Function<? super T, ? extends K> keyMapper) {
            return ImmutableSetMultimap.toImmutableSetMultimap(keyMapper, valueMapper);
        }
    }

    private static final class MultimapStreamValueMapper<T, U> implements MultimapValueMapper<T, U> {
        private final Function<? super T, Stream<? extends U>> valueStreamMapper;

        private MultimapStreamValueMapper(Function<? super T, Stream<? extends U>> valueStreamMapper) {
            this.valueStreamMapper = valueStreamMapper;
        }

        @Override
        public <A> void accumulate(BiConsumer<A, ? super U> accumulator, A accumulation, T element) {
            valueStreamMapper.apply(element)
                    .forEachOrdered(value -> accumulator.accept(accumulation, value));
        }

        @SuppressWarnings("UnstableApiUsage")
        @Override
        public <K, M extends Multimap<K, U>> Collector<T, ?, M> toMultimap(Function<? super T, ? extends K> keyMapper,
                                                                           Supplier<M> multimapFactory) {
            return Multimaps.flatteningToMultimap(keyMapper, valueStreamMapper, multimapFactory);
        }

        @Override
        public <K> Collector<T, ?, ImmutableListMultimap<K, U>> toImmutableListMultimap(Function<? super T, ? extends K> keyMapper) {
            return ImmutableListMultimap.flatteningToImmutableListMultimap(
                    keyMapper, valueStreamMapper);
        }

        @Override
        public <K> Collector<T, ?, ImmutableSetMultimap<K, U>> toImmutableSetMultimap(Function<? super T, ? extends K> keyMapper) {
            return ImmutableSetMultimap.flatteningToImmutableSetMultimap(
                    keyMapper, valueStreamMapper);
        }
    }

}
