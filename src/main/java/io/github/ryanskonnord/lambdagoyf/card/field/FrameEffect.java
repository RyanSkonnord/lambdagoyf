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

package io.github.ryanskonnord.lambdagoyf.card.field;

import io.github.ryanskonnord.lambdagoyf.card.WordType;

public enum FrameEffect implements WordType {
    TOMBSTONE,
    COLORSHIFTED,
    SUN_MOON_DFC,
    MIRACLE,
    NYXTOUCHED,
    ORIGIN_PW_DFC,
    DEVOID,
    MOON_ELDRAZI_DFC,
    DRAFT,
    COMPASS_LAND_DFC,
    LEGENDARY,
    INVERTED,
    EXTENDED_ART,
    SHOWCASE,
    WAXING_AND_WANING_MOON_DFC,
    NYXBORN,
    COMPANION,
    FULL_ART,
    ETCHED,
    SNOW,
    LESSON,
    BOOSTER;

    private final String key;

    FrameEffect() {
        key = name().toLowerCase().replace("_", "");
    }

    @Override
    public String getKey() {
        return key;
    }
}
