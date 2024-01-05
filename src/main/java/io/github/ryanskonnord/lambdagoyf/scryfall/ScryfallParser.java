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
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.ImmutableLongArray;
import com.google.gson.Gson;
import io.github.ryanskonnord.lambdagoyf.Environment;
import io.github.ryanskonnord.lambdagoyf.card.CardFactory;
import io.github.ryanskonnord.lambdagoyf.card.ExpansionSpoiler;
import io.github.ryanskonnord.lambdagoyf.card.Spoiler;
import io.github.ryanskonnord.util.MapCollectors;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ScryfallParser {

    public static final String BULK_DATA_TYPE = "default_cards";

    public static Spoiler createSpoiler() throws IOException, InterruptedException {
        Path location = Environment.getScryfallResourcePath();
        ScryfallFetcher.Builder builder = new ScryfallFetcher.Builder(location).logToStdout();
        if (BULK_DATA_TYPE != null) {
            builder = builder.withTypeFilter(BULK_DATA_TYPE::equals);
        }

        Path data = builder.build().refresh();
        return new ScryfallParser().parseScryfallData(data).createSpoiler();
    }

    private <T> T readJsonFile(Path directory, String filename, Class<T> type) throws IOException {
        try (Reader reader = Files.newBufferedReader(directory.resolve(filename))) {
            return new Gson().fromJson(reader, type);
        }
    }

    public CardFactory parseScryfallData(Path directory) throws IOException {
        Map<?, ?> manifest = readJsonFile(directory, "manifest.json", Map.class);
        Map<?, ?> files = (Map<?, ?>) manifest.get("files");
        String filename = (String) files.get(BULK_DATA_TYPE);

        Map<?, ?> setJson = readJsonFile(directory, "sets.json", Map.class);
        Collection<ScryfallSet> setData = ((List<?>) setJson.get("data")).stream()
                .map(e -> new ScryfallSet((Map<?, ?>) e))
                .collect(ImmutableList.toImmutableList());
        ExpansionSpoiler expansions = new ExpansionSpoiler(setData);

        List<Map<?, ?>> cards = readJsonFile(directory, filename, List.class);

        Set<String> unaccountedKeys = Collections.synchronizedSet(new TreeSet<>());
        List<ScryfallCardEntry> cardEntries = cards.parallelStream()
                .map((Map<?, ?> data) -> new ScryfallCardEntry((Map<String, ?>) data, unaccountedKeys::add))
                .collect(Collectors.toCollection(ArrayList::new));
        if (!unaccountedKeys.isEmpty()) {
            System.err.println("Unaccounted keys: " + unaccountedKeys);
        }
        Collections.shuffle(cardEntries);

        return new CardFactory(expansions, cardEntries);
    }


    private static long checkInteger(Double number) {
        long integer = number.longValue();
        if (integer != number) {
            throw new ScryfallDataException();
        }
        return integer;
    }

    static OptionalLong parseNumber(Double number) {
        return number == null ? OptionalLong.empty() : OptionalLong.of(checkInteger(number));
    }

    static ImmutableLongArray parseNumbers(List<?> numbers) {
        ImmutableLongArray.Builder builder = ImmutableLongArray.builder(numbers.size());
        for (Object number : numbers) {
            builder.add(checkInteger((Double) number));
        }
        return builder.build();
    }

    static ImmutableList<String> parseStrings(List<?> list) {
        return list.stream().map(String.class::cast).collect(ImmutableList.toImmutableList());
    }

    static ImmutableList<String> parseOptionalStrings(List<?> list) {
        return Optional.ofNullable(list).map(ScryfallParser::parseStrings).orElse(ImmutableList.of());
    }

    static ImmutableMap<String, String> parseStringMap(Map<?, ?> map) {
        return map.entrySet().stream()
                .filter((Map.Entry<?, ?> entry) -> entry.getValue() != null)
                .collect(MapCollectors.<Map.Entry<?, ?>>collecting()
                        .withKey((Map.Entry<?, ?> entry) -> (String) entry.getKey())
                        .withValue((Map.Entry<?, ?> entry) -> (String) entry.getValue())
                        .unique().toImmutableMap());
    }

    static <T> Function<List<?>, ImmutableList<T>> parseObjectList(Function<Map<?, ?>, T> elementParser) {
        return (List<?> list) -> list.stream()
                .map((Object element) -> {
                    T parsedElement;
                    try {
                        parsedElement = elementParser.apply((Map<?, ?>) element);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return Optional.<T>empty();
                    }
                    return Optional.of(parsedElement);
                })
                .flatMap(Optional::stream)
                .collect(ImmutableList.toImmutableList());
    }

}
