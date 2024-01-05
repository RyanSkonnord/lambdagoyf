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

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class OrderingUtil {

    public static <T> Comparator<T> trueFirst(Predicate<? super T> predicate) {
        return (T t1, T t2) -> {
            boolean b1 = predicate.test(t1);
            boolean b2 = predicate.test(t2);
            return (b1 == b2) ? 0 : b1 ? -1 : 1;
        };
    }

    public static <T> Comparator<T> falseFirst(Predicate<? super T> predicate) {
        return OrderingUtil.<T>trueFirst(predicate).reversed();
    }

    public static interface OptionalOrdering<T> {
        Comparator<Optional<T>> emptyFirst();

        Comparator<Optional<T>> emptyLast();
    }

    public static <T> OptionalOrdering<T> compareOptional(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator);
        return new OptionalOrdering<T>() {
            private Comparator<Optional<T>> createComparator(int direction) {
                return (Optional<T> o1, Optional<T> o2) -> o1.isPresent()
                        ? (o2.isPresent() ? comparator.compare(o1.get(), o2.get()) : direction)
                        : (o2.isPresent() ? -direction : 0);
            }

            @Override
            public Comparator<Optional<T>> emptyFirst() {
                return createComparator(1);
            }

            @Override
            public Comparator<Optional<T>> emptyLast() {
                return createComparator(-1);
            }
        };
    }

    public static <T extends Comparable<T>> Comparator<Optional<T>> emptyFirst() {
        return OrderingUtil.<T>compareOptional(Comparator.naturalOrder()).emptyFirst();
    }

    public static <T extends Comparable<T>> Comparator<Optional<T>> emptyLast() {
        return OrderingUtil.<T>compareOptional(Comparator.naturalOrder()).emptyLast();
    }

    public static <T extends Comparable<T>> int compareLexicographically(Iterable<? extends T> o1,
                                                                         Iterable<? extends T> o2) {
        return compareLexicographically(Comparator.naturalOrder(), o1, o2);
    }

    public static <T> int compareLexicographically(Comparator<? super T> comparator,
                                                   Iterable<? extends T> o1, Iterable<? extends T> o2) {
        Iterator<? extends T> i1 = o1.iterator();
        Iterator<? extends T> i2 = o2.iterator();
        while (i1.hasNext() && i2.hasNext()) {
            int cmp = comparator.compare(i1.next(), i2.next());
            if (cmp != 0) return cmp;
        }
        return i1.hasNext() ? 1 : i2.hasNext() ? -1 : 0;
    }

    public static <T> Comparator<Iterable<T>> lexicographic(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator);
        return (Iterable<T> o1, Iterable<T> o2) -> compareLexicographically(comparator, o1, o2);
    }

    private static final Comparator NATURAL_LEXICOGRAPHIC = lexicographic(Comparator.naturalOrder());

    public static <T extends Comparable<T>> Comparator<Iterable<T>> lexicographic() {
        return NATURAL_LEXICOGRAPHIC;
    }

    public static <T> List<T> findAllMin(Comparator<? super T> comparator, Collection<? extends T> elements) {
        List<T> sorted = elements.stream().sorted(comparator).collect(Collectors.toList());
        int limit = 0;
        while (limit < elements.size()) {
            if (limit == 0 || comparator.compare(sorted.get(limit - 1), sorted.get(limit)) == 0) {
                limit++;
            } else {
                break;
            }
        }
        return Collections.unmodifiableList(sorted.subList(0, limit));
    }

    public static <T> List<T> findAllMax(Comparator<? super T> comparator, Collection<? extends T> elements) {
        return findAllMin(comparator.reversed(), elements);
    }

    public static <T> ImmutableMap<T, Integer> toIndexMap(Iterable<? extends T> elements) {
        ImmutableMap.Builder<T, Integer> builder = ImmutableMap.builder();
        int index = 0;
        for (T element : elements) {
            builder = builder.put(element, index++);
        }
        return builder.build();
    }

    public static <T> Iterable<ImmutableList<T>> getSortedEqualGroups(Comparator<? super T> comparator,
                                                                      Iterable<? extends T> elements) {
        ImmutableList<T> sorted = ImmutableList.sortedCopyOf(comparator, elements);
        return () -> new AbstractIterator<ImmutableList<T>>() {
            private int cursor = 0;

            @Override
            protected ImmutableList<T> computeNext() {
                if (cursor == sorted.size()) return endOfData();
                int end = cursor + 1;
                for (; end < sorted.size(); end++) {
                    int cmp = comparator.compare(sorted.get(cursor), sorted.get(end));
                    if (cmp < 0) break;
                    if (cmp > 0) throw new IllegalStateException("Comparator is inconsistent");
                }
                ImmutableList<T> value = sorted.subList(cursor, end);
                cursor = end;
                return value;
            }
        };
    }

}
