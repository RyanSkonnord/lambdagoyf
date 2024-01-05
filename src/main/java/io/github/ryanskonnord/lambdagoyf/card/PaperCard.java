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

import io.github.ryanskonnord.lambdagoyf.card.field.Finish;

public final class PaperCard implements FinishedCardVersion, Comparable<PaperCard> {

    private final CardEdition cardEdition;
    private final Finish finish;

    PaperCard(CardEdition cardEdition, Finish finish) {
        this.cardEdition = cardEdition;
        this.finish = finish;
    }

    @Override
    public CardEdition getEdition() {
        return cardEdition;
    }

    @Override
    public Finish getFinish() {
        return finish;
    }

    @Override
    public int compareTo(PaperCard that) {
        if (this == that) return 0;
        int cmp = this.cardEdition.compareTo(that.cardEdition);
        if (cmp != 0) return cmp;
        return this.finish.compareTo(that.finish);
    }

    @Override
    public String toString() {
        return String.format("%s (%s %s #%s)",
                cardEdition.getCard().getFullName(), finish.name().toLowerCase(),
                cardEdition.getExpansion().getProductCode(), cardEdition.getCollectorNumber());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PaperCard)) return false;
        PaperCard paperCard = (PaperCard) o;
        return finish == paperCard.finish && cardEdition.equals(paperCard.cardEdition);
    }

    @Override
    public int hashCode() {
        return 31 * cardEdition.hashCode() + finish.hashCode();
    }
}
