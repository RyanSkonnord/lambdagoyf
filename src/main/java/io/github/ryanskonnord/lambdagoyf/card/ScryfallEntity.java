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

package io.github.ryanskonnord.lambdagoyf.card;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import java.util.UUID;

public abstract class ScryfallEntity {

    public abstract UUID getScryfallId();

    @Override
    public final boolean equals(Object o) {
        return this == o || o != null && getClass() == o.getClass()
                && getScryfallId().equals(((ScryfallEntity) o).getScryfallId());
    }

    private static final HashFunction HASH_FUNCTION = Hashing.goodFastHash(Integer.SIZE);
    private transient int hash;

    @Override
    public final int hashCode() {
        if (hash != 0) return hash;
        UUID scryfallId = getScryfallId();
        return hash = HASH_FUNCTION.newHasher()
                .putLong(scryfallId.getMostSignificantBits())
                .putLong(scryfallId.getLeastSignificantBits())
                .hash().asInt();
    }

}
