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

package io.github.ryanskonnord.lambdagoyf.scryfall;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.ryanskonnord.lambdagoyf.Environment;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ScryfallFetcher {
    private final Path rootDirectory;
    private final HttpClient httpClient;
    private final Clock clock;
    private final Predicate<String> typeFilter;
    private final boolean keepOldDownloads;
    private final Duration refreshInterval;
    private final Duration downloadDelay;
    private final Optional<PrintStream> log;

    private ScryfallFetcher(Builder builder) {
        rootDirectory = Objects.requireNonNull(builder.rootDirectory);
        httpClient = Optional.ofNullable(builder.httpClient).orElseGet(() -> HttpClient.newBuilder().build());
        clock = Optional.ofNullable(builder.clock).orElse(Clock.systemUTC());
        typeFilter = Optional.ofNullable(builder.typeFilter).orElse(t -> true);
        keepOldDownloads = Optional.ofNullable(builder.keepOldDownloads).orElse(true);
        refreshInterval = Optional.ofNullable(builder.refreshInterval).orElse(Duration.ofDays(7));
        downloadDelay = Optional.ofNullable(builder.downloadDelay).orElse(Duration.ofMillis(100));
        log = Optional.ofNullable(builder.log);
    }

    public static final class Builder {
        private final Path rootDirectory;
        private HttpClient httpClient;
        private Clock clock;
        private Predicate<String> typeFilter;
        private Boolean keepOldDownloads;
        private Duration refreshInterval;
        private Duration downloadDelay;
        private PrintStream log;

        public Builder(Path rootDirectory) {
            this.rootDirectory = Objects.requireNonNull(rootDirectory);
        }

        public Builder withHttpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public Builder withClock(Clock clock) {
            this.clock = clock;
            return this;
        }

        public Builder withTypeFilter(Predicate<String> typeFilter) {
            this.typeFilter = typeFilter;
            return this;
        }

        public Builder withKeepOldDownloads(boolean keepOldDownloads) {
            this.keepOldDownloads = keepOldDownloads;
            return this;
        }

        public Builder withRefreshInterval(Duration refreshInterval) {
            this.refreshInterval = refreshInterval;
            return this;
        }

        public Builder withDownloadDelay(Duration downloadDelay) {
            this.downloadDelay = downloadDelay;
            return this;
        }

        public Builder setDownloadReport(PrintStream log) {
            this.log = log;
            return this;
        }

        public Builder logToStdout() {
            return setDownloadReport(System.out);
        }

        public ScryfallFetcher build() {
            return new ScryfallFetcher(this);
        }
    }


    private static final DateTimeFormatter FILENAME_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC);
    private static final String MANIFEST_JSON = "manifest.json";
    private static final String SETS_JSON = "sets.json";

    private void log(String message) {
        log.ifPresent(ps -> ps.println(message));
    }

    public Path refresh() throws IOException, InterruptedException {
        Path current = rootDirectory.resolve("current");
        if (!Files.exists(current)) {
            Files.createDirectories(current);
            download(current);
            return current;
        }

        Path manifestPath = current.resolve(MANIFEST_JSON);
        Map<?, ?> manifest;
        try (Reader manifestReader = Files.newBufferedReader(manifestPath)) {
            manifest = new Gson().fromJson(manifestReader, Map.class);
        }
        Instant timestamp = Instant.parse((String) manifest.get("latestUpdated"));
        if (Duration.between(timestamp, clock.instant()).compareTo(refreshInterval) >= 0) {
            if (keepOldDownloads) {
                Path archiveDestination = rootDirectory.resolve(FILENAME_TIMESTAMP_FORMATTER.format(timestamp));
                Files.move(current, archiveDestination);
            } else {
                for (Path file : Files.list(current).collect(Collectors.toList())) {
                    Files.delete(file);
                    log("Deleting: " + file);
                }
                Files.delete(current);
            }
            Files.createDirectories(current);
            download(current);
        }
        return current;
    }

    public void download(Path directory) throws IOException, InterruptedException {
        BulkDataDropSet dropSet = fetchDropSet();
        Map<String, Object> files = new LinkedHashMap<>();

        Collection<BulkDataDrop> drops = dropSet.getDrops();
        for (BulkDataDrop drop : drops) {
            if (typeFilter.test(drop.type)) {
                Thread.sleep(downloadDelay.toMillis());
                Path downloadPath = downloadDrop(drop, directory);
                files.put(drop.type, directory.relativize(downloadPath).toString());
            }
        }
        Instant latestUpdated = drops.stream()
                .map(d -> d.updatedAt)
                .max(Comparator.naturalOrder())
                .orElseThrow(() -> new RuntimeException("No drops found"));

        Thread.sleep(downloadDelay.toMillis());
        httpClient.send(
                HttpRequest.newBuilder(URI.create("https://api.scryfall.com/sets/")).GET().build(),
                HttpResponse.BodyHandlers.ofFile(directory.resolve(SETS_JSON)));
        files.put("sets", SETS_JSON);

        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("downloadTime", clock.instant().toString());
        manifest.put("latestUpdated", latestUpdated.toString());
        manifest.put("files", files);
        manifest.put("metadata", dropSet.metadata);

        Path manifestPath = directory.resolve(MANIFEST_JSON);
        try (Writer manifestWriter = Files.newBufferedWriter(manifestPath)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(manifest, manifestWriter);
        }
    }

    private static final class BulkDataDropSet {
        private final List<?> metadata;

        public BulkDataDropSet(List<?> metadata) {
            this.metadata = Objects.requireNonNull(metadata);
        }

        public ImmutableList<BulkDataDrop> getDrops() {
            return metadata.stream()
                    .map(entry -> new BulkDataDrop((Map<?, ?>) entry))
                    .collect(ImmutableList.toImmutableList());
        }
    }

    private static final class BulkDataDrop {
        private final String type;
        private final Instant updatedAt;
        private final URI downloadUri;

        public BulkDataDrop(Map<?, ?> data) {
            type = (String) Objects.requireNonNull(data.get("type"));
            updatedAt = Instant.parse((String) data.get("updated_at"));
            downloadUri = URI.create((String) data.get("download_uri"));
        }

        private String extractFilename() {
            String downloadUri = this.downloadUri.toString();
            int index = downloadUri.lastIndexOf('/');
            return downloadUri.substring(index + 1);
        }
    }

    private BulkDataDropSet fetchDropSet() throws IOException, InterruptedException {
        HttpRequest bulkDataReq = HttpRequest.newBuilder(URI.create("https://api.scryfall.com/bulk-data")).GET().build();
        HttpResponse<String> bulkDataResponse = httpClient.send(bulkDataReq, HttpResponse.BodyHandlers.ofString());
        Map<?, ?> bulkDataBody = new Gson().fromJson(bulkDataResponse.body(), Map.class);
        return new BulkDataDropSet((List<?>) bulkDataBody.get("data"));
    }

    private Path downloadDrop(BulkDataDrop drop, Path location) throws IOException, InterruptedException {
        log("Downloading from " + drop.downloadUri + " to " + location);
        HttpResponse<Path> response = httpClient.send(
                HttpRequest.newBuilder(drop.downloadUri).GET().build(),
                (HttpResponse.ResponseInfo responseInfo) -> {
                    String filename = responseInfo.headers().firstValue("x-bz-file-name").orElseGet(drop::extractFilename);
                    Path destination = location.resolve(Path.of(filename).getFileName());
                    return HttpResponse.BodyHandlers.ofFile(destination).apply(responseInfo);
                });
        return response.body();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        new ScryfallFetcher.Builder(Environment.getScryfallResourcePath()).build().refresh();
    }
}
