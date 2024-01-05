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

import io.github.ryanskonnord.lambdagoyf.card.CardEdition;
import io.github.ryanskonnord.lambdagoyf.card.CardFace;
import io.github.ryanskonnord.lambdagoyf.card.MtgoCard;
import io.github.ryanskonnord.lambdagoyf.card.TypeLine;
import io.github.ryanskonnord.lambdagoyf.card.field.BorderColor;
import io.github.ryanskonnord.lambdagoyf.card.field.CardSupertype;
import io.github.ryanskonnord.lambdagoyf.card.field.Finish;
import io.github.ryanskonnord.lambdagoyf.card.field.FrameStyle;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.Predicate;

public final class MtgoCardPreferences {
    private MtgoCardPreferences() {
    }

    public static Predicate<MtgoCard> onEdition(Predicate<? super CardEdition> editionPredicate) {
        Objects.requireNonNull(editionPredicate);
        return (MtgoCard mtgoCard) -> editionPredicate.test(mtgoCard.getEdition());
    }

    public static Comparator<MtgoCard> onEditions(Comparator<? super CardEdition> editionComparator) {
        Objects.requireNonNull(editionComparator);
        return (MtgoCard o1, MtgoCard o2) -> editionComparator.compare(o1.getEdition(), o2.getEdition());
    }

    public static Predicate<MtgoCard> hasFinish(Finish finish) {
        Objects.requireNonNull(finish);
        return (MtgoCard mtgoCard) -> (mtgoCard.getFinish() == finish);
    }

    public static Predicate<MtgoCard> hasBorderColor(BorderColor borderColor) {
        Objects.requireNonNull(borderColor);
        return (MtgoCard mtgoCard) -> mtgoCard.getBorderColor().is(borderColor);
    }

    public static Predicate<MtgoCard> hasMechanicalFrameUpdate() {
        return (MtgoCard mtgoCard) -> {
            CardEdition cardEdition = mtgoCard.getEdition();
            return cardEdition.getFrameStyle().getEnum().filter(style -> style.compareTo(FrameStyle._2015) >= 0).isPresent() &&
                    cardEdition.getCard().getFaces().stream()
                            .anyMatch((CardFace face) -> {
                                TypeLine typeLine = face.getTypeLine();
                                return typeLine.is(CardSupertype.LEGENDARY) || typeLine.is(CardSupertype.SNOW);
                            });
        };
    }

}
