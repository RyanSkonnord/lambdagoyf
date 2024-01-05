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

package io.github.ryanskonnord.lambdagoyf;

import java.nio.file.Path;
import java.util.Optional;

public class Environment {
    private Environment() {
        throw new RuntimeException();
    }

    private static Path getEnvironmentalPath(String name) {
        return Optional.ofNullable(System.getenv(name)).map(Path::of)
                .orElseThrow(() -> new RuntimeException(String.format("Environment variable '%s' must be set", name)));
    }

    public static Path getInternalResourcePath() {
        return getEnvironmentalPath("INTERNAL_RESOURCES");
    }

    public static Path getScryfallResourcePath() {
        return getEnvironmentalPath("SCRYFALL_RESOURCES");
    }

    public static Path getDeckFilePath() {
        return getEnvironmentalPath("DECK_FILES");
    }
}
