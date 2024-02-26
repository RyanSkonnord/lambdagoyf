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

package io.github.ryanskonnord.lambdagoyf.deck;

import com.google.common.base.Preconditions;

import java.util.Objects;

public class ArenaVersionId {
    private final String expansionCode;
    private final int collectorNumber;

    public ArenaVersionId(String expansionCode, int collectorNumber) {
        Preconditions.checkArgument(collectorNumber > 0);
        this.expansionCode = Objects.requireNonNull(expansionCode);
        this.collectorNumber = collectorNumber;
    }

    public String getExpansionCode() {
        return expansionCode;
    }

    public int getCollectorNumber() {
        return collectorNumber;
    }

    @Override
    public String toString() {
        return String.format("(%s) %d", expansionCode, collectorNumber);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o != null && getClass() == o.getClass()
                && collectorNumber == ((ArenaVersionId) o).collectorNumber
                && expansionCode.equals(((ArenaVersionId) o).expansionCode);
    }

    @Override
    public int hashCode() {
        return 31 * expansionCode.hashCode() + collectorNumber;
    }
}
