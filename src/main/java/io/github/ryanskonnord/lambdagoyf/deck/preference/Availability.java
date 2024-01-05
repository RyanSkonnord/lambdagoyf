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

package io.github.ryanskonnord.lambdagoyf.deck.preference;

import com.google.common.collect.Multiset;

import java.util.Objects;
import java.util.function.Predicate;

@FunctionalInterface
public interface Availability<T> {

    public abstract boolean check(T value, int count);

    public static <T> Availability<T> unlimitedAvailability() {
        return (T value, int count) -> true;
    }

    public static <T> Availability<T> unlimitedAvailabilityIf(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        return (T value, int count) -> predicate.test(value);
    }

    public static <T> Availability<T> fromMultiset(Multiset<? extends T> multiset) {
        Objects.requireNonNull(multiset);
        return (T value, int count) -> multiset.count(value) <= count;
    }

}
