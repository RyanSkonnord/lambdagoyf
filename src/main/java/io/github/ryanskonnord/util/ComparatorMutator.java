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

import com.google.common.base.Preconditions;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

public final class ComparatorMutator<T, B> {

    private final B chainingObject;
    private Comparator<T> state = null;

    public ComparatorMutator(B chainingObject) {
        this.chainingObject = chainingObject;
    }

    public static <T> ComparatorMutator<T, Void> nonChaining() {
        return new ComparatorMutator<>(null);
    }

    public B set(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator);
        Preconditions.checkState(state == null, "Comparator has already been set");
        state = (Comparator<T>) comparator;
        return chainingObject;
    }

    public B override(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator);
        if (state == null) {
            state = (Comparator<T>) comparator;
        } else {
            state = ((Comparator<T>) comparator).thenComparing(state);
        }
        return chainingObject;
    }

    public B tiebreak(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator);
        if (state == null) {
            state = (Comparator<T>) comparator;
        } else {
            state = state.thenComparing(comparator);
        }
        return chainingObject;
    }

    public Optional<Comparator<T>> get() {
        return Optional.ofNullable(state);
    }
}
