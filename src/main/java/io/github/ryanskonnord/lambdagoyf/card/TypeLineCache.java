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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;
import io.github.ryanskonnord.lambdagoyf.card.field.CardSupertype;
import io.github.ryanskonnord.lambdagoyf.card.field.CardType;
import io.github.ryanskonnord.lambdagoyf.scryfall.ScryfallParser;
import io.github.ryanskonnord.util.MapCollectors;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.github.ryanskonnord.lambdagoyf.card.field.CardSupertype.LEGENDARY;
import static io.github.ryanskonnord.lambdagoyf.card.field.CardSupertype.TOKEN;
import static io.github.ryanskonnord.lambdagoyf.card.field.CardSupertype.WORLD;

final class TypeLineCache {

    private final Cache<TypeLine, TypeLine> cache;
    private final ImmutableMap<TypeLine, TypeLine> flyweights;

    public TypeLineCache() {
        cache = CacheBuilder.newBuilder().build();
        flyweights = getStoredFlyweights().collect(MapCollectors.<TypeLine>collectingIdentities().unique().toImmutableMap());
    }

    public TypeLine get(TypeLine value) {
        TypeLine flyweight = flyweights.get(value);
        if (flyweight != null) return flyweight;
        try {
            return cache.get(value, () -> value);
        } catch (ExecutionException e) {
            return value;
        }
    }


    private static TypeLine createFlyweight(Object... values) {
        List<Word<CardSupertype>> supertypes = new ArrayList<>();
        List<Word<CardType>> cardTypes = new ArrayList<>();
        List<String> subtypes = new ArrayList<>();
        for (Object value : values) {
            if (value instanceof CardSupertype) {
                supertypes.add(Word.of((CardSupertype) value));
            } else if (value instanceof CardType) {
                cardTypes.add(Word.of((CardType) value));
            } else if (value instanceof String) {
                subtypes.add((String) value);
            } else {
                throw new IllegalArgumentException();
            }
        }
        return new TypeLine(WordSet.copyWords(supertypes), WordSet.copyWords(cardTypes), subtypes);
    }


    private static class Maintenance {
        private static String formatAsFlyweight(Multiset.Entry<TypeLine> entry) {
            TypeLine typeLine = entry.getElement();
            Stream<String> beforeDash = Stream.of(typeLine.getSupertypes(), typeLine.getCardTypes())
                    .flatMap(part -> part.asList().stream().map(word -> word.getEnum()
                            .orElseThrow(() -> new IllegalArgumentException("Expected only words with enums"))
                            .name()));
            Stream<String> subtypes = typeLine.getSubtypes().stream().map(subtype -> '"' + subtype + '"');
            String args = Stream.concat(beforeDash, subtypes).collect(Collectors.joining(", "));
            return String.format("    /* %4d */ {%s},\n", entry.getCount(), args);
        }

        private static Stream<Multiset.Entry<TypeLine>> getAllTypeLines(Collection<Card> cards) {
            return cards.stream()
                    .flatMap(card -> card.getFaces().stream())
                    .map(CardFace::getTypeLine)
                    .collect(ImmutableMultiset.toImmutableMultiset())
                    .entrySet().stream();
        }

        private static String listFlyweights(Collection<Card> cards, FlyweightThreshold threshold) {
            String preamble = String.format("    // Generated on %s from %d cards: %s\n",
                    LocalDate.now(), cards.size(), threshold.describe());
            Stream<Multiset.Entry<TypeLine>> typeLines = getAllTypeLines(cards);
            typeLines = threshold.apply(typeLines);
            String flyweightList = typeLines
                    .map(Maintenance::formatAsFlyweight)
                    .collect(Collectors.joining());
            return preamble + flyweightList;
        }

        private interface FlyweightThreshold {
            Stream<Multiset.Entry<TypeLine>> apply(Stream<Multiset.Entry<TypeLine>> stream);

            String describe();
        }

        private static final class WithMinimumFrequency implements FlyweightThreshold {
            private final int minimumFrequency;

            private WithMinimumFrequency(int minimumFrequency) {
                this.minimumFrequency = minimumFrequency;
            }

