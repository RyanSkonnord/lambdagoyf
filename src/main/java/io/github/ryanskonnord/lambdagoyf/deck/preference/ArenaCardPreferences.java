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

import io.github.ryanskonnord.lambdagoyf.card.ArenaCard;
import io.github.ryanskonnord.lambdagoyf.card.CardEdition;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.Predicate;

public final class ArenaCardPreferences {
    private ArenaCardPreferences() {
    }

    public static Predicate<ArenaCard> onEdition(Predicate<? super CardEdition> editionPredicate) {
        Objects.requireNonNull(editionPredicate);
        return (ArenaCard arenaCard) -> editionPredicate.test(arenaCard.getEdition());
    }

    public static Comparator<ArenaCard> onEditions(Comparator<? super CardEdition> editionComparator) {
        Objects.requireNonNull(editionComparator);
        return (ArenaCard o1, ArenaCard o2) -> editionComparator.compare(o1.getEdition(), o2.getEdition());
    }

}
