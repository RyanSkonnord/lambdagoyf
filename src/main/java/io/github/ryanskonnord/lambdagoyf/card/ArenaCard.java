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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.MoreCollectors;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import io.github.ryanskonnord.lambdagoyf.card.field.BorderColor;
import io.github.ryanskonnord.lambdagoyf.card.field.CardLayout;
import io.github.ryanskonnord.lambdagoyf.card.field.ExpansionType;
import io.github.ryanskonnord.lambdagoyf.card.field.FrameEffect;
import io.github.ryanskonnord.lambdagoyf.card.field.Language;
import io.github.ryanskonnord.lambdagoyf.card.field.PromoType;
import io.github.ryanskonnord.util.MapCollectors;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class ArenaCard implements CardVersion, Comparable<ArenaCard> {

    private static final ImmutableMap<String, String> EXPANSION_FIXES = ImmutableMap.<String, String>builder()
            .put("DOM", "DAR")
            .put("CON", "CONF")
            .put("YMID", "Y22")
            .put("YVOW", "Y22")
            .put("YNEO", "Y22")
            .put("YSNC", "Y22")
            .put("YDMU", "Y23")
            .put("YBRO", "Y23")
            .put("YONE", "Y23")
            .put("YMOM", "Y23")
            .build();
    private static final Pattern STANDARD_ALCHEMY_SET_CODE = Pattern.compile("Y(\\w{3})"); // TODO: Use me

    private static final ImmutableSetMultimap<String, String> REVERSE_ARENA_FIXES =
            EXPANSION_FIXES.entrySet().stream()
                    .collect(MapCollectors.<Map.Entry<String, String>>collecting()
                            .withKey(Map.Entry::getValue)
                            .withValue(Map.Entry::getKey)
                            .grouping().toImmutableSetMultimap());

    private static String toArenaCode(String expansionCode) {
        return EXPANSION_FIXES.getOrDefault(expansionCode, expansionCode);
    }

    private static final LocalDate ARENA_CARD_THRESHOLD = LocalDate.parse("2017-09-29");

    static Optional<ArenaCard> create(CardEdition edition, OptionalLong arenaId, ArenaIdFix.Registry fixRegistry) {
        Optional<ArenaIdFix> fix = fixRegistry.getFix(edition);
        if (fix.isPresent()) {
            return fix.get().setUpArenaCard(edition, arenaId);
        }

        boolean isUnfinityLand = edition.getExpansion().isNamed("Unfinity") && Range.closed(235, 244).contains(edition.getCollectorNumber().getNumber());
        boolean isStandardArenaCard = isStandardArenaCard(edition, arenaId);
        return isStandardArenaCard | isUnfinityLand
                ? Optional.of(new ArenaCard(edition, arenaId, null))
                : Optional.empty();
    }

    private static final ImmutableSet<FrameEffect> EXCLUDED_FRAME_EFFECTS = Sets.immutableEnumSet(
            FrameEffect.INVERTED, FrameEffect.SHOWCASE, FrameEffect.EXTENDED_ART);

    private static final ImmutableSet<String> BASE_EDITION_EXEMPTION_EXPANSIONS = ImmutableSet.of(
            "Strixhaven Mystical Archive", "Multiverse Legends", "Wilds of Eldraine: Enchanting Tales");

    private static boolean isBaseEdition(CardEdition edition) {
        boolean isExcludedFrameEffect = EXCLUDED_FRAME_EFFECTS.stream().anyMatch(edition.getFrameEffects()::contains);
        boolean isBorderless = edition.getBorderColor().is(BorderColor.BORDERLESS);
        boolean isSpecialExpansion = BASE_EDITION_EXEMPTION_EXPANSIONS.stream()
                .anyMatch(name -> edition.getExpansion().isNamed(name));
        return isSpecialExpansion | !isExcludedFrameEffect & !isBorderless;
    }

    private static boolean isStandardArenaCard(CardEdition edition, OptionalLong arenaId) {
        Expansion expansion = edition.getExpansion();
        Word<ExpansionType> expansionType = expansion.getType();
        boolean isStandardEdition = ExpansionType.isStandardRelease(expansionType) || expansionType.is(ExpansionType.ALCHEMY)
                || expansionType.is(ExpansionType.MASTERPIECE) && !expansion.isNamed("ZNE");
        boolean isDuringArena = expansion.getReleaseDate().compareTo(ARENA_CARD_THRESHOLD) >= 0;
        boolean isBaseEdition = isBaseEdition(edition);
        boolean isBundleEdition = edition.getPromoTypes().contains(PromoType.BUNDLE);

        boolean check = (isStandardEdition & isDuringArena & isBaseEdition & !isBundleEdition) | (!isStandardEdition & arenaId.isPresent());
        return check && isArenaVersionLanguage(edition);
    }

    private static boolean isArenaVersionLanguage(CardEdition edition) {
        return edition.getLanguage().isOneOf(Language.ENGLISH, Language.PHYREXIAN);
    }


    private final CardEdition parent;
    private final OptionalLong arenaId;
    private final Optional<ArenaIdFix.OverrideNumber> idFix;

    ArenaCard(CardEdition parent, OptionalLong arenaId, ArenaIdFix.OverrideNumber idFix) {
        this.parent = Objects.requireNonNull(parent);
        this.arenaId = Objects.requireNonNull(arenaId);
        this.idFix = Optional.ofNullable(idFix);
    }

    @Override
    public CardEdition getEdition() {
        return parent;
    }

    public OptionalLong getArenaId() {
        return arenaId;
    }

    private transient ArenaDeckEntry deckEntry;

    private String getCardName() {
        Card card = parent.getCard();
        if (card.getLayout().is(CardLayout.SPLIT)) {
            List<CardFace> faces = card.getFaces();
            boolean isAftermath = faces.get(1).getOracleTextParagraphs().stream()
                    .anyMatch(p -> p.startsWith("Aftermath"));
            if (isAftermath) {
                return faces.get(0) + " /// " + faces.get(1);
            }
        }
        return card.getMainName();
    }

    public ArenaDeckEntry getDeckEntry() {
        if (deckEntry != null) return deckEntry;

        String expansionCode;
        int collectorNumber;
        if (idFix.isPresent()) {
            expansionCode = idFix.get().getExpansionCode();
            collectorNumber = idFix.get().getCollectorNumber();
        } else {
            expansionCode = parent.getExpansion().getProductCode();
            collectorNumber = parent.getCollectorNumber().getNumber();
        }

        expansionCode = toArenaCode(expansionCode);
        return deckEntry = new ArenaDeckEntry(getCardName(), expansionCode, collectorNumber);
    }

    public Optional<ArenaCard> fromDeckEntry(Spoiler spoiler, ArenaDeckEntry entry) {
        String expansionCode = entry.getExpansionCode();
        Set<String> fixedCodes = REVERSE_ARENA_FIXES.get(expansionCode);
        Predicate<Expansion> expansionPredicate = fixedCodes.isEmpty()
                ? expansion -> expansion.isNamed(expansionCode)
                : expansion -> expansion.isNamed(expansionCode) || fixedCodes.stream().anyMatch(expansion::isNamed);
        return spoiler.lookUpByName(entry.getCardName())
                .flatMap(card -> card.getEditions().stream()
                        .filter(edition -> entry.getCollectorNumber() == edition.getCollectorNumber().getNumber()
                                && expansionPredicate.test(edition.getExpansion()))
                        .flatMap(edition -> edition.getArenaCard().stream())
                        .collect(MoreCollectors.toOptional()));
    }

    @Override
    public int compareTo(ArenaCard o) {
        return getEdition().compareTo(o.getEdition());
    }

    @Override
    public String toString() {
        ArenaDeckEntry deckEntry = getDeckEntry();
        return String.format("%s (%s %d; ID=%s)",
                getCardName(), deckEntry.getExpansionCode(), deckEntry.getCollectorNumber(),
                (arenaId.isPresent() ? Long.toString(arenaId.getAsLong()) : "null"));
    }

    @Override
    public final boolean equals(Object o) {
        return this == o || o instanceof ArenaCard && parent.equals(((ArenaCard) o).parent);
    }

    @Override
    public final int hashCode() {
        return parent.hashCode();
    }

}
