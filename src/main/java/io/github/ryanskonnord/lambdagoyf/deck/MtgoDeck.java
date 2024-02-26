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

package io.github.ryanskonnord.lambdagoyf.deck;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import io.github.ryanskonnord.lambdagoyf.card.Card;
import io.github.ryanskonnord.lambdagoyf.card.CardFace;
import io.github.ryanskonnord.lambdagoyf.card.CardNames;
import io.github.ryanskonnord.lambdagoyf.card.MtgoCard;
import io.github.ryanskonnord.lambdagoyf.card.Spoiler;
import io.github.ryanskonnord.util.MapCollectors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A deck parsed from an MTGO deck file. It is not guaranteed that every MTGO ID found in the deck file will resolve to
 * an MtgoCard known in this system's Spoiler object.
 */
public class MtgoDeck {

    public static final class CardEntry {
        private final String name;
        private final long id;
        private final Optional<MtgoCard> version;

        private CardEntry(String name, long id, Optional<MtgoCard> version) {
            Preconditions.checkArgument(id > 0);
            this.name = Objects.requireNonNull(name);
            this.id = id;
            this.version = Objects.requireNonNull(version);
            Preconditions.checkArgument(version.isEmpty() || version.get().getMtgoId() == id);
        }

        public CardEntry(MtgoCard version) {
            this(getMtgoName(version.getCard()), version.getMtgoId(), Optional.of(version));
        }

        public String getName() {
            return name;
        }

        public long getId() {
            return id;
        }

        public Optional<MtgoCard> getVersion() {
            return version;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            return id == ((CardEntry) o).id && Objects.equals(name, ((CardEntry) o).name) && version.equals(((CardEntry) o).version);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, id, version);
        }
    }

    public static final class DeckEntry {
        private final int quantity;
        private final CardEntry card;
        private final boolean isInSideboard;

        public DeckEntry(int quantity, CardEntry card, boolean isInSideboard) {
            Preconditions.checkArgument(quantity > 0);
            this.quantity = quantity;
            this.card = Objects.requireNonNull(card);
            this.isInSideboard = isInSideboard;
        }

        public int getQuantity() {
            return quantity;
        }

        public CardEntry getCard() {
            return card;
        }

        public boolean isInSideboard() {
            return isInSideboard;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            return quantity == ((DeckEntry) o).quantity && isInSideboard == ((DeckEntry) o).isInSideboard && card.equals(((DeckEntry) o).card);
        }

        @Override
        public int hashCode() {
            return Objects.hash(quantity, card, isInSideboard);
        }
    }

    static String getMtgoName(Card card) {
        return card.getBaseFaces().map(CardFace::getName).collect(Collectors.joining("/"));
    }

    public static final class Builder {
        private final List<DeckEntry> entries = new ArrayList<>();


        public Builder add(Spoiler spoiler, String name, long id, int quantity, boolean isInSideboard) {
            Optional<MtgoCard> version = spoiler.lookUpByMtgoId(id);
            name = version.isPresent() ? getMtgoName(version.get().getCard()) : name;
            CardEntry cardEntry = new CardEntry(name, id, version);
            entries.add(new DeckEntry(quantity, cardEntry, isInSideboard));
            return this;
        }

        public MtgoDeck build() {
            return new MtgoDeck(ImmutableList.copyOf(entries));
        }
    }

    private final ImmutableList<DeckEntry> entries;
    private final ImmutableSetMultimap<String, CardEntry> cardsByName;

    private MtgoDeck(Collection<DeckEntry> entries) {
        this.entries = ImmutableList.copyOf(entries);
        this.cardsByName = this.entries.stream().map(DeckEntry::getCard)
                .collect(MapCollectors.<CardEntry>collecting()
                        .indexing(e -> CardNames.normalize(e.getName()))
                        .grouping().toImmutableSetMultimap()
                );
    }

    public ImmutableList<DeckEntry> getEntries() {
        return entries;
    }

    public Deck<CardEntry> asDeckObject() {
        Deck.Builder<CardEntry> builder = new Deck.Builder<>();
        for (DeckEntry entry : entries) {
            Deck.Section section = entry.isInSideboard() ? Deck.Section.SIDEBOARD : Deck.Section.MAIN_DECK;
            builder.addTo(section, entry.getCard(), entry.getQuantity());
        }
        return builder.build();
    }

    public ImmutableSet<CardEntry> getEntriesForName(String cardName) {
        return cardsByName.get(CardNames.normalize(cardName));
    }
}
