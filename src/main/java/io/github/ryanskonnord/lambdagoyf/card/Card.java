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
import com.google.common.collect.ImmutableListMultimap;
import io.github.ryanskonnord.lambdagoyf.card.field.CardLayout;
import io.github.ryanskonnord.lambdagoyf.card.field.ExpansionType;
import io.github.ryanskonnord.lambdagoyf.scryfall.ScryfallCardEntry;
import io.github.ryanskonnord.lambdagoyf.scryfall.ScryfallCardFace;
import io.github.ryanskonnord.util.MapCollectors;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class Card extends ScryfallEntity
        implements CardIdentity, Comparable<Card> {

    public static final class ScryfallParsingException extends RuntimeException {
        ScryfallParsingException() {
        }

        ScryfallParsingException(String message) {
            super(message);
        }
    }

    private final class ScryfallCardBuilder extends RedundantBuilder<ScryfallCardEntry> {
        private final CardFactory factory;

        private ScryfallCardBuilder(CardFactory factory, Collection<ScryfallCardEntry> elements) {
            super(elements);
            this.factory = Objects.requireNonNull(factory);
        }

        private ImmutableList<CardFace> getFaces() {
            List<List<ScryfallCardFace>> editionFaces = elements.stream()
                    .map((ScryfallCardEntry entry) -> entry.getFaceStream().collect(ImmutableList.toImmutableList()))
                    .collect(ImmutableList.toImmutableList());
            int[] distinctSizes = editionFaces.stream().mapToInt(Collection::size).distinct().toArray();
            if (distinctSizes.length != 1) {
                throw new ScryfallParsingException();
            }
            return IntStream.range(0, distinctSizes[0])
                    .mapToObj((int faceIndex) -> {
                        Collection<ScryfallCardFace> faceData = editionFaces.stream()
                                .map((List<ScryfallCardFace> list) -> list.get(faceIndex))
                                .collect(ImmutableList.toImmutableList());
                        return new CardFace(factory, Card.this, faceIndex, faceData);
                    })
                    .collect(ImmutableList.toImmutableList());
        }

        private ImmutableList<CardEdition> getEditions() {
            boolean isStrict = true;
            return elements.stream()
                    .flatMap((ScryfallCardEntry entry) -> {
                        try {
                            return Stream.of(new CardEdition(factory, Card.this, entry));
                        } catch (ScryfallParsingException e) {
                            if (isStrict) {
                                throw e;
                            } else {
                                return Stream.empty();
                            }
                        }
                    })
                    .sorted()
                    .collect(ImmutableList.toImmutableList());
        }
    }


    private final UUID scryfallId;
    private final String name;

    private final ImmutableList<CardFace> faces;
    private final ImmutableList<CardEdition> editions;
    private final ImmutableListMultimap<CardIllustration, CardEdition> illustrations;

    private final Word<CardLayout> layout;
    private final ColorSet colors;
    private final ColorSet colorIdentity;
    private final int cmc;
    private final CardLegality legalities;
    private final boolean isReserved;
    private final boolean hasContentWarning;

    public Card(CardFactory factory, Collection<ScryfallCardEntry> entries) {
        ScryfallCardBuilder builder = new ScryfallCardBuilder(factory, entries);

        name = builder.getCommon(ScryfallCardEntry::getName);
        scryfallId = builder.getCommon(ScryfallCardEntry::getOracleId);

        faces = builder.getFaces();
        editions = builder.getEditions();
        illustrations = editions.stream().collect(MapCollectors.<CardEdition>collecting()
                .indexing(CardEdition::getIllustration)
                .grouping().toImmutableListMultimap());

        layout = Word.of(CardLayout.class, builder.getCommon(ScryfallCardEntry::getLayout));
        colors = faces.stream().flatMap((CardFace f) -> f.getColors().stream()).collect(ColorSet.toColorSet());
        colorIdentity = builder.getCommon(e -> ColorSet.fromStrings(e.getColorIdentity()));
        cmc = builder.getCommon(ScryfallCardEntry::getCmc).intValue();
        legalities = factory.getLegalityFactory().merge(editions.stream().map(CardEdition::getCardLegality));
        isReserved = builder.getCommon(ScryfallCardEntry::isReserved);
        hasContentWarning = builder.getCommonIfPresent(ScryfallCardEntry::getContentWarning).orElse(false);
    }


    @Override
    public UUID getScryfallId() {
        return scryfallId;
    }

    @Override
    public Card getCard() {
        return this;
    }

    public String getFullName() {
        return name;
    }

    public String getMainName() {
        return getMainFace().map(CardFace::getName).orElse(name);
    }

    public TypeLine getMainTypeLine() {
        TypeLine firstTypeLine = faces.get(0).getTypeLine();
        return !layout.is(CardLayout.SPLIT) ? firstTypeLine
                : firstTypeLine.equals(faces.get(1).getTypeLine()) ? firstTypeLine
                : TypeLine.compose(faces.stream().map(CardFace::getTypeLine).collect(Collectors.toList()));
    }

    public ImmutableList<CardEdition> getEditions() {
        return editions;
    }

    public Word<CardLayout> getLayout() {
        return layout;
    }

    public ColorSet getColors() {
        return colors;
    }

    public ColorSet getColorIdentity() {
        return colorIdentity;
    }

    public int getCmc() {
        return cmc;
    }

    public ImmutableList<CardFace> getFaces() {
        return faces;
    }

    public Optional<CardFace> getMainFace() {
        return layout.is(CardLayout.SPLIT) ? Optional.empty() : Optional.of(faces.get(0));
    }

    public Stream<CardFace> getBaseFaces() {
        return layout.is(CardLayout.SPLIT) ? faces.stream() : Stream.of(faces.get(0));
    }

    public CardLegality getCardLegality() {
        return legalities;
    }

    public boolean isExtra() {
        return getLayout().is(CardLayout.ART_SERIES)
                || editions.stream().anyMatch(v -> v.getExpansion().getType().is(ExpansionType.TOKEN));
    }

    public boolean isReserved() {
        return isReserved;
    }

    public boolean hasContentWarning() {
        return hasContentWarning;
    }

    /**
     * Return all names that have been printed on an edition of the card. This includes names localized in other
     * languages, and "flavor names" such as the Godzilla cards in Ikoria.
     * <p>
     * These names are not guaranteed to be unique for each card. For example, the cards Balance and Equilibrium were
     * mistakenly both localized in Spanish as "Equilibrio".
     *
     * @return a stream containing all names that have been printed on an edition of the card
     */
    public Stream<String> getAllNames() {
        return Stream.concat(Stream.of(name),
                        editions.stream()
                                .flatMap((CardEdition e) -> e.getFaces().stream())
                                .flatMap((CardEditionFace f) -> Stream.of(f.getPrintedName(), f.getFlavorName())
                                        .flatMap(Optional::stream)))
                .distinct();
    }

    @Override
    public int compareTo(Card that) {
        if (this == that || this.scryfallId.equals(that.scryfallId)) return 0;
        return this.editions.get(0).compareTo(that.editions.get(0));
    }

    @Override
    public String toString() {
        return getFullName();
    }

}
