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
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.github.ryanskonnord.util.MapCollectors;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class MtgoDeckSeeker {

    private final Path root;
    private final Predicate<Path> filter;

    public MtgoDeckSeeker(Path root) {
        this(root, path -> true);
    }

    public MtgoDeckSeeker(Path root, Predicate<Path> filter) {
        this.root = Objects.requireNonNull(root);
        this.filter = Objects.requireNonNull(filter);
    }

    public static enum MtgoDeckFileFormat {
        TXT, DEK, CSV;

        private String getExtension() {
            return name().toLowerCase();
        }

        private static final ImmutableMap<String, MtgoDeckFileFormat> BY_EXTENSION = EnumSet.allOf(MtgoDeckFileFormat.class)
                .stream().collect(MapCollectors.<MtgoDeckFileFormat>collecting()
                        .indexing(MtgoDeckFileFormat::getExtension)
                        .unique().toImmutableSortedMap(String.CASE_INSENSITIVE_ORDER));
    }

    public static final class MtgoDeckEntry {
        private final Path base;
        private final ImmutableMap<MtgoDeckFileFormat, Path> files;

        private MtgoDeckEntry(Path base, Builder builder) {
            this.base = base;
            this.files = Maps.immutableEnumMap(builder.files);
        }

        private static class Builder {
            private final Map<MtgoDeckFileFormat, Path> files = new EnumMap<>(MtgoDeckFileFormat.class);
        }

        public Optional<Path> getExisting(MtgoDeckFileFormat format) {
            return Optional.ofNullable(files.get(Objects.requireNonNull(format)));
        }

        public boolean hasFile(MtgoDeckFileFormat format) {
            return files.containsKey(Objects.requireNonNull(format));
        }

        public Path getPath(MtgoDeckFileFormat format) {
            return getExisting(format).orElseGet(() -> Paths.get(base + "." + format.getExtension()));
        }

        @Override
        public boolean equals(Object o) {
            return this == o || !(o == null || getClass() != o.getClass()) && files.equals(((MtgoDeckEntry) o).files);
        }

        @Override
        public int hashCode() {
            return files.hashCode();
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).add("files", files).toString();
        }
    }

    private static final Pattern FILE_EXTENSION_PATTERN = Pattern.compile("(?<name>.*)\\.(?<extension>\\w+)");

    public ImmutableCollection<MtgoDeckEntry> seek() throws IOException {
        List<Path> paths = Files.walk(root).filter(filter).collect(Collectors.toList());
        Map<Path, MtgoDeckEntry.Builder> builders = new HashMap<>();
        for (Path path : paths) {
            Matcher matcher = FILE_EXTENSION_PATTERN.matcher(path.toAbsolutePath().toString());
            if (matcher.matches()) {
                String extension = matcher.group("extension");
                MtgoDeckFileFormat format = MtgoDeckFileFormat.BY_EXTENSION.get(extension);
                if (format != null) {
                    Path name = Paths.get(matcher.group("name"));
                    MtgoDeckEntry.Builder builder = builders.get(name);
                    if (builder == null) {
                        builders.put(name, builder = new MtgoDeckEntry.Builder());
                    }
                    builder.files.put(format, path);
                }
            }
        }
        return builders.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(e -> new MtgoDeckEntry(e.getKey(), e.getValue()))
                .collect(ImmutableList.toImmutableList());
    }

}
