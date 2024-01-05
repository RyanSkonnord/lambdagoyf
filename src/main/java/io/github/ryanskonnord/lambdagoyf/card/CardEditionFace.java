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
import io.github.ryanskonnord.lambdagoyf.card.field.Watermark;
import io.github.ryanskonnord.lambdagoyf.scryfall.ScryfallCardFace;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class CardEditionFace implements Comparable<CardEditionFace> {

    private final CardEdition parent;
    private final int faceIndex;

    private final Optional<String> printedName;
    private final Optional<String> flavorName;
    private final Optional<String> artist;
    private final Optional<String> flavorText;
    private final Optional<UUID> illustrationId;
    private final Optional<Word<Watermark>> watermark;

    CardEditionFace(CardEdition parent, int faceIndex, ScryfallCardFace data) {
        this.parent = Objects.requireNonNull(parent);
        this.faceIndex = faceIndex;
        Preconditions.checkArgument(faceIndex >= 0);

        printedName = data.getPrintedName();
        flavorName = data.getFlavorName();
        artist = data.getArtist();
        flavorText = data.getFlavorText();
        illustrationId = data.getIllustrationId();
        watermark = data.getWatermark().map(w -> Word.of(Watermark.class, w));
    }

    public CardEdition getParentEdition() {
        return parent;
    }

    public CardFace getParentFace() {
        return parent.getCard().getFaces().get(faceIndex);
    }

    public Optional<String> getPrintedName() {
        return printedName;
    }

    public Optional<String> getFlavorName() {
        return flavorName;
    }

    public Optional<String> getArtist() {
        return artist;
    }

    public Optional<String> getFlavorText() {
        return flavorText;
    }

    public Optional<UUID> getIllustrationId() {
        return illustrationId;
    }

    public Optional<Word<Watermark>> getWatermark() {
        return watermark;
    }

    @Override
    public int compareTo(CardEditionFace that) {
        if (this == that) return 0;
        int cmp = this.parent.compareTo(that.parent);
        if (cmp != 0) return cmp;
        return this.faceIndex - that.faceIndex;
    }

    @Override
    public String toString() {
        return String.format("%s (%s #%s)", getParentFace().getName(),
                parent.getExpansion().getProductCode(), parent.getCollectorNumber());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CardEditionFace that = (CardEditionFace) o;
        return faceIndex == that.faceIndex && parent.equals(that.parent);
    }

    @Override
    public int hashCode() {
        return 31 * parent.hashCode() + faceIndex;
    }
}
