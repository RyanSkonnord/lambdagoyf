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

public enum PromoType implements WordType {
    EVENT,
    CONVENTION,
    MEDIA_INSERT,
    IN_STORE,
    ARENA_LEAGUE,
    DATE_STAMPED,
    PRERELEASE,
    JUDGE_GIFT,
    SET_PROMO,
    TOURNEY,
    FNM,
    PLAYER_REWARDS,
    RELEASE,
    STARTER_DECK,
    PREMIERE_SHOP,
    GATEWAY,
    GAME_DAY,
    WIZARDS_PLAY_NETWORK,
    DUELS,
    BUY_A_BOX,
    LEAGUE,
    GIFT_BOX,
    STAMPED,
    PROMO_PACK,
    INTRO_PACK,
    PLANESWALKER_DECK,
    DRAFT_WEEKEND,
    OPEN_HOUSE,
    BOX_TOPPER,
    JP_WALKER,
    BUNDLE,
    BOOSTER_FUN,
    BRAWL_DECK,
    THEME_PACK,
    GODZILLA_SERIES,
    DRACULA_SERIES,
    PLAY_PROMO;

    private final String key;

    PromoType() {
        key = name().toLowerCase().replace("_", "");
    }

    @Override
    public String getKey() {
        return key;
    }
}
