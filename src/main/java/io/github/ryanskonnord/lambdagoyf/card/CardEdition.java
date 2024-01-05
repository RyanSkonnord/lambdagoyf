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

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import io.github.ryanskonnord.lambdagoyf.card.field.BorderColor;
import io.github.ryanskonnord.lambdagoyf.card.field.ExpansionType;
import io.github.ryanskonnord.lambdagoyf.card.field.Finish;
import io.github.ryanskonnord.lambdagoyf.card.field.Format;
import io.github.ryanskonnord.lambdagoyf.card.field.FrameEffect;
import io.github.ryanskonnord.lambdagoyf.card.field.FrameStyle;
import io.github.ryanskonnord.lambdagoyf.card.field.Language;
import io.github.ryanskonnord.lambdagoyf.card.field.Legality;
import io.github.ryanskonnord.lambdagoyf.card.field.PromoType;
import io.github.ryanskonnord.lambdagoyf.card.field.Rarity;
import io.github.ryanskonnord.lambdagoyf.card.field.SecurityStamp;
import io.github.ryanskonnord.lambdagoyf.scryfall.ScryfallCardEntry;
import io.github.ryanskonnord.lambdagoyf.scryfall.ScryfallCardFace;
import io.github.ryanskonnord.util.MapCollectors;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class CardEdition extends ScryfallEntity
        implements CardIdentity, CardVersion, Comparable<CardEdition> {

    private final Card parent;
    private final UUID scryfallId;
    private final ImmutableList<CardEditionFace> faces;
    private final CardIllustration illustration;

    private final Expansion expansion;
    private final Word<Language> language;
    private final Word<Rarity> rarity;
    private final CollectorNumber collectorNumber;
    private final LocalDate releaseDate;
    private final boolean isInBooster;
    private final CardLegality cardLegality;

    private final Word<BorderColor> borderColor;
    private final Word<FrameStyle> frameStyle;
    private final Word<SecurityStamp> securityStamp;
    private final WordSet<FrameEffect> frameEffects;
    private final WordSet<PromoType> promoTypes;
    private final boolean isFullArt;

    private final ImmutableSet<Finish> paperFinishes;
    private final MtgoCard mtgoNonfoil;
    private final MtgoCard mtgoFoil;
    private final ArenaCard arenaCard;

    private final ImmutableSet<UUID> relatedParts;

    CardEdition(CardFactory factory, Card parentCard, ScryfallCardEntry entry) {
        parent = Objects.requireNonNull(parentCard);
        scryfallId = entry.getId();
        faces = buildFaces(entry);
        illustration = CardIllustration.from(faces);

        expansion = factory.getExpansions().lookUpByProductCode(entry.getSet())
                .orElseThrow(() -> new Card.ScryfallParsingException("Product code not matched: " + entry.getSet()));
        language = Word.of(Language.class, entry.getLang());
        rarity = Word.of(Rarity.class, entry.getRarity());
        collectorNumber = CollectorNumber.parse(entry.getCollectorNumber());
        releaseDate = entry.getReleasedAt();
        isInBooster = entry.isBooster();
        cardLegality = buildCardLegality(factory.getLegalityFactory(), entry.getLegalities());

        borderColor = Word.of(BorderColor.class, entry.getBorderColor());
        frameStyle = Word.of(FrameStyle.class, entry.getFrame());
        securityStamp = entry.getSecurityStamp().map(s -> Word.of(SecurityStamp.class, s)).orElse(Word.of(SecurityStamp.NONE));
        frameEffects = WordSet.ofWords(FrameEffect.class, entry.getFrameEffects());
        promoTypes = WordSet.ofWords(PromoType.class, entry.getPromoTypes());
        isFullArt = entry.isFullArt();

        paperFinishes = factory.cacheFinishSet(buildPaperFinishes(entry));
        arenaCard = ArenaCard.create(this, entry.getArenaId(), factory.getArenaFixes()).orElse(null);

        MtgoCard[] mtgoCards = buildMtgoCards(entry, factory.getMtgoFixes());
        mtgoNonfoil = mtgoCards[0];
        mtgoFoil = mtgoCards[1];

        relatedParts = entry.getAllParts().stream().flatMap(Collection::stream)
                .map(part -> UUID.fromString(part.get("id")))
                .filter(id -> !scryfallId.equals(id))
                .collect(ImmutableSet.toImmutableSet());
    }

    private ImmutableList<CardEditionFace> buildFaces(ScryfallCardEntry entry) {
        ImmutableList<ScryfallCardFace> faceData = entry.getFaceStream().collect(ImmutableList.toImmutableList());
        return IntStream.range(0, faceData.size())
                .mapToObj((int faceIndex) -> new CardEditionFace(CardEdition.this, faceIndex, faceData.get(faceIndex)))
                .collect(ImmutableList.toImmutableList());
    }

    private CardLegality buildCardLegality(CardLegality.Factory factory, ImmutableMap<String, String> legalities) {
        return factory.create(legalities.entrySet().stream()
                .collect(MapCollectors.<Map.Entry<String, String>>collecting()
                        .withKey(e -> Word.of(Format.class, e.getKey()))
                        .withValue(e -> Legality.fromName(e.getValue()).orElseThrow())
                        .unique().toImmutableMap()));
    }

    private ImmutableSet<Finish> buildPaperFinishes(ScryfallCardEntry entry) {
        if (!entry.getGames().contains("paper")) return ImmutableSet.of();
        return entry.getFinishes().stream()
                .map(f -> Word.of(Finish.class, f).getEnum().orElseThrow(() -> new RuntimeException("Unrecognized finish: " + f)))
                .collect(Sets.toImmutableEnumSet());
    }

    private MtgoCard[] buildMtgoCards(ScryfallCardEntry entry, MtgoIdFix.Registry fixRegistry) {
        OptionalLong nonFoilId = entry.getMtgoId();
        OptionalLong foilId = entry.getMtgoFoilId();
        if (nonFoilId.isPresent() && foilId.isEmpty() && !expansion.getType().is(ExpansionType.VANGUARD)) {
            foilId = OptionalLong.of(nonFoilId.getAsLong() + 1);
        }

        Optional<MtgoIdFix> fix = fixRegistry.getFix(entry);
        if (fix.isPresent()) {
            OptionalLong nonFoilFix = fix.get().getNormalId();
            OptionalLong foilFix = fix.get().getFoilId();
            if ((nonFoilFix.isPresent() || foilFix.isPresent()) && (nonFoilFix.equals(nonFoilId) || foilFix.equals(foilId))) {
                String source = fix.get().getSource().orElse("?");
                if (nonFoilFix.equals(nonFoilId) && foilFix.equals(foilId)) {
                    System.err.println(String.format(
                            "Redundant fix from `%s` on %s: Scryfall IDs are accurate",
                            source, this));
                } else {
                    System.err.println(String.format(
                            "Redundant fix from `%s` on %s: (nonfoil: %s -> %s; foil %s -> %s)",
                            this, source,
                            asNullableString(nonFoilId), asNullableString(nonFoilFix),
                            asNullableString(foilId), asNullableString(foilFix)));
                }
            }
            nonFoilId = nonFoilFix;
            foilId = foilFix;
        }

        return new MtgoCard[]{
                nonFoilId.isPresent() ? new MtgoCard(nonFoilId.getAsLong(), this, Finish.NONFOIL) : null,
                foilId.isPresent() ? new MtgoCard(foilId.getAsLong(), this, Finish.FOIL) : null,
        };
    }

    private static String asNullableString(OptionalLong value) {
        return value.isPresent() ? Long.toString(value.getAsLong()) : Objects.toString(null);
    }

    @Override
    public Card getCard() {
        return parent;
    }

    @Override
    public UUID getScryfallId() {
        return scryfallId;
    }

    @Override
    public CardEdition getEdition() {
        return this;
    }

    public ImmutableList<CardEditionFace> getFaces() {
        return faces;
    }

    public CardIllustration getIllustration() {
        return illustration;
    }

    public Word<Language> getLanguage() {
        return language;
    }

    public Word<Rarity> getRarity() {
        return rarity;
    }

    public CollectorNumber getCollectorNumber() {
        return collectorNumber;
    }

    public Expansion getExpansion() {
        return expansion;
    }

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public boolean isInBooster() {
        return isInBooster;
    }

    public CardLegality getCardLegality() {
        return cardLegality;
    }

    public Word<BorderColor> getBorderColor() {
        return borderColor;
    }

    public Word<FrameStyle> getFrameStyle() {
        return frameStyle;
    }

    public WordSet<FrameEffect> getFrameEffects() {
        return frameEffects;
    }

    public WordSet<PromoType> getPromoTypes() {
        return promoTypes;
    }

    public boolean isFullArt() {
        return isFullArt;
    }

    public Optional<ArenaCard> getArenaCard() {
        return Optional.ofNullable(arenaCard);
    }

    public Stream<MtgoCard> getMtgoCards() {
        return Stream.of(mtgoNonfoil, mtgoFoil).filter(Objects::nonNull);
    }

    public Optional<MtgoCard> getMtgoCard(Finish finish) {
        return Optional.ofNullable(switch (finish) {
            case NONFOIL -> mtgoNonfoil;
            case FOIL -> mtgoFoil;
            default -> null;
        });
    }

    public Stream<PaperCard> getPaperCards() {
        return paperFinishes.stream().map(finish -> new PaperCard(CardEdition.this, finish));
    }

    public Optional<PaperCard> getPaperCard(Finish finish) {
        return paperFinishes.contains(finish) ? Optional.of(new PaperCard(this, finish)) : Optional.empty();
    }

    public Stream<String> getArtists() {
        return faces.stream().map(CardEditionFace::getArtist)
                .flatMap(Optional::stream)
                .distinct();
    }

    public boolean hasArtist(String artist) {
        return getArtists().anyMatch(artist::equals);
    }

    public ImmutableSet<UUID> getRelatedParts() {
        return relatedParts;
    }

    public boolean isReprint() {
        CardEdition firstEdition = parent.getEditions().get(0);
        return !firstEdition.equals(this) && firstEdition.getReleaseDate().isBefore(getReleaseDate());
    }

    public Iterable<CardEdition> getEarlierReleases() {
        return () -> new AbstractIterator<>() {
            private final Iterator<CardEdition> allEditions = parent.getEditions().iterator();

            @Override
            protected CardEdition computeNext() {
                if (allEditions.hasNext()) {
                    CardEdition next = allEditions.next();
                    if (next.getReleaseDate().isBefore(CardEdition.this.getReleaseDate())) {
                        return next;
                    }
                }
                return endOfData();
            }
        };
    }

    public boolean isGameCard() {
        return !expansion.getType().is(ExpansionType.MEMORABILIA) && !paperFinishes.isEmpty()
                || Stream.of(mtgoNonfoil, mtgoFoil, arenaCard).anyMatch(Objects::nonNull);
    }

    @Override
    public int compareTo(CardEdition that) {
        if (this == that || this.scryfallId.equals(that.scryfallId)) return 0;
        int cmp = this.releaseDate.compareTo(that.releaseDate);
        if (cmp != 0) return cmp;
        cmp = this.expansion.compareTo(that.expansion);
        if (cmp != 0) return cmp;
        cmp = this.collectorNumber.compareTo(that.collectorNumber);
        if (cmp != 0) return cmp;
        cmp = this.language.compareTo(that.language);
        if (cmp != 0) return cmp;
        if (this.isGameCard() || that.isGameCard()) {
            throw new RuntimeException("Cards not sufficiently distinguished");
        }
        return this.getCard().getFullName().compareTo(that.getCard().getFullName());
    }

    @Override
    public String toString() {
        return String.format("%s (%s #%s)", parent.getFullName(), getExpansion().getProductCode(), getCollectorNumber());
    }

}
