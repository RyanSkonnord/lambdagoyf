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

import com.google.common.base.Preconditions;
import io.github.ryanskonnord.lambdagoyf.card.Card;
import io.github.ryanskonnord.lambdagoyf.card.CardVersion;

import java.util.Objects;
import java.util.Optional;

public class DeckEntry<V extends CardVersion> {

    private final Card card;
    private final Optional<V> version;

    public DeckEntry(Card card, Optional<V> version) {
        this.card = Objects.requireNonNull(card);
        this.version = Objects.requireNonNull(version);
        Preconditions.checkArgument(version.isEmpty() || version.get().getCard().equals(card));
    }

    public Card getCard() {
        return card;
    }

    public String getName() {
        return card.getMainName();
    }

    public Optional<V> getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return version.map(Object::toString).orElseGet(this::getName);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o != null && getClass() == o.getClass()
                && card.equals(((DeckEntry<?>) o).card)
                && version.equals(((DeckEntry<?>) o).version);
    }

    @Override
    public int hashCode() {
        return 31 * card.hashCode() + version.hashCode();
    }
}
