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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public abstract class WordSet<E extends Enum<E> & WordType> implements Iterable<Word<E>> {

    public abstract List<Word<E>> asList();

    public abstract boolean contains(E value);

    public boolean is(E... values) {
        List<Word<E>> list = asList();
        int length = values.length;
        if (list.size() != length) return false;
        for (int i = 0; i < length; i++) {
            if (!list.get(i).is(values[i])) return false;
        }
        return true;
    }

    @Override
    public Iterator<Word<E>> iterator() {
        return asList().iterator();
    }

    @Override
    public String toString() {
        return asList().toString();
    }

    @Override
    public final boolean equals(Object o) {
        return this == o || o instanceof WordSet && asList().equals(((WordSet<?>) o).asList());
    }

    @Override
    public final int hashCode() {
        return asList().hashCode();
    }


    @SuppressWarnings("unchecked")
    public static <E extends Enum<E> & WordType> WordSet<E> empty() {
        return (WordSet<E>) EMPTY;
    }

    public static <E extends Enum<E> & WordType> WordSet<E> ofWords(Class<E> type, String... words) {
        return ofWords(type, Arrays.asList(words));
    }

    public static <E extends Enum<E> & WordType> WordSet<E> ofWords(Class<E> type, Collection<String> words) {
        return words.stream().map(Word.constructor(type)).collect(toWordSet());
    }

    public static <E extends Enum<E> & WordType> WordSet<E> copyWords(Collection<Word<E>> words) {
        Iterator<Word<E>> iterator = words.iterator();
        if (!iterator.hasNext()) return empty();
        boolean haveFoundNonEnumElement = false;
        Set<E> enumSet = null;
        while (!haveFoundNonEnumElement && iterator.hasNext()) {
            Optional<E> next = iterator.next().getEnum();
            if (next.isPresent()) {
                if (enumSet == null) {
                    enumSet = EnumSet.of(next.get());
                } else {
                    enumSet.add(next.get());
                }
            } else {
                haveFoundNonEnumElement = true;
            }
        }
        if (!haveFoundNonEnumElement && didPreserveOrder(enumSet, words)) {
            return new Optimized<>(enumSet);
        } else {
            return new Basic<>(words);
        }
    }

    private static <E extends Enum<E> & WordType> boolean didPreserveOrder(Set<E> enumSet, Collection<Word<E>> words) {
        if (words.size() <= 1) return true;
        Iterator<E> enumSetIterator = enumSet.iterator();
        Iterator<Word<E>> wordIterator = words.iterator();
        while (enumSetIterator.hasNext() && wordIterator.hasNext()) {
            if (!wordIterator.next().is(enumSetIterator.next())) {
                return false;
            }
        }
        return enumSetIterator.hasNext() == wordIterator.hasNext();
    }

    public static <E extends Enum<E> & WordType> WordSet<E> ofEnums(E... values) {
        return copyEnums(Arrays.asList(values));
    }

    public static <E extends Enum<E> & WordType> WordSet<E> copyEnums(Collection<E> values) {
        ImmutableSet<E> optimized = Sets.immutableEnumSet(values);
        return optimized.isEmpty() ? empty()
                : optimized.size() == 1 || Iterables.elementsEqual(values, optimized) ? new Optimized<>(optimized)
                : new Basic<>(values.stream().map(Word::of).collect(ImmutableSet.toImmutableSet()));
    }

    public static <E extends Enum<E> & WordType> Collector<Word<E>, ?, WordSet<E>> toWordSet() {
        return Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), WordSet::copyWords);
    }


    private static final WordSet<?> EMPTY = new WordSet() {
        @Override
        public List<Word> asList() {
            return ImmutableList.of();
        }

        @Override
        public boolean contains(Enum value) {
            return false;
        }
    };

    private static class Basic<E extends Enum<E> & WordType> extends WordSet<E> {
        private final ImmutableSet<Word<E>> delegate;

        private Basic(Collection<Word<E>> delegate) {
            this.delegate = ImmutableSet.copyOf(delegate);
        }

        @Override
        public List<Word<E>> asList() {
            return delegate.asList();
        }

        @Override
        public boolean contains(E value) {
            return delegate.stream().anyMatch(w -> w.is(value));
        }
    }

    private static final class Optimized<E extends Enum<E> & WordType> extends WordSet<E> {
        private final ImmutableSet<E> delegate;

        private Optimized(Set<E> delegate) {
            this.delegate = Sets.immutableEnumSet(delegate);
        }

        @Override
        public List<Word<E>> asList() {
            return Lists.transform(delegate.asList(), Word::of);
        }

        @Override
        public boolean contains(E value) {
            return delegate.contains(value);
        }
    }

}
