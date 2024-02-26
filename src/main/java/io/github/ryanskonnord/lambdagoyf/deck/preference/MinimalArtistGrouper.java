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

package io.github.ryanskonnord.lambdagoyf.deck.preference;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.MoreCollectors;
import com.google.common.collect.Multimaps;
import io.github.ryanskonnord.lambdagoyf.card.Card;
import io.github.ryanskonnord.lambdagoyf.card.CardEdition;
import io.github.ryanskonnord.lambdagoyf.card.CardVersion;
import io.github.ryanskonnord.lambdagoyf.card.CardVersionExtractor;
import io.github.ryanskonnord.lambdagoyf.card.Color;
import io.github.ryanskonnord.lambdagoyf.card.Expansion;
import io.github.ryanskonnord.lambdagoyf.card.Spoiler;
import io.github.ryanskonnord.lambdagoyf.card.field.CardSupertype;
import io.github.ryanskonnord.lambdagoyf.deck.Deck;
import io.github.ryanskonnord.lambdagoyf.deck.DeckRandomChoice;
import io.github.ryanskonnord.util.MapCollectors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.github.ryanskonnord.lambdagoyf.deck.preference.BasicLandPreferenceSequence.infiniteAvailability;

/**
 * Modifies decks to use basic land versions (from a specified group) all from the same artist.
 * Typically, such a group will have various artists illustrating larger and smaller sets of land
 * versions. Where possible, use a set of matching lands from the artist who did the smallest
 * available set of lands. Ties are broken at random.
 * <p>
 * The motive is to promote visual variety among the user's decks, by giving the artists who
 * illustrated fewer lands more "exposure" when a deck needs only those lands.
 *
 * @param <C>
 */
public class MinimalArtistGrouper<C extends CardVersion> {
    private final Spoiler spoiler;
    private final CardVersionExtractor<C> extractor;
    private final Scope scope;
    private final ImmutableList<Predicate<? super CardEdition>> predicates;
    private final boolean defaultToAnyArtist;

    private MinimalArtistGrouper(Builder<C> builder) {
        this.spoiler = builder.spoiler;
        this.extractor = builder.extractor;
        this.scope = builder.scope;
        this.predicates = ImmutableList.copyOf(builder.predicates);
        this.defaultToAnyArtist = builder.defaultToAnyArtist;
    }

    public static final class Builder<C extends CardVersion> {
        private final Spoiler spoiler;
        private final CardVersionExtractor<C> extractor;
        private Scope scope = Scope.NORMAL_BASIC_LANDS;
        private List<Predicate<? super CardEdition>> predicates = new ArrayList<>();
        private boolean defaultToAnyArtist = false;

        public Builder(Spoiler spoiler, CardVersionExtractor<C> extractor) {
            this.spoiler = Objects.requireNonNull(spoiler);
            this.extractor = Objects.requireNonNull(extractor);
        }

        public Builder<C> addPredicate(Predicate<? super CardEdition> predicate) {
            this.predicates.add(predicate);
            return this;
        }

        public Builder<C> setScope(Scope scope) {
            this.scope = Objects.requireNonNull(scope);
            return this;
        }

        public Builder<C> defaultToAnyArtist() {
            this.defaultToAnyArtist = true;
            return this;
        }

        public MinimalArtistGrouper<C> build() {
            return new MinimalArtistGrouper<>(this);
        }

        public UnaryOperator<Deck<C>> getModifier() {
            return build().getModifier();
        }
    }

    public enum Scope {
        NORMAL_BASIC_LANDS(spoiler -> Color.getBasicLandTypes().stream().map(t -> spoiler.lookUpByName(t).orElseThrow())),
        BASIC_SNOW_LANDS(spoiler -> Color.getBasicLandTypes().stream().map(t -> spoiler.lookUpByName("Snow-Covered " + t).orElseThrow())),
        ALL_BASIC_LANDS(spoiler -> spoiler.getCards().stream().filter(c -> c.getMainTypeLine().is(CardSupertype.BASIC))),
        ALL_CARDS(spoiler -> {
            throw new UnsupportedOperationException();
        });

        private final Function<Spoiler, Stream<Card>> extractor;

        Scope(Function<Spoiler, Stream<Card>> extractor) {
            this.extractor = extractor;
        }
    }

    private final class Category {
        private final ImmutableCollection<Set<C>> groupedByArtist;

        private Category(Collection<Set<C>> groupedByArtist) {
            this.groupedByArtist = ImmutableList.copyOf(groupedByArtist);
        }

        public List<Predicate<C>> shuffle(DeckRandomChoice randomChoice) {
            List<Set<C>> groupOrder = new ArrayList<>(groupedByArtist);
            if (groupOrder.size() > 1) {
                // Prefer smaller groups first. Order groups of same size at random.
                groupOrder = randomChoice.shuffle(groupOrder);
                groupOrder.sort(Comparator.comparing(Collection::size));
            }

            return groupOrder.stream()
                    .map(group -> (Predicate<C>) group::contains)
                    .collect(ImmutableList.toImmutableList());
        }
    }

    private Category getArtistGroups(Predicate<? super CardEdition> predicate) {
        ImmutableSetMultimap<String, C> artistGroupMap = scope.extractor.apply(spoiler)
                .flatMap(c -> c.getEditions().stream())
                .filter(predicate)
                .flatMap(extractor::fromEdition)
                .collect(MapCollectors.<C>collecting()
                        .indexing((CardVersion c) -> c.getEdition().getArtists().collect(MoreCollectors.onlyElement()))
                        .grouping().toImmutableSetMultimap());
        return new Category(Multimaps.asMap(artistGroupMap).values());
    }

    public UnaryOperator<Deck<C>> getModifier() {
        ImmutableList<Category> categories = predicates.stream()
                .map(this::getArtistGroups)
                .collect(ImmutableList.toImmutableList());

        return (Deck<C> deck) -> {
            DeckRandomChoice[] seeds = DeckRandomChoice.withSalt(0x0afece4f56362d28L).getArrayForDeck(deck, categories.size());
            BasicLandPreferenceSequence.Builder<C> builder = new BasicLandPreferenceSequence.Context<>(spoiler, extractor, infiniteAvailability()).builder();
            if (defaultToAnyArtist) {
                builder.defaultByMixingAll();
            }
            for (int i = 0; i < categories.size(); i++) {
                Category category = categories.get(i);
                List<Predicate<C>> shuffledPredicates = category.shuffle(seeds[i]);
                builder.addAll(shuffledPredicates);
            }
            BasicLandPreferenceSequence<C> build = builder.build();
            return build.apply(deck);
        };
    }

    public static <C extends CardVersion> UnaryOperator<Deck<C>> forCommander(Spoiler spoiler, CardVersionExtractor<C> extractor) {
        return (Deck<C> deck) -> {
            Collection<Expansion> commanderExpansions = deck.get(Deck.Section.COMMANDER).stream()
                    .map((C card) -> card.getEdition().getExpansion())
                    .distinct()
                    .collect(Collectors.toList());
            if (commanderExpansions.size() != 1) return deck;
            Expansion commanderExpansion = Iterables.getOnlyElement(commanderExpansions);
            MinimalArtistGrouper<C> grouper = new Builder<C>(spoiler, extractor)
                    .setScope(Scope.ALL_BASIC_LANDS)
                    .defaultToAnyArtist()
                    .addPredicate(edition -> edition.getExpansion().equals(commanderExpansion))
                    .build();
            return grouper.getModifier().apply(deck);
        };

    }

}
