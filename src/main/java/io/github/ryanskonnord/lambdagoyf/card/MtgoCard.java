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
import io.github.ryanskonnord.lambdagoyf.card.field.BorderColor;
import io.github.ryanskonnord.lambdagoyf.card.field.Finish;

import java.util.Comparator;
import java.util.Objects;

public final class MtgoCard implements FinishedCardVersion, Comparable<MtgoCard> {

    private final long mtgoId;
    private final CardEdition cardEdition;
    private final Finish finish;

    MtgoCard(long mtgoId, CardEdition cardEdition, Finish finish) {
        Preconditions.checkArgument(mtgoId >= 0L);
        this.mtgoId = mtgoId;
        this.cardEdition = Objects.requireNonNull(cardEdition);
        this.finish = Objects.requireNonNull(finish);
    }

    public long getMtgoId() {
        return mtgoId;
    }

    @Override
    public CardEdition getEdition() {
        return cardEdition;
    }

    @Override
    public Finish getFinish() {
        return finish;
    }

    public Word<BorderColor> getBorderColor() {
        Word<BorderColor> editionColor = cardEdition.getBorderColor();
        return (finish == Finish.FOIL && editionColor.is(BorderColor.WHITE))
                ? Word.of(BorderColor.BLACK) : editionColor;
    }

    public static final Comparator<MtgoCard> EDITION_ORDER = Comparator.comparing(MtgoCard::getEdition)
            .thenComparing(MtgoCard::getFinish);
    public static final Comparator<MtgoCard> CARD_ORDER = Comparator.comparing(MtgoCard::getCard)
            .thenComparing(EDITION_ORDER);

    @Override
    public int compareTo(MtgoCard that) {
        return Long.compare(this.mtgoId, that.mtgoId);
    }

    @Override
    public String toString() {
        Expansion expansion = cardEdition.getExpansion();
        return String.format("%s (%d: %s %s #%s)",
                cardEdition.getCard().getFullName(), mtgoId, finish.toString().toLowerCase(),
                expansion.getMtgoCode().orElse(expansion.getProductCode()), cardEdition.getCollectorNumber());
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof MtgoCard && mtgoId == ((MtgoCard) o).mtgoId;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(mtgoId);
    }
}
