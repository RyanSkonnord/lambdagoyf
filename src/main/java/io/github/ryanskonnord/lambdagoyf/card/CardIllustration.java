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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class CardIllustration {

    private final ImmutableList<UUID> illustrationIds;

    private CardIllustration(List<UUID> illustrationIds) {
        this.illustrationIds = ImmutableList.copyOf(illustrationIds);
    }

    public static CardIllustration from(List<CardEditionFace> faces) {
        List<UUID> illustrationIds = faces.stream()
                .map(CardEditionFace::getIllustrationId)
                .flatMap(Optional::stream)
                .collect(ImmutableList.toImmutableList());
        return new CardIllustration(illustrationIds);
    }

    public ImmutableList<UUID> getIllustrationIds() {
        return illustrationIds;
    }

    @Override
    public String toString() {
        return illustrationIds.toString();
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o != null && getClass() == o.getClass()
                && illustrationIds.equals(((CardIllustration) o).illustrationIds);
    }

    @Override
    public int hashCode() {
        return illustrationIds.hashCode();
    }
}
