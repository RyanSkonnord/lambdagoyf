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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import io.github.ryanskonnord.lambdagoyf.card.field.Language;
import io.github.ryanskonnord.util.MapCollectors;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class LocalizedSpoiler {

    private final Spoiler parent;
    private final Language language;
    private final ImmutableMap<String, Card> byName;
    private final ImmutableSetMultimap<String, Card> collidingNames;

    private LocalizedSpoiler(Spoiler spoiler, Language language,
                             Map<String, Card> byName, Multimap<String, Card> collidingNames) {
        this.parent = Objects.requireNonNull(spoiler);
        this.language = Objects.requireNonNull(language);
        this.byName = byName.entrySet().stream()
                .sorted(Comparator.comparing((Map.Entry<String, Card> e) -> e.getValue())
                        .thenComparing(Map.Entry::getKey))
                .collect(MapCollectors.<String, Card>collectingEntries().unique().toImmutableMap());
        this.collidingNames = collidingNames.asMap().entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .collect(ImmutableSetMultimap.flatteningToImmutableSetMultimap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream().sorted()
                ));
    }

    public static Optional<LocalizedSpoiler> create(Spoiler spoiler, Language language) {
        Collection<Card> cards = spoiler.getCards();
        Map<String, Card> byName = Maps.newHashMapWithExpectedSize(cards.size() * 2);
        SetMultimap<String, Card> collidingNames = HashMultimap.create();

        for (Card card : cards) {
            for (CardEdition edition : card.getEditions()) {
                if (edition.getLanguage().is(language)) {
                    for (CardEditionFace face : edition.getFaces()) {
                        Optional<String> printedName = face.getPrintedName();
                        if (printedName.isPresent()) {
                            String name = Spoiler.normalize(printedName.get());

                            if (collidingNames.containsKey(name)) {
                                collidingNames.put(name, card);
                            } else {
                                Card previous = byName.put(name, card);
                                if (previous != null && !previous.equals(card)) {
                                    byName.remove(name);
                                    collidingNames.put(name, previous);
                                    collidingNames.put(name, card);
                                }
                            }
                        }
                    }
                }
            }
        }

        return Optional.of(new LocalizedSpoiler(spoiler, language, byName, collidingNames))
                .filter(LocalizedSpoiler::isNotEmpty);
    }

    private boolean isNotEmpty() {
        return !byName.isEmpty() && !collidingNames.isEmpty();
    }

    public Language getLanguage() {
        return language;
    }

    public Optional<Card> lookUpByUniqueLocalizedName(String name) {
        return Optional.ofNullable(byName.get(Spoiler.normalize(name)));
    }

    public ImmutableSet<Card> lookUpByLocalizedName(String name) {
        return lookUpByUniqueLocalizedName(name).map(ImmutableSet::of).orElseGet(() -> collidingNames.get(name));
    }

    public ImmutableCollection<Card> getCardsWithCollidingNames() {
        return collidingNames.values();
    }

    public Optional<Card> lookUpByName(String name) {
        Optional<Card> byUniqueLocalizedName = lookUpByUniqueLocalizedName(name);
        return byUniqueLocalizedName.isPresent() ? byUniqueLocalizedName : parent.lookUpByName(name);
    }

}
