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

import io.github.ryanskonnord.lambdagoyf.card.Word;
import io.github.ryanskonnord.lambdagoyf.card.WordType;

public enum ExpansionType implements WordType {
    EXPANSION,
    CORE,
    MASTERS,
    PROMO,
    COMMANDER,
    BOX,
    MEMORABILIA,
    DUEL_DECK,
    TOKEN,
    STARTER,
    DRAFT_INNOVATION,
    PLANECHASE,
    FUNNY,
    TREASURE_CHEST,
    ARCHENEMY,
    MASTERPIECE,
    FROM_THE_VAULT,
    VANGUARD,
    PREMIUM_DECK,
    SPELLBOOK,
    ALCHEMY,
    MINIGAME,
    ARSENAL;

    @Override
    public String getKey() {
        return name().toLowerCase();
    }

    public boolean isStandardRelease() {
        return this == EXPANSION || this == CORE;
    }

    public static boolean isStandardRelease(Word<ExpansionType> expansionType) {
        return expansionType.is(EXPANSION) || expansionType.is(CORE);
    }

}
