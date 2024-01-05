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

import java.util.Optional;
import java.util.UUID;

public interface ScryfallCardFace {

    Optional<String> getArtist();

    Optional<ImmutableList<String>> getColorIndicator();

    Optional<ImmutableList<String>> getColors();

    Optional<String> getFlavorText();

    Optional<UUID> getIllustrationId();

    Optional<ImmutableMap<String, String>> getImageUris();

    Optional<String> getLoyalty();

    Optional<String> getManaCost();

    String getName();

    String getObject();

    Optional<String> getOracleText();

    Optional<String> getPower();

    Optional<String> getPrintedName();

    Optional<String> getFlavorName();

    Optional<String> getToughness();

    Optional<String> getTypeLine();

    Optional<String> getWatermark();

}
