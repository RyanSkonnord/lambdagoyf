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

package io.github.ryanskonnord.util;

import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface MinimalRng {

    public int generateInt(int bound);

    public default <E> E choose(Collection<E> collection) {
        return Iterables.get(collection, generateInt(collection.size()));
    }

    public default <E> List<E> shuffle(Collection<E> collection) {
        List<E> list = new ArrayList<>(collection);
        int size = list.size();
        for (int i = 0; i < size - 1; i++) {
            int j = i + generateInt(size - i);
            E swap = list.get(i);
            list.set(i, list.get(j));
            list.set(j, swap);
        }
        return list;
    }

}
