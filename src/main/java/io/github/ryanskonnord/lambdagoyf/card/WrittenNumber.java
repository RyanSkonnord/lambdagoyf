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
import com.google.common.collect.ImmutableMap;
import io.github.ryanskonnord.util.MapCollectors;

import java.util.OptionalInt;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class WrittenNumber implements Comparable<WrittenNumber> {

    private final String writtenValue;
    private final OptionalInt numericValue;

    private WrittenNumber(String writtenValue) {
        Preconditions.checkArgument(!writtenValue.isEmpty());
        this.writtenValue = writtenValue;
        this.numericValue = parseIfNumeric(writtenValue);
    }

    private static final ImmutableMap<String, WrittenNumber> FLYWEIGHTS = Stream
            .of(
                    IntStream.rangeClosed(-1, 16).mapToObj(Integer::toString),
                    Stream.of("*", "X"),
                    IntStream.rangeClosed(1, 2).mapToObj((int n) -> n + "+*"),
                    IntStream.rangeClosed(1, 4).mapToObj((int n) -> "+" + n))
            .flatMap(Function.identity())
            .map(WrittenNumber::new)
            .sorted()
            .collect(MapCollectors.<WrittenNumber>collecting()
                    .indexing(WrittenNumber::getWrittenValue)
                    .unique().toImmutableMap());

    public static WrittenNumber create(String writtenValue) {
        WrittenNumber value = FLYWEIGHTS.get(writtenValue);
        return value != null ? value : new WrittenNumber(writtenValue);
    }

    private static OptionalInt parseIfNumeric(String writtenValue) {
        try {
            return OptionalInt.of(Integer.parseInt(writtenValue));
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }

    public String getWrittenValue() {
        return writtenValue;
    }

    public OptionalInt getNumericValue() {
        return numericValue;
    }

    @Override
    public int compareTo(WrittenNumber that) {
        return (this == that) ? 0
                : this.numericValue.isPresent() && that.numericValue.isPresent()
                ? this.numericValue.getAsInt() - that.numericValue.getAsInt()
                : this.numericValue.isPresent() ? -1 : that.numericValue.isPresent() ? 1
                : this.writtenValue.compareTo(that.writtenValue);
    }

    @Override
    public String toString() {
        return writtenValue;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || !(o == null || getClass() != o.getClass())
                && writtenValue.equals(((WrittenNumber) o).writtenValue);
    }

    @Override
    public int hashCode() {
        return writtenValue.hashCode();
    }
}
