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
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import io.github.ryanskonnord.lambdagoyf.scryfall.ScryfallCardFace;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

public final class CardFace implements Comparable<CardFace> {

    private final Card parent;
    private final int faceIndex;

    private final String name;
    private final Optional<String> manaCost;
    private final TypeLine typeLine;
    private final String oracleText;
    private final ColorSet colors;
    private final Optional<ColorSet> colorIndicator;
    private final Optional<WrittenNumber> power;
    private final Optional<WrittenNumber> toughness;
    private final Optional<WrittenNumber> loyalty;

    CardFace(CardFactory factory, Card parent, int faceIndex, Collection<ScryfallCardFace> data) {
        Preconditions.checkArgument(faceIndex >= 0);
        this.parent = Objects.requireNonNull(parent);
        this.faceIndex = faceIndex;

        RedundantBuilder<ScryfallCardFace> builder = new RedundantBuilder<>(data);
        name = builder.getCommon(ScryfallCardFace::getName);
        manaCost = factory.cacheManaCost(builder.getCommon(ScryfallCardFace::getManaCost).filter(((Predicate<String>) String::isEmpty).negate()));
        typeLine = builder.getCommon(ScryfallCardFace::getTypeLine).map(TypeLine::parse).map(factory::getCachedTypeLine).orElse(TypeLine.EMPTY);
        oracleText = builder.getCommon(ScryfallCardFace::getOracleText).orElseThrow(Card.ScryfallParsingException::new);
        colors = builder.getCommon(f -> f.getColors().map(ColorSet::fromStrings)).orElse(ColorSet.of());
        colorIndicator = builder.getCommon(f -> f.getColorIndicator().map(ColorSet::fromStrings));
        power = builder.getCommon(ScryfallCardFace::getPower).map(WrittenNumber::create);
        toughness = builder.getCommon(ScryfallCardFace::getToughness).map(WrittenNumber::create);
        loyalty = builder.getCommon(ScryfallCardFace::getLoyalty).map(WrittenNumber::create);
    }

    public Card getParent() {
        return parent;
    }

    public int getFaceIndex() {
        return faceIndex;
    }

    private transient ImmutableList<CardEditionFace> editions;

    public ImmutableList<CardEditionFace> getEditions() {
        return (editions != null) ? editions : (editions =
                parent.getEditions().stream()
                        .map((CardEdition v) -> v.getFaces().get(faceIndex))
                        .collect(ImmutableList.toImmutableList()));
    }

    public String getName() {
        return name;
    }

    public Optional<String> getManaCost() {
        return manaCost;
    }

    public TypeLine getTypeLine() {
        return typeLine;
    }

    public String getOracleText() {
        return oracleText;
    }

    public List<String> getOracleTextParagraphs() {
        return Splitter.on('\n').splitToList(getOracleText());
    }

    public ColorSet getColors() {
        return colors;
    }

    public Optional<ColorSet> getColorIndicator() {
        return colorIndicator;
    }

    public Optional<WrittenNumber> getPower() {
        return power;
    }

    public Optional<WrittenNumber> getToughness() {
        return toughness;
    }

    public Optional<WrittenNumber> getLoyalty() {
        return loyalty;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public int compareTo(CardFace that) {
        if (this == that) return 0;
        int cmp = this.parent.compareTo(that.parent);
        if (cmp != 0) return cmp;
        return this.faceIndex - that.faceIndex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CardFace cardFace = (CardFace) o;
        return faceIndex == cardFace.faceIndex && parent.equals(cardFace.parent);
    }

    @Override
    public int hashCode() {
        return 31 * parent.hashCode() + faceIndex;
    }
}
