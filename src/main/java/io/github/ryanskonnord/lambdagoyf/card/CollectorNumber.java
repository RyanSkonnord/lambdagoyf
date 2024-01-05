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
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public final class CollectorNumber implements Comparable<CollectorNumber> {

    private final String collectorString;
    private final String prefix;
    private final int number;

    private CollectorNumber(int number) {
        this(Integer.toString(number), "", number);
    }

    private CollectorNumber(String original, String prefix, int number) {
        Preconditions.checkArgument(number >= 0);
        this.collectorString = Objects.requireNonNull(original);
        this.prefix = Objects.requireNonNull(prefix);
        this.number = number;
    }

    private static final ImmutableList<CollectorNumber> FLYWEIGHTS = IntStream.rangeClosed(0, 350)
            .mapToObj(CollectorNumber::new)
            .collect(ImmutableList.toImmutableList());

    private static final Pattern NUMBER_PATTERN = Pattern.compile("(?<prefix>\\d*[^\\d]+)?(?<number>\\d+)(?<suffix>.*?)");

    public static CollectorNumber of(int number) {
        return (number < FLYWEIGHTS.size()) ? FLYWEIGHTS.get(number) : new CollectorNumber(number);
    }

    public static CollectorNumber parse(String collectorString) {
        int number;
        try {
            number = Integer.parseInt(collectorString);
            Preconditions.checkArgument(number >= 0);
            return of(number);
        } catch (NumberFormatException e) {
            // Fall through
        }
        Matcher matcher = NUMBER_PATTERN.matcher(collectorString);
        String prefix;
        if (matcher.matches()) {
            number = Integer.parseInt(matcher.group("number"));
            prefix = Strings.nullToEmpty(matcher.group("prefix"));
        } else {
            number = 0;
            prefix = collectorString;
        }
        return new CollectorNumber(collectorString, prefix, number);
    }

    public int getNumber() {
        return number;
    }

    public String getCollectorString() {
        return collectorString;
    }

    @Override
    public int compareTo(CollectorNumber that) {
        if (this == that) return 0;
        int cmp = this.prefix.compareTo(that.prefix);
        if (cmp != 0) return cmp;
        cmp = this.number - that.number;
        if (cmp != 0) return cmp;
        return this.collectorString.compareTo(that.collectorString);
    }

    @Override
    public String toString() {
        return getCollectorString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CollectorNumber that = (CollectorNumber) o;
        return collectorString.equals(that.collectorString);
    }

    @Override
    public int hashCode() {
        return collectorString.hashCode();
    }
}