            @Override
            public Stream<Multiset.Entry<TypeLine>> apply(Stream<Multiset.Entry<TypeLine>> stream) {
                return stream.filter(e -> e.getCount() >= minimumFrequency)
                        .sorted(Comparator.comparing(Multiset.Entry::getElement));
            }

            @Override
            public String describe() {
                return String.format("appearing on at least %d cards", minimumFrequency);
            }
        }

        private static final class LimitNumberOfFlyweights implements FlyweightThreshold {
            private final int maximumCount;

            private LimitNumberOfFlyweights(int maximumCount) {
                this.maximumCount = maximumCount;
            }

            @Override
            public Stream<Multiset.Entry<TypeLine>> apply(Stream<Multiset.Entry<TypeLine>> stream) {
                return stream
                        .sorted(Comparator.<Multiset.Entry<TypeLine>, Integer>comparing(Multiset.Entry::getCount).reversed()
                                .thenComparing(Multiset.Entry::getElement))
                        .limit(maximumCount);
            }

            @Override
            public String describe() {
                return String.format("%d most common type lines", maximumCount);
            }
        }
    }

    private static Stream<TypeLine> getStoredFlyweights() {
        Object[][] values = {
                // Generated on 2021-02-20 from 22530 cards with threshold=20
                /* 1366 */ {CardType.ENCHANTMENT},
                /*  949 */ {CardType.ENCHANTMENT, "Aura"},
                /*   29 */ {CardType.ENCHANTMENT, "Aura", "Curse"},
                /*   44 */ {CardType.ENCHANTMENT, "Saga"},
                /* 1054 */ {CardType.ARTIFACT},
                /*   45 */ {CardType.ARTIFACT, "Contraption"},
                /*  268 */ {CardType.ARTIFACT, "Equipment"},
                /*   30 */ {CardType.ARTIFACT, "Vehicle"},
                /*  136 */ {CardType.ARTIFACT, CardType.CREATURE, "Construct"},
                /*  113 */ {CardType.ARTIFACT, CardType.CREATURE, "Golem"},
                /*   34 */ {CardType.ARTIFACT, CardType.CREATURE, "Myr"},
                /*   33 */ {CardType.ARTIFACT, CardType.CREATURE, "Scarecrow"},
                /*   25 */ {CardType.ARTIFACT, CardType.CREATURE, "Wall"},
                /*  601 */ {CardType.LAND},
                /*   20 */ {CardType.LAND, "Desert"},
                /*  119 */ {CardType.CREATURE, "Angel"},
                /*   40 */ {CardType.CREATURE, "Avatar"},
                /*  270 */ {CardType.CREATURE, "Beast"},
                /*  123 */ {CardType.CREATURE, "Bird"},
                /*   42 */ {CardType.CREATURE, "Bird", "Soldier"},
                /*   24 */ {CardType.CREATURE, "Boar"},
                /*   82 */ {CardType.CREATURE, "Cat"},
                /*   68 */ {CardType.CREATURE, "Demon"},
                /*   30 */ {CardType.CREATURE, "Devil"},
                /*   83 */ {CardType.CREATURE, "Dinosaur"},
                /*   34 */ {CardType.CREATURE, "Djinn"},
                /*   40 */ {CardType.CREATURE, "Dog"},
                /*  132 */ {CardType.CREATURE, "Dragon"},
                /*   79 */ {CardType.CREATURE, "Drake"},
                /*   22 */ {CardType.CREATURE, "Dryad"},
                /*   22 */ {CardType.CREATURE, "Dwarf"},
                /*   39 */ {CardType.CREATURE, "Eldrazi"},
                /*   52 */ {CardType.CREATURE, "Eldrazi", "Drone"},
                /*  289 */ {CardType.CREATURE, "Elemental"},
                /*   23 */ {CardType.CREATURE, "Elemental", "Shaman"},
                /*   32 */ {CardType.CREATURE, "Elephant"},
                /*   47 */ {CardType.CREATURE, "Elf"},
                /*   20 */ {CardType.CREATURE, "Elf", "Archer"},
                /*   68 */ {CardType.CREATURE, "Elf", "Druid"},
                /*   26 */ {CardType.CREATURE, "Elf", "Scout"},
                /*   50 */ {CardType.CREATURE, "Elf", "Shaman"},
                /*   61 */ {CardType.CREATURE, "Elf", "Warrior"},
                /*   31 */ {CardType.CREATURE, "Faerie"},
                /*   24 */ {CardType.CREATURE, "Faerie", "Rogue"},
                /*   20 */ {CardType.CREATURE, "Fish"},
                /*   28 */ {CardType.CREATURE, "Fungus"},
                /*   66 */ {CardType.CREATURE, "Giant"},
                /*   32 */ {CardType.CREATURE, "Giant", "Warrior"},
                /*   94 */ {CardType.CREATURE, "Goblin"},
                /*   35 */ {CardType.CREATURE, "Goblin", "Rogue"},
                /*   47 */ {CardType.CREATURE, "Goblin", "Shaman"},
                /*   88 */ {CardType.CREATURE, "Goblin", "Warrior"},
                /*   46 */ {CardType.CREATURE, "Griffin"},
                /*   97 */ {CardType.CREATURE, "Horror"},
                /*   54 */ {CardType.CREATURE, "Human"},
                /*   24 */ {CardType.CREATURE, "Human", "Advisor"},
                /*   50 */ {CardType.CREATURE, "Human", "Artificer"},
                /*   21 */ {CardType.CREATURE, "Human", "Assassin"},
                /*   28 */ {CardType.CREATURE, "Human", "Berserker"},
                /*  225 */ {CardType.CREATURE, "Human", "Cleric"},
                /*   60 */ {CardType.CREATURE, "Human", "Druid"},
                /*  175 */ {CardType.CREATURE, "Human", "Knight"},
                /*   42 */ {CardType.CREATURE, "Human", "Monk"},
                /*   62 */ {CardType.CREATURE, "Human", "Pirate"},
                /*   20 */ {CardType.CREATURE, "Human", "Rebel"},
                /*   97 */ {CardType.CREATURE, "Human", "Rogue"},
                /*   33 */ {CardType.CREATURE, "Human", "Scout"},
                /*   83 */ {CardType.CREATURE, "Human", "Shaman"},
                /*  322 */ {CardType.CREATURE, "Human", "Soldier"},
                /*   30 */ {CardType.CREATURE, "Human", "Spellshaper"},
                /*  164 */ {CardType.CREATURE, "Human", "Warrior"},
                /*  343 */ {CardType.CREATURE, "Human", "Wizard"},
                /*   31 */ {CardType.CREATURE, "Hydra"},
                /*   55 */ {CardType.CREATURE, "Illusion"},
                /*   34 */ {CardType.CREATURE, "Imp"},
                /*  119 */ {CardType.CREATURE, "Insect"},
                /*   41 */ {CardType.CREATURE, "Kavu"},
                /*   24 */ {CardType.CREATURE, "Kithkin", "Soldier"},
                /*   37 */ {CardType.CREATURE, "Lizard"},
                /*   28 */ {CardType.CREATURE, "Merfolk"},
                /*   26 */ {CardType.CREATURE, "Merfolk", "Rogue"},
                /*   20 */ {CardType.CREATURE, "Merfolk", "Soldier"},
                /*   81 */ {CardType.CREATURE, "Merfolk", "Wizard"},
                /*   24 */ {CardType.CREATURE, "Minotaur", "Warrior"},
                /*   22 */ {CardType.CREATURE, "Ogre"},
                /*   35 */ {CardType.CREATURE, "Ogre", "Warrior"},
                /*   28 */ {CardType.CREATURE, "Ooze"},
                /*   21 */ {CardType.CREATURE, "Orc", "Warrior"},
                /*   26 */ {CardType.CREATURE, "Phoenix"},
                /*   34 */ {CardType.CREATURE, "Rat"},
                /*   30 */ {CardType.CREATURE, "Serpent"},
                /*   22 */ {CardType.CREATURE, "Shade"},
                /*   80 */ {CardType.CREATURE, "Shapeshifter"},
                /*   98 */ {CardType.CREATURE, "Sliver"},
                /*   43 */ {CardType.CREATURE, "Snake"},
                /*   22 */ {CardType.CREATURE, "Specter"},
                /*   40 */ {CardType.CREATURE, "Sphinx"},
                /*   51 */ {CardType.CREATURE, "Spider"},
                /*  274 */ {CardType.CREATURE, "Spirit"},
                /*   22 */ {CardType.CREATURE, "Thrull"},
                /*   32 */ {CardType.CREATURE, "Treefolk"},
                /*   20 */ {CardType.CREATURE, "Troll"},
                /*   74 */ {CardType.CREATURE, "Vampire"},
                /*   20 */ {CardType.CREATURE, "Vampire", "Soldier"},
                /*   23 */ {CardType.CREATURE, "Vedalken", "Wizard"},
                /*   83 */ {CardType.CREATURE, "Wall"},
                /*   33 */ {CardType.CREATURE, "Werewolf"},
                /*   42 */ {CardType.CREATURE, "Wolf"},
                /*   80 */ {CardType.CREATURE, "Wurm"},
                /*  179 */ {CardType.CREATURE, "Zombie"},
                /*   31 */ {CardType.CREATURE, "Zombie", "Wizard"},
                /* 2374 */ {CardType.SORCERY},
                /*   28 */ {CardType.SORCERY, "Arcane"},
                /* 2542 */ {CardType.INSTANT},
                /*   66 */ {CardType.INSTANT, "Arcane"},
                /*   20 */ {CardType.INSTANT, "Trap"},
                /*   25 */ {CardType.CONSPIRACY},
                /*  107 */ {CardType.VANGUARD},
                /*   58 */ {CardType.SCHEME},
                /*  423 */ {CardType.CARD},
                /*   26 */ {TOKEN, CardType.CREATURE, "Elemental"},
                /*   28 */ {LEGENDARY, CardType.ENCHANTMENT},
                /*   22 */ {LEGENDARY, CardType.ENCHANTMENT, CardType.CREATURE, "God"},
                /*   57 */ {LEGENDARY, CardType.ARTIFACT},
                /*   21 */ {LEGENDARY, CardType.ARTIFACT, "Equipment"},
                /*   54 */ {LEGENDARY, CardType.LAND},
                /*   33 */ {LEGENDARY, CardType.CREATURE, "Angel"},
                /*   27 */ {LEGENDARY, CardType.CREATURE, "Dragon"},
                /*   21 */ {LEGENDARY, CardType.CREATURE, "Human", "Advisor"},
                /*   26 */ {LEGENDARY, CardType.CREATURE, "Human", "Cleric"},
                /*   23 */ {LEGENDARY, CardType.CREATURE, "Human", "Knight"},
                /*   45 */ {LEGENDARY, CardType.CREATURE, "Human", "Soldier"},
                /*   40 */ {LEGENDARY, CardType.CREATURE, "Human", "Warrior"},
                /*   64 */ {LEGENDARY, CardType.CREATURE, "Human", "Wizard"},
                /*   46 */ {LEGENDARY, CardType.CREATURE, "Spirit"},
                /*   28 */ {WORLD, CardType.ENCHANTMENT},
        };
        return Arrays.stream(values).map(TypeLineCache::createFlyweight);
    }


    public static void main(String[] args) throws Exception {
        Spoiler spoiler = ScryfallParser.createSpoiler();

        List<Multiset.Entry<String>> manaCosts = spoiler.getCards().stream().flatMap(c -> c.getFaces().stream().flatMap(f -> f.getManaCost().stream()))
                .collect(ImmutableMultiset.toImmutableMultiset())
                .entrySet().stream()
                .sorted(Comparator.comparing((Function<Multiset.Entry<String>, Integer>) Multiset.Entry::getCount).reversed())
                .collect(Collectors.toList());

        System.out.println(Maintenance.listFlyweights(spoiler.getCards(), new Maintenance.LimitNumberOfFlyweights((int) (128 * 0.75))));
    }

}
