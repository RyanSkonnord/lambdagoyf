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

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public final class PreferenceBuilder<T> {

    private final List<Comparator<? super T>> comparators = new ArrayList<>();

    public PreferenceBuilder<T> addRule(Comparator<? super T> rule) {
        if (rule instanceof PreferenceChain) {
            comparators.addAll(((PreferenceChain<? super T>) rule).comparators);
        } else {
            comparators.add(Objects.requireNonNull(rule));
        }
        return this;
    }

    public PreferenceBuilder<T> prefer(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        return addRule((T o1, T o2) -> Boolean.compare(predicate.test(o2), predicate.test(o1)));
    }

    public PreferenceBuilder<T> preferWithRule(Predicate<? super T> predicate, Comparator<? super T> rule) {
        Objects.requireNonNull(predicate);
        Objects.requireNonNull(rule);
        return addRule((T o1, T o2) -> {
            boolean t1 = predicate.test(o1);
            boolean t2 = predicate.test(o2);
            if (t1 && t2) {
                return rule.compare(o1, o2);
            } else {
                return t1 ? -1 : t2 ? 1 : 0;
            }
        });
    }

    public Comparator<T> build() {
        return new PreferenceChain<>(comparators);
    }

    private static final class PreferenceChain<T> implements Comparator<T> {
        private final ImmutableList<Comparator<? super T>> comparators;

        private PreferenceChain(List<Comparator<? super T>> comparators) {
            this.comparators = ImmutableList.copyOf(comparators);
        }

        @Override
        public int compare(T o1, T o2) {
            if (o1 == o2) return 0;
            for (Comparator<? super T> comparator : comparators) {
                int cmp = comparator.compare(o1, o2);
                if (cmp != 0) return cmp;
            }
            return 0;
        }

        @Override
        public boolean equals(Object o) {
            return this == o || !(o == null || getClass() != o.getClass()) && comparators.equals(((PreferenceChain<?>) o).comparators);
        }

        @Override
        public int hashCode() {
            return comparators.hashCode();
        }
    }

}
