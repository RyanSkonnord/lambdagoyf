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
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.hash.PrimitiveSink;
import io.github.ryanskonnord.lambdagoyf.card.CardIdentity;
import io.github.ryanskonnord.lambdagoyf.card.DeckElement;
import io.github.ryanskonnord.util.MinimalRng;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.IntStream;

public class DeckRandomChoice implements MinimalRng {

    /*
     * Implement an RNG that has good performance on adjacent seeds. Adjacent seeds yield inadequate diffusion from
     * java.util.Random, as evidenced by
     *
     *     long base = System.currentTimeMillis();
     *     for (int i = 0; i < 100; i++) { System.out.println(new java.util.Random(base + i).nextBoolean()); }
     */
    private static final class SplitMix64 implements MinimalRng {
        private long state;

        public SplitMix64(long seed) {
            this.state = seed;
        }

        private long nextLong() {
            // Derived from splitmix64.c by Sebastiano Vigna (2015), which is in the public domain.
            // Source: https://xoshiro.di.unimi.it/splitmix64.c
            state += 0x9e3779b97f4a7c15L;
            long z = state;
            z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
            z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
            return z ^ (z >>> 31);
        }

        @Override
        public int generateInt(int bound) {
            Preconditions.checkArgument(bound > 0);
            long choice, value;
            do {
                choice = nextLong() & Long.MAX_VALUE;
                value = choice % bound;
            } while (choice - value + (long) bound + 1 < 0);
            return (int) value;
        }
    }


    private static final HashFunction HASH_FUNCTION = Hashing.sha256();

    public static DeckHasher withSalt(long salt) {
        return new DeckHasher(salt);
    }

    public static final class DeckHasher {
        private final long salt;

        public DeckHasher(long salt) {
            this.salt = salt;
        }

        private static void putUuid(UUID from, PrimitiveSink into) {
            into.putLong(from.getMostSignificantBits());
            into.putLong(from.getLeastSignificantBits());
        }

        private <C extends CardIdentity> long digestDeck(Deck<C> deck) {
            Hasher sink = HASH_FUNCTION.newHasher();
            for (Map.Entry<Deck.Section, ImmutableMultiset<C>> sectionEntry : deck.getAllSections()) {
                Deck.Section section = sectionEntry.getKey();
                sink.putInt(section.ordinal());

                List<Multiset.Entry<C>> cardEntries = new ArrayList<>(sectionEntry.getValue().entrySet());
                cardEntries.sort(Comparator.comparing(e -> e.getElement().getCard().getScryfallId()));
                for (Multiset.Entry<C> cardEntry : cardEntries) {
                    sink.putInt(cardEntry.getCount());
                    sink.putObject(cardEntry.getElement().getCard().getScryfallId(), DeckHasher::putUuid);
                }
            }
            return sink.hash().asLong() ^ salt;
        }

        public <C extends CardIdentity> DeckRandomChoice forDeck(Deck<C> deck) {
            return new DeckRandomChoice(digestDeck(deck));
        }

        public <C extends CardIdentity> DeckRandomChoice[] getArrayForDeck(Deck<C> deck, int length) {
            Preconditions.checkArgument(length >= 0);
            long seed = digestDeck(deck);
            return IntStream.range(0, length)
                    .mapToObj(n -> new DeckRandomChoice(seed + n))
                    .toArray(DeckRandomChoice[]::new);
        }
    }


    private final long seed;

    private DeckRandomChoice(long seed) {
        this.seed = seed;
    }

    public DeckRandomChoice forCard(CardIdentity card) {
        return new DeckRandomChoice(seed ^ card.getCard().getScryfallId().getLeastSignificantBits());
    }

    public MinimalRng getStatefulRng() {
        return new SplitMix64(seed);
    }

    @Override
    public int generateInt(int bound) {
        return getStatefulRng().generateInt(bound);
    }

    private static String generateSalt() {
        long salt = new SecureRandom().nextLong();
        return String.format("0x%016xL", salt);
    }

    /**
     * Run this to generate a new salt.
     */
    public static void main(String[] args) {
        System.out.println(String.format("DeckRandomChoice.withSalt(%s)", generateSalt()));
    }

}
