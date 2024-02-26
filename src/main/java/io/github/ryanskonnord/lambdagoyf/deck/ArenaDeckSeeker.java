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

import io.github.ryanskonnord.lambdagoyf.card.ArenaCard;
import io.github.ryanskonnord.lambdagoyf.card.Spoiler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;

public final class ArenaDeckSeeker {

    private final Spoiler spoiler;
    private final DeckConstructor<ArenaCard, ArenaDeckEntry> deckConstructor;

    public ArenaDeckSeeker(Spoiler spoiler, DeckConstructor<ArenaCard, ArenaDeckEntry> deckConstructor) {
        this.spoiler = Objects.requireNonNull(spoiler);
        this.deckConstructor = Objects.requireNonNull(deckConstructor);
    }

    public static final class Entry {
        private final Path source;
        private final Path destination;

        private Entry(Path source, Path destination) {
            this.source = Objects.requireNonNull(source);
            this.destination = Objects.requireNonNull(destination);
        }

        public Path getSource() {
            return source;
        }

        public Path getDestination() {
            return destination;
        }

        @Override
        public boolean equals(Object o) {
            return this == o || o instanceof Entry
                    && source.equals(((Entry) o).source)
                    && destination.equals(((Entry) o).destination);
        }

        @Override
        public int hashCode() {
            return 31 * source.hashCode() + destination.hashCode();
        }
    }

    private static final String TXT_SUFFIX = ".txt";

    public Stream<Entry> seek(Path sourceDirectory, Path destinationDirectory) throws IOException {
        return Files.walk(sourceDirectory, 1)
                .filter((Path path) -> path.getFileName().toString().endsWith(TXT_SUFFIX))
                .map((Path source) -> new Entry(source, destinationDirectory.resolve(source.getFileName())));
    }

    @Override

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ArenaDeckSeeker)) return false;

        ArenaDeckSeeker that = (ArenaDeckSeeker) o;
        if (!spoiler.equals(that.spoiler)) return false;
        return deckConstructor.equals(that.deckConstructor);
    }

    @Override
    public int hashCode() {
        int result = spoiler.hashCode();
        result = 31 * result + deckConstructor.hashCode();
        return result;
    }
}
