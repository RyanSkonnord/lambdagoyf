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

import com.google.common.collect.MoreCollectors;

import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;

public class AlchemyConverter {
    private static final String ARENA_PREFIX = "A-";

    public static Optional<String> toAlchemyName(String name) {
        return name.startsWith(ARENA_PREFIX) ? Optional.empty() : Optional.of(ARENA_PREFIX + name);
    }

    public static Optional<String> fromAlchemyName(String name) {
        return name.startsWith(ARENA_PREFIX) ? Optional.of(name.substring(ARENA_PREFIX.length())) : Optional.empty();
    }


    private final Spoiler spoiler;

    public AlchemyConverter(Spoiler spoiler) {
        this.spoiler = Objects.requireNonNull(spoiler);
    }

    public Optional<ArenaCard> toAlchemyVersion(ArenaCard card) {
        CardEdition edition = card.getEdition();
        String name = edition.getCard().getMainName();
        return edition.getRelatedParts().stream()
                .flatMap(id -> spoiler.lookUpEditionByUuid(id).stream())
                .filter(e -> toAlchemyName(e.getCard().getMainName()).filter(name::equals).isPresent())
                .flatMap(e -> e.getArenaCard().stream())
                .collect(MoreCollectors.toOptional());
    }

    public Optional<ArenaCard> fromAlchemyVersion(ArenaCard card) {
        CardEdition edition = card.getEdition();
        String name = edition.getCard().getMainName();
        return edition.getRelatedParts().stream()
                .flatMap(id -> spoiler.lookUpEditionByUuid(id).stream())
                .filter(e -> fromAlchemyName(e.getCard().getMainName()).filter(name::equals).isPresent())
                .flatMap(e -> e.getArenaCard().stream())
                .collect(MoreCollectors.toOptional());
    }


    public Optional<Card> toAlchemyVersion(Card card) {
        return toAlchemyName(card.getMainName()).flatMap(name -> getVersionWithConvertedName(card, name));
    }

    public Optional<Card> fromAlchemyVersion(Card card) {
        return fromAlchemyName(card.getMainName()).flatMap(name -> getVersionWithConvertedName(card, name));
    }

    private Optional<Card> getVersionWithConvertedName(Card card, String convertedName) {
        return card.getEditions().stream()
                .flatMap(c -> c.getRelatedParts().stream())
                .flatMap(id -> spoiler.lookUpEditionByUuid(id).map(CardEdition::getCard).stream())
                .distinct()
                .filter(c -> c.getMainName().equals(convertedName))
                .collect(MoreCollectors.toOptional());
    }

    public UnaryOperator<Card> toAlchemyCardTransformer() {
        return (Card card) -> toAlchemyVersion(card).orElse(card);
    }

    public UnaryOperator<Card> fromAlchemyCardTransformer() {
        return (Card card) -> fromAlchemyVersion(card).orElse(card);
    }
}
