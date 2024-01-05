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
import com.google.common.collect.Comparators;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.UnmodifiableIterator;
import io.github.ryanskonnord.util.MapCollectors;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class ColorSet implements Set<Color>, Comparable<ColorSet> {

    private static ImmutableBiMap<Set<Color>, ColorSet> createUniverse() {
        /*
         * These strings define the canonical presentation for both the order of each color within
         * its set, and the order of the sets among each other.
         *
         * The modern canonical ordering for the color sets is taken from the deck builder in Magic
         * Arena. Note that the order of the color pairs predates it, having appeared in such
         * places as on the card Tablet of the Guilds.
         *
         * Magic Arena canonizes that the "arc" three-color sets are ordered before the "wedge"
         * three-color sets and that these groups of five are sorted lexicographically (i.e.,
         * starting with the set that has white in the leftmost position). Lexicographic ordering
         * is consistent with the order of the color pairs.
         *
         * The ordering of the colors within the "wedge" sets has been subject to some
         * revision. Historically, the color that is the enemy of the other two (the "point" color;
         * e.g., white for Mardu) was placed in the leftmost position. In "Khans of Tarkir", the
         * "point" color was moved to the center position; this allegedly was done in order to move
         * the "dominant" color of each of the Tarkir clans (e.g., red for Mardu) to the leftmost
         * position, but has the pleasing side effect of being symmetric as positioned on the color
         * wheel. Later wedge-colored cards (e.g., Jodah, Archmage Eternal) have preserved the
         * Tarkir-style ordering, and older cards have been updated to match.
         */
        ImmutableList<String> canonicalOrdering = ImmutableList.of(
                "", "W", "U", "B", "R", "G",
                "WU", "WB", "UB", "UR", "BR", "BG", "RG", "RW", "GW", "GU",
                "WUB", "UBR", "BRG", "RGW", "GWU", "WBG", "URW", "BWR", "RUG", "GBU",
                "WUBR", "UBRG", "BRGW", "RGWU", "GWUB", "WUBRG");

        ImmutableBiMap<Set<Color>, ColorSet> universe = IntStream.range(0, canonicalOrdering.size())
                .mapToObj((int ordinal) -> new ColorSet(ordinal, canonicalOrdering.get(ordinal)))
                .collect(MapCollectors.<ColorSet>collecting()
                        .indexing(cs -> (Set<Color>) cs)
                        .toImmutableBiMap());

        assert universe.keySet().asList().equals(universe.values().asList());
        assert universe.keySet().equals(Sets.powerSet(EnumSet.allOf(Color.class)));
        assert Comparators.isInOrder(universe.values(), Comparator.comparing(ColorSet::size));
        assert Comparators.isInOrder(universe.values(), Comparator.naturalOrder());

        return universe;
    }

    private final int ordinal;
    private final ImmutableSet<Color> delegateSet;
    private final ImmutableList<Color> order;
    private final String symbols;

    private ColorSet(int ordinal, String symbols) {
        ImmutableSet<Color> orderedSet = symbols.chars()
                .mapToObj((int c) -> Color.fromSymbol((char) c))
                .collect(ImmutableSet.toImmutableSet());

        this.ordinal = ordinal;
        this.symbols = symbols;
        this.delegateSet = Sets.immutableEnumSet(orderedSet);
        this.order = delegateSet.asList().equals(orderedSet.asList()) ? delegateSet.asList() : orderedSet.asList();

        Preconditions.checkArgument(symbols.equals(
                order.stream().map(Color::getSymbol).map(Object::toString).collect(Collectors.joining())));
    }


    private static final ImmutableBiMap<Set<Color>, ColorSet> UNIVERSE = createUniverse();
    private static final ColorSet EMPTY = UNIVERSE.get(ImmutableSet.<Color>of());
    private static final ColorSet ALL = UNIVERSE.get(EnumSet.allOf(Color.class));
    private static final ImmutableMap<Color, ColorSet> SINGLETONS = Maps.immutableEnumMap(
            UNIVERSE.values().stream()
                    .filter((ColorSet s) -> s.size() == 1)
                    .collect(MapCollectors.<ColorSet>collecting()
                            .indexing(Iterables::getOnlyElement)
                            .unique().toImmutableMap()));
    private static final ImmutableMap<String, ColorSet> BY_SYMBOLS = Maps.uniqueIndex(
            UNIVERSE.values(), ColorSet::getSymbols);

    public static ColorSet of() {
        return EMPTY;
    }

    public static ColorSet of(Color c) {
        return SINGLETONS.get(Objects.requireNonNull(c));
    }

    public static ColorSet of(Color c1, Color c2) {
        return UNIVERSE.get(EnumSet.of(c1, c2));
    }

    public static ColorSet of(Color c1, Color c2, Color c3) {
        return UNIVERSE.get(EnumSet.of(c1, c2, c3));
    }

    public static ColorSet of(Color c1, Color c2, Color c3, Color c4) {
        return UNIVERSE.get(EnumSet.of(c1, c2, c3, c4));
    }

    public static ColorSet of(Color c1, Color c2, Color c3, Color c4, Color c5) {
        return UNIVERSE.get(EnumSet.of(c1, c2, c3, c4, c5));
    }

    public static ColorSet of(Color c1, Color c2, Color c3, Color c4, Color c5, Color c6, Color... more) {
        Set<Color> colors = EnumSet.noneOf(Color.class);
        Collections.addAll(colors, c1, c2, c3, c4, c5, c6);
        Collections.addAll(colors, more);
        return UNIVERSE.get(colors);
    }

    public static ColorSet all() {
        return ALL;
    }

    public static ColorSet copyOf(Color[] colors) {
        if (colors.length == 0) return of();
        if (colors.length == 1) return of(colors[0]);
        Set<Color> set = EnumSet.noneOf(Color.class);
        Collections.addAll(set, colors);
        return UNIVERSE.get(set);
    }

    public static ColorSet copyOf(Iterable<Color> colors) {
        if (colors instanceof ColorSet) return (ColorSet) colors;
        ColorSet colorSet = UNIVERSE.get(colors instanceof Set ? (Set<Color>) colors : Sets.immutableEnumSet(colors));
        Preconditions.checkArgument(colorSet != null,
                "Argument is poorly behaved (contains non-Color elements or implements equals/hashCode incorrectly)");
        return colorSet;
    }

    private static final Collector<Color, EnumSet<Color>, ColorSet> COLOR_SET_COLLECTOR = Collectors.collectingAndThen(
            (Collector<Color, EnumSet<Color>, EnumSet<Color>>) Collectors.toCollection(() -> EnumSet.noneOf(Color.class)),
            ColorSet::copyOf);

    public static Collector<Color, ?, ColorSet> toColorSet() {
        return COLOR_SET_COLLECTOR;
    }

    public static ColorSet fromSymbols(String symbols) {
        ColorSet colors = BY_SYMBOLS.get(symbols);
        if (colors != null) return colors;
        return symbols.chars()
                .mapToObj((int c) -> Color.fromSymbol((char) c))
                .collect(toColorSet());
    }

    public static ColorSet fromStrings(Collection<String> strings) {
        return strings.stream().map(Color::fromString).collect(toColorSet());
    }


    public static ImmutableSet<ColorSet> getPowerSet() {
        return UNIVERSE.values();
    }

    public static final Comparator<ColorSet> COLORLESS_LAST = Comparator.comparingInt(cs -> (cs.ordinal - 1) & 31);


    public String getSymbols() {
        return symbols;
    }

    @Override
    public int compareTo(ColorSet that) {
        return this.ordinal - that.ordinal;
    }

    public ImmutableList<Color> asList() {
        return order;
    }

    @Override
    public boolean isEmpty() {
        return delegateSet.isEmpty();
    }

    @Override
    public int size() {
        return delegateSet.size();
    }

    @Override
    public boolean contains(@Nullable Object object) {
        return delegateSet.contains(object);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return delegateSet.containsAll(c instanceof ColorSet ? ((ColorSet) c).delegateSet : c);
    }

    @Override
    public UnmodifiableIterator<Color> iterator() {
        return order.iterator();
    }

    @Override
    public Color[] toArray() {
        return toArray(new Color[size()]);
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return order.toArray(a);
    }

    @Override
    public <T> T[] toArray(IntFunction<T[]> generator) {
        return order.toArray(generator.apply(size()));
    }

    @Override
    public String toString() {
        return order.toString();
    }

    @Override
    public boolean equals(@Nullable Object object) {
        return (object instanceof ColorSet) ? (ordinal == ((ColorSet) object).ordinal) : delegateSet.equals(object);
    }

    @Override
    public int hashCode() {
        return delegateSet.hashCode();
    }


    @Deprecated
    @Override
    public boolean add(Color color) {
        throw new UnsupportedOperationException("immutable");
    }

    @Deprecated
    @Override
    public boolean addAll(Collection<? extends Color> c) {
        throw new UnsupportedOperationException("immutable");
    }

    @Deprecated
    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("immutable");
    }

    @Deprecated
    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("immutable");
    }

    @Deprecated
    @Override
    public boolean removeIf(Predicate<? super Color> filter) {
        throw new UnsupportedOperationException("immutable");
    }

    @Deprecated
    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("immutable");
    }

    @Deprecated
    @Override
    public void clear() {
        throw new UnsupportedOperationException("immutable");
    }
}
