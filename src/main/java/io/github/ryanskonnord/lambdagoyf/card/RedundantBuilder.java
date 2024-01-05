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
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

class RedundantBuilder<E> {

    protected final ImmutableCollection<E> elements;

    public RedundantBuilder(Collection<E> elements) {
        this.elements = ImmutableList.copyOf(elements);
        Preconditions.checkArgument(!this.elements.isEmpty());
    }

    public <T> Optional<T> getCommonIfPresent(Function<? super E, Optional<T>> getter) {
        List<Optional<T>> elements = this.elements.stream().map(getter).filter(Optional::isPresent).distinct().collect(Collectors.toList());
        return elements.isEmpty() ? Optional.empty() : getCommon(elements);
    }

    public <T> T getCommon(Function<? super E, ? extends T> getter) {
        return getCommon(this.elements.stream().map(getter).distinct().collect(Collectors.toList()));
    }

    private <T> T getCommon(List<T> elements) {
        if (elements.size() > 1) {
            System.err.printf("Expected only one common value but got %d: %s", elements.size(), elements);
        }
        return elements.get(0);
    }

}
