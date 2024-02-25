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

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.MoreCollectors;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import io.github.ryanskonnord.lambdagoyf.card.field.ExpansionType;
import io.github.ryanskonnord.lambdagoyf.card.field.Language;
import io.github.ryanskonnord.util.MapCollectors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.github.ryanskonnord.lambdagoyf.card.CardNames.normalize;

public final class Spoiler {

    private static <E extends ScryfallEntity> ImmutableMap<UUID, E> checkScryfallIdUniqueness(Stream<? extends E> elements) {
        ListMultimap<UUID, E> groups = elements.collect(MapCollectors.<E>collecting()
                .indexing(ScryfallEntity::getScryfallId)
                .grouping().toImmutableListMultimap());
        Set<Map.Entry<UUID, List<E>>> entries = Multimaps.asMap(groups).entrySet();
        ImmutableMap.Builder<UUID, E> builder = ImmutableMap.builderWithExpectedSize(entries.size());
        for (Map.Entry<UUID, List<E>> entry : entries) {
            List<E> group = entry.getValue();
            if (group.size() > 1) {
                System.err.println(String.format(
                        "Scryfall ID collision on %s: %s",
                        entry.getKey(), group));
            }
            E element = group.get(0);
            builder.put(element.getScryfallId(), element);
        }
        return builder.build();
    }

    private final ImmutableMap<UUID, Card> cards;
    private final ImmutableMap<UUID, CardEdition> editions;
    private final ImmutableMap<String, Card> byName;
    private final ImmutableBiMap<Long, MtgoCard> byMtgoId;
    private final ImmutableMap<Language, LocalizedSpoiler> localizedSpoilers;
    private final ImmutableSetMultimap<Expansion, CardEdition> byExpansion;
    private final ImmutableMap<String, Expansion> expansionsByName;

    Spoiler(Collection<Card> cards) {
        this.cards = checkScryfallIdUniqueness(cards.stream());

        editions = checkScryfallIdUniqueness(this.cards.values().stream()
                .flatMap(c -> c.getEditions().stream()));

        byName = buildNameDictionary(this.cards.values());

        byMtgoId = buildMtgoIdMap(this.cards.values());

        localizedSpoilers = EnumSet.allOf(Language.class).parallelStream()
                .map(language -> LocalizedSpoiler.create(Spoiler.this, language))
                .flatMap(Optional::stream)
                .collect(MapCollectors.<LocalizedSpoiler>collecting()
                        .indexing(LocalizedSpoiler::getLanguage)
                        .unique().toImmutableMap());

        byExpansion = this.cards.values().stream()
                .flatMap((Card c) -> c.getEditions().stream())
                .sorted()
                .collect(MapCollectors.<CardEdition>collecting()
                        .indexing(CardEdition::getExpansion)
                        .grouping().toImmutableSetMultimap());

        expansionsByName = buildExpansionNameMap(byExpansion.keySet());
    }

    private static ImmutableMap<String, Card> buildNameDictionary(Collection<Card> cards) {
        Multimap<String, Card> groupedByCardName = MultimapBuilder.hashKeys(cards.size()).arrayListValues(1).build();
        for (Card card : cards) {
            if (!card.isExtra()) {
                groupedByCardName.put(card.getFullName(), card);
            }
        }
        Iterable<Card> uniquelyNamedCards = () -> groupedByCardName.asMap().values().stream()
                .filter(group -> group.size() == 1)
                .map(Iterables::getOnlyElement)
                .iterator();

        Map<String, Card> names = Maps.newHashMapWithExpectedSize(9 * cards.size());
        for (Card card : uniquelyNamedCards) {
            String name = normalize(card.getFullName());
            names.put(name, card);
            for (CardFace face : card.getFaces()) {
                String faceName = normalize(face.getName());
                if (!faceName.equals(name)) {
                    names.put(faceName, card);
                }
            }
        }

        SetMultimap<String, Card> byPrintedName = MultimapBuilder.hashKeys(8 * cards.size()).hashSetValues(2).build();
        for (Card card : uniquelyNamedCards) {
            Iterable<String> printedNames = () -> card.getAllNames().map(CardNames::normalize).iterator();
            for (String printedName : printedNames) {
                if (!names.containsKey(printedName)) {
                    byPrintedName.put(printedName, card);
                }
            }
        }
        for (Map.Entry<String, Collection<Card>> entry : byPrintedName.asMap().entrySet()) {
            String name = entry.getKey();
            if (entry.getValue().size() == 1) {
                Card previous = names.put(name, Iterables.getOnlyElement(entry.getValue()));
                if (previous != null) throw new AssertionError();
            }
        }

        return names.entrySet().parallelStream()
                .sorted(Comparator.comparing((Map.Entry<String, Card> e) -> e.getValue())
                        .thenComparing(Map.Entry::getKey))
                .collect(MapCollectors.<String, Card>collectingEntries().unique().toImmutableMap());
    }

