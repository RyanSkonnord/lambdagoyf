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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import io.github.ryanskonnord.lambdagoyf.card.WordType;

public enum CardLayout implements WordType {
    NORMAL,
    VANGUARD,
    TOKEN,
    SPLIT,
    FLIP,
    PLANAR,
    LEVELER,
    SCHEME,
    TRANSFORM,
    DOUBLE_FACED_TOKEN,
    EMBLEM,
    MELD,
    HOST,
    AUGMENT,
    SAGA,
    ADVENTURE,
    ART_SERIES,
    MODAL_DFC,
    CLASS;

    @Override
    public String getKey() {
        return name().toLowerCase();
    }

    private static final ImmutableSet<CardLayout> PSEUDO_CARD_LAYOUTS = Sets.immutableEnumSet(
            VANGUARD, TOKEN, PLANAR, SCHEME, DOUBLE_FACED_TOKEN, EMBLEM, ART_SERIES);

}
