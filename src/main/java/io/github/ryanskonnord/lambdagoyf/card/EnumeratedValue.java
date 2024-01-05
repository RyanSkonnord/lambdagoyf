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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Maps;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public final class EnumeratedValue<T extends Enum<T>> implements Comparable<EnumeratedValue<T>> {

    public static final class Universe<T extends Enum<T>> {
        private final ImmutableMap<T, Map<String, EnumeratedValue<T>>> instances;

        public Universe(Class<T> fieldType) {
            instances = EnumSet.allOf(fieldType).stream().collect(Maps.toImmutableEnumMap(
                    Function.<T>identity(),
                    (T f) -> new HashMap<>()));
        }

        public EnumeratedValue<T> get(T field, String name) {
            Map<String, EnumeratedValue<T>> fieldMap = instances.get(Objects.requireNonNull(field));
            EnumeratedValue<T> value = fieldMap.get(Objects.requireNonNull(name));
            if (value == null) {
                synchronized (fieldMap) {
                    value = fieldMap.get(name);
                    if (value == null) {
                        value = new EnumeratedValue<T>(field, name);
                        fieldMap.put(name, value);
                    }
                }
            }
            return value;
        }

        public ImmutableSortedSet<String> getAllNames(T field) {
            return ImmutableSortedSet.copyOf(instances.get(field).keySet());
        }

        public ImmutableSortedSet<EnumeratedValue> getAllValues(T field) {
            return ImmutableSortedSet.copyOf(instances.get(field).values());
        }
    }

    private final T field;
    private final String name;

    private EnumeratedValue(T field, String name) {
        this.field = Objects.requireNonNull(field);
        this.name = Objects.requireNonNull(name);
    }

    public T getField() {
        return field;
    }

    public String getName() {
        return name;
    }

    @Override
    public int compareTo(EnumeratedValue<T> that) {
        if (this == that) return 0;
        if (this.field != that.field) {
            if (this.field.getClass() != that.field.getClass()) {
                throw new IllegalArgumentException("Can only compare values with same field type");
            }
            return this.field.ordinal() - that.field.ordinal();
        }
        return this.name.compareTo(that.name);
    }

    @Override
    public String toString() {
        return field + "=" + name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EnumeratedValue that = (EnumeratedValue) o;
        return field == that.field && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return 31 * field.hashCode() + name.hashCode();
    }
}