    private static ImmutableBiMap<Long, MtgoCard> buildMtgoIdMap(Collection<Card> cards) {
        Map<Long, MtgoCard> map = new LinkedHashMap<>();
        cards.stream()
                .flatMap((Card c) -> c.getEditions().stream())
                .flatMap(CardEdition::getMtgoCards).sorted()
                .forEachOrdered(mtgoCard -> {
                    long id = mtgoCard.getMtgoId();
                    MtgoCard previous = map.get(id);
                    if (previous == null) {
                        map.put(id, mtgoCard);
                    } else {
                        System.err.printf("MTGO ID collision on: %s; %s%n", previous, mtgoCard);
                    }
                });
        return ImmutableBiMap.copyOf(map);
    }


    private static ImmutableMap<String, Expansion> buildExpansionNameMap(Set<Expansion> expansions) {
        Map<String, Expansion> byName = new LinkedHashMap<>((int) (expansions.size() * 2.75));
        List<Expansion> orderedExpansions = expansions.stream().sorted().collect(ImmutableList.toImmutableList());

        for (Expansion expansion : orderedExpansions) {
            Collection<String> names = new ArrayList<>(3);
            names.add(expansion.getName());
            names.add(expansion.getProductCode());

            for (String name : names) {
                Expansion previous = byName.put(normalize(name), expansion);
                if (previous != null) {
                    throw new IllegalArgumentException(String.format(
                            "Key collision on %s between: %s; %s", name, previous, expansion));
                }
            }
        }

        SetMultimap<String, Expansion> groupedByToken = orderedExpansions.stream()
                .filter(expansion -> !expansion.getType().is(ExpansionType.PROMO) && !expansion.getType().is(ExpansionType.TOKEN))
                .flatMap(expansion -> expansion.getColonNameTokens().stream().map(
                        token -> Maps.immutableEntry(token, expansion)))
                .collect(MapCollectors.<String, Expansion>collectingEntries()
                        .grouping().toImmutableSetMultimap());
        for (Map.Entry<String, Set<Expansion>> entry : Multimaps.asMap(groupedByToken).entrySet()) {
            String token = normalize(entry.getKey());
            Set<Expansion> expansionsWithToken = entry.getValue();
            if (expansionsWithToken.size() == 1) {
                byName.putIfAbsent(token, Iterables.getOnlyElement(expansionsWithToken));
            }
        }

        for (Expansion expansion : orderedExpansions) {
            expansion.getMtgoCode().ifPresent(mtgoCode ->
                    byName.putIfAbsent(normalize(mtgoCode), expansion));
        }

        return ImmutableMap.copyOf(byName);
    }


    public ImmutableCollection<Card> getCards() {
        return cards.values();
    }

    public Optional<Card> lookUpCardByUuid(UUID uuid) {
        return Optional.ofNullable(cards.get(uuid));
    }

    public Optional<CardEdition> lookUpEditionByUuid(UUID uuid) {
        return Optional.ofNullable(editions.get(uuid));
    }

    public Optional<Card> lookUpByName(String name) {
        return Optional.ofNullable(byName.get(normalize(name)));
    }

    public Optional<MtgoCard> lookUpByMtgoId(long mtgoId) {
        return Optional.ofNullable(byMtgoId.get(mtgoId));
    }

    public Optional<ArenaCard> lookUpByArenaDeckEntry(ArenaDeckEntry entry) {
        return lookUpByName(entry.getCardName()).flatMap((Card card) ->
                card.getEditions().stream()
                        .map(CardEdition::getArenaCard)
                        .flatMap(Optional::stream)
                        .filter(arenaCard -> arenaCard.getDeckEntry().equals(entry))
                        .collect(MoreCollectors.toOptional()));
    }

    public ImmutableSet<CardEdition> getAllFromExpansion(Expansion expansion) {
        return byExpansion.get(expansion);
    }

    public Optional<CardEdition> getByCollectorNumber(Expansion expansion, CollectorNumber number) {
        return getAllFromExpansion(expansion).stream()
                .filter(e -> e.getCollectorNumber().equals(number))
                .collect(MoreCollectors.toOptional());
    }

    public ImmutableSet<Expansion> getExpansions() {
        return byExpansion.keySet();
    }

    public Optional<Expansion> getExpansion(String name) {
        return Optional.ofNullable(expansionsByName.get(normalize(name)));
    }


    @Override
    public boolean equals(Object o) {
        return this == o || !(o == null || getClass() != o.getClass())
                && cards.keySet().equals(((Spoiler) o).cards.keySet());
    }

    @Override
    public int hashCode() {
        return cards.keySet().hashCode();
    }


    private <W extends Word> List<String> gatherWordsByFirstApperance(Function<CardEdition, Stream<W>> extractor) {
        Map<W, CardEdition> firstApperances = CardVersionExtractor.getCardEditions().getAll(this)
                .flatMap(c -> extractor.apply(c).map(w -> Maps.immutableEntry(w, c)))
                .collect(MapCollectors.<W, CardEdition>collectingEntries()
                        .grouping()
                        .into(Collectors.collectingAndThen(
                                Collectors.minBy(Comparator.<CardEdition>naturalOrder()),
                                Optional::orElseThrow))
                        .toImmutableMap());
        return firstApperances.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .map(Word::getKey)
                .collect(Collectors.toList());
    }

}
