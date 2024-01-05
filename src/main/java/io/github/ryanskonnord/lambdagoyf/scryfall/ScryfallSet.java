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

import com.google.common.primitives.Ints;

import java.net.URI;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

public final class ScryfallSet {

    private final Optional<String> block;
    private final Optional<String> blockCode;
    private final int cardCount;
    private final String code;
    private final boolean digital;
    private final boolean foilOnly;
    private final URI iconSvgUri;
    private final UUID id;
    private final Optional<String> mtgoCode;
    private final String name;
    private final String object;
    private final Optional<String> parentSetCode;
    private final LocalDate releasedAt;
    private final URI scryfallUri;
    private final URI searchUri;
    private final String setType;
    private final OptionalLong tcgplayerId;
    private final URI uri;

    public ScryfallSet(Map<?, ?> data) {
        block = Optional.ofNullable((String) data.get("block"));
        blockCode = Optional.ofNullable((String) data.get("block_code"));
        cardCount = Ints.checkedCast(ScryfallParser.parseNumber((Double) data.get("card_count")).orElseThrow(ScryfallDataException::new));
        code = Objects.requireNonNull((String) data.get("code"));
        digital = Objects.requireNonNull((Boolean) data.get("digital"));
        foilOnly = Objects.requireNonNull((Boolean) data.get("foil_only"));
        iconSvgUri = URI.create((String) data.get("icon_svg_uri"));
        id = UUID.fromString((String) data.get("id"));
        mtgoCode = Optional.ofNullable((String) data.get("mtgo_code"));
        name = Objects.requireNonNull((String) data.get("name"));
        object = Objects.requireNonNull((String) data.get("object"));
        parentSetCode = Optional.ofNullable((String) data.get("parent_set_code"));
        releasedAt = LocalDate.parse((String) data.get("released_at"));
        scryfallUri = URI.create((String) data.get("scryfall_uri"));
        searchUri = URI.create((String) data.get("search_uri"));
        setType = Objects.requireNonNull((String) data.get("set_type"));
        tcgplayerId = ScryfallParser.parseNumber((Double) data.get("tcgplayer_id"));
        uri = URI.create((String) data.get("uri"));
    }

    public Optional<String> getBlock() {
        return block;
    }

    public Optional<String> getBlockCode() {
        return blockCode;
    }

    public int getCardCount() {
        return cardCount;
    }

    public String getCode() {
        return code;
    }

    public boolean isDigital() {
        return digital;
    }

    public boolean isFoilOnly() {
        return foilOnly;
    }

    public URI getIconSvgUri() {
        return iconSvgUri;
    }

    public UUID getId() {
        return id;
    }

    public Optional<String> getMtgoCode() {
        return mtgoCode;
    }

    public String getName() {
        return name;
    }

    public String getObject() {
        return object;
    }

    public Optional<String> getParentSetCode() {
        return parentSetCode;
    }

    public LocalDate getReleasedAt() {
        return releasedAt;
    }

    public URI getScryfallUri() {
        return scryfallUri;
    }

    public URI getSearchUri() {
        return searchUri;
    }

    public String getSetType() {
        return setType;
    }

    public OptionalLong getTcgplayerId() {
        return tcgplayerId;
    }

    public URI getUri() {
        return uri;
    }
}
