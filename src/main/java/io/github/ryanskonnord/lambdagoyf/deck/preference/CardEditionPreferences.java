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

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.github.ryanskonnord.lambdagoyf.card.CardEdition;
import io.github.ryanskonnord.lambdagoyf.card.CardEditionFace;
import io.github.ryanskonnord.lambdagoyf.card.CardIllustration;
import io.github.ryanskonnord.lambdagoyf.card.Expansion;
import io.github.ryanskonnord.lambdagoyf.card.Word;
import io.github.ryanskonnord.lambdagoyf.card.field.ExpansionType;
import io.github.ryanskonnord.lambdagoyf.card.field.FrameStyle;
import io.github.ryanskonnord.lambdagoyf.card.field.Watermark;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.Predicate;

public final class CardEditionPreferences {

    public static Comparator<CardEdition> olderFirst() {
        return Comparator.comparing(CardEdition::getReleaseDate);
    }

    public static Comparator<CardEdition> newerFirst() {
        return olderFirst().reversed();
    }

    public static Predicate<CardEdition> isExpansionType(ExpansionType expansionType) {
        Objects.requireNonNull(expansionType);
        return (CardEdition edition) -> edition.getExpansion().getType().is(expansionType);
    }

    public static Predicate<CardEdition> isExpansionType(Iterable<ExpansionType> types) {
        ImmutableSet<ExpansionType> expansionTypeSet = ImmutableSet.copyOf(types);
        return (CardEdition edition) ->
                expansionTypeSet.contains(edition.getExpansion().getType().getEnum().orElse(null));
    }

    public static Predicate<CardEdition> isFromExpansionNamed(String... expansionNames) {
        ImmutableCollection<String> defensiveCopy = ImmutableList.copyOf(expansionNames);
        return (CardEdition edition) -> {
            Expansion exp = edition.getExpansion();
            return defensiveCopy.stream().anyMatch(exp::isNamed);
        };
    }

    public static Predicate<CardEdition> onFrameStyle(Predicate<? super FrameStyle> stylePredicate) {
        Objects.requireNonNull(stylePredicate);
        return (CardEdition edition) -> edition.getFrameStyle().getEnum().map(stylePredicate::test).orElse(false);
    }

    public static Predicate<CardEdition> hasModernFrame() {
        return onFrameStyle((FrameStyle frameStyle) -> frameStyle.compareTo(FrameStyle._2003) >= 0);
    }

    public static Comparator<CardEdition> hasLowerRarity() {
        return Comparator.comparing(CardEdition::getRarity);
    }

    public static Comparator<CardEdition> hasHigherRarity() {
        return hasLowerRarity().reversed();
    }

    public static Predicate<CardEdition> hasNewIllustration() {
        return (CardEdition edition) -> {
            CardIllustration illustration = edition.getIllustration();
            for (CardEdition candidate : edition.getEarlierReleases()) {
                if (candidate.getIllustration().equals(illustration)) {
                    return false;
                }
            }
            return true;
        };
    }

    public static Predicate<CardEdition> hasWatermark() {
        return hasWatermark(w -> true);
    }

    public static Predicate<CardEdition> hasWatermark(Predicate<Word<Watermark>> watermarkPredicate) {
        return (CardEdition e) ->
                e.getFaces().stream().anyMatch((CardEditionFace f) ->
                        f.getWatermark().filter(watermarkPredicate).isPresent());
    }

}
