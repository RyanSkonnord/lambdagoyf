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

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class DeckSeeker {

    public static enum DeckFileFormat {
        SOURCE("txt", null, false),
        DEK("dek", "dek", true),
        CSV("csv", "csv", true),
        ARENA("txt", "arena", false);

        private final String fileExtension;
        private final Optional<String> subdirectoryName;
        private final boolean usesFormatTag;

        DeckFileFormat(String fileExtension, String subdirectoryName, boolean usesFormatTag) {
            this.fileExtension = "." + fileExtension;
            this.subdirectoryName = Optional.ofNullable(subdirectoryName);
            this.usesFormatTag = usesFormatTag;
        }
    }

    private static final ImmutableSet<String> ALL_SUBDIRECTORY_NAMES = EnumSet.allOf(DeckFileFormat.class).stream()
            .map(f -> f.subdirectoryName).flatMap(Optional::stream)
            .collect(ImmutableSet.toImmutableSet());

    private final Path directory;
    private final Optional<String> formatTag;

    public DeckSeeker(Path directory, String formatTag) {
        this.directory = Objects.requireNonNull(directory);
        this.formatTag = Optional.ofNullable(formatTag).filter(t -> !t.isEmpty());
    }

    public static Stream<DeckSeeker> walk(Path root, String formatTag) throws IOException {
        return Files.walk(root)
                .filter(p -> Files.isDirectory(p) && !ALL_SUBDIRECTORY_NAMES.contains(p.getFileName().toString()))
                .map(p -> new DeckSeeker(p, formatTag));
    }

    public static Collection<Entry> seekAll(Path root, String formatTag) throws IOException {
        Collection<Entry> entries = new ArrayList<>();
        for (Iterator<DeckSeeker> seekers = walk(root, formatTag).iterator(); seekers.hasNext(); ) {
            seekers.next().seek().forEachOrdered(entries::add);
        }
        return entries;
    }

    private Path getDirectoryFor(DeckFileFormat fileFormat) {
        return fileFormat.subdirectoryName.map(directory::resolve).orElse(directory);
    }

    private String createFormatTagPrefix(DeckFileFormat fileFormat) {
        return fileFormat.usesFormatTag ? formatTag.map(tag -> String.format("[%s] ", tag)).orElse("") : "";
    }

    private static Stream<Path> listIfExists(Path dir) throws IOException {
        return Files.exists(dir) ? Files.list(dir) : Stream.of();
    }

    private Collection<String> findNames(DeckFileFormat fileFormat) throws IOException {
        Pattern fileNamePattern = Pattern.compile(Pattern.quote(createFormatTagPrefix(fileFormat))
                + "(?<name>.*)" + Pattern.quote(fileFormat.fileExtension));
        Path subdirectory = getDirectoryFor(fileFormat);
        return listIfExists(subdirectory)
                .map((Path p) -> {
                    if (Files.isDirectory(p)) return Optional.<String>empty();
                    Matcher matcher = fileNamePattern.matcher(p.getFileName().toString());
                    return matcher.matches() ? Optional.of(matcher.group("name")) : Optional.<String>empty();
                })
                .flatMap(Optional::stream)
                .collect(ImmutableList.toImmutableList());
    }

    public Stream<Entry> seek() throws IOException {
        SetMultimap<String, DeckFileFormat> entries = MultimapBuilder.hashKeys().enumSetValues(DeckFileFormat.class).build();
        for (DeckFileFormat fileFormat : EnumSet.allOf(DeckFileFormat.class)) {
            for (String name : findNames(fileFormat)) {
                entries.put(name, fileFormat);
            }
        }
        return entries.asMap().entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map((Map.Entry<String, Collection<DeckFileFormat>> e) -> new Entry(e.getKey(), e.getValue()));
    }

    public final class Entry {
        private final String name;
        private final ImmutableSet<DeckFileFormat> presentFiles;

        private Entry(String name, Collection<DeckFileFormat> presentFiles) {
            this.name = Objects.requireNonNull(name);
            this.presentFiles = Sets.immutableEnumSet(presentFiles);
        }

        public String getName() {
            return name;
        }

        public ImmutableSet<DeckFileFormat> getPresentFiles() {
            return presentFiles;
        }

        public boolean hasFile(DeckFileFormat fileFormat) {
            return presentFiles.contains(fileFormat);
        }

        public Path getPath(DeckFileFormat fileFormat) {
            String fileName = createFormatTagPrefix(fileFormat) + name + fileFormat.fileExtension;
            return getDirectoryFor(fileFormat).resolve(fileName);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("name", name)
                    .add("presentFiles", presentFiles)
                    .toString();
        }
    }

}
