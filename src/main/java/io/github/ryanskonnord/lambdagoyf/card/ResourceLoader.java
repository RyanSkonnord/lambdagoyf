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

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;

final class ResourceLoader {

    private static InputStream openResource(String name) {
        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
        return stream != null ? stream : ResourceLoader.class.getResourceAsStream(name);
    }

    public static ImmutableList<String> getResourceNames(String root) {
        ImmutableList.Builder<String> resourceNames = ImmutableList.builder();
        try (InputStream stream = openResource(root);
             BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String resourceName = root + "/" + line;
                resourceNames.add(resourceName);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return resourceNames.build();
    }

    public static Object readYamlResource(String resourceName) {
        try (InputStream stream = openResource(resourceName);
             Reader reader = new BufferedReader(new InputStreamReader(stream, Charsets.UTF_8))) {
            return new Yaml().load(reader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static final class NamedYamlResource {
        private final String resourceName;
        private final Object resource;

        private NamedYamlResource(String resourceName, Object resource) {
            this.resourceName = Objects.requireNonNull(resourceName);
            this.resource = resource;
        }

        public String getResourceName() {
            return resourceName;
        }

        public Object getResource() {
            return resource;
        }
    }

    public static Stream<NamedYamlResource> readAllYamlResources(String root) {
        return ResourceLoader.getResourceNames(root).stream()
                .map((String resourceName) -> {
                    Object resource = readYamlResource(resourceName);
                    return new NamedYamlResource(resourceName, resource);
                });
    }

    public static Stream<Object> combineAllYamlLists(String root) {
        return ResourceLoader.getResourceNames(root).stream()
                .flatMap((String resourceName) -> {
                    Collection<?> collection = (Collection<?>) readYamlResource(resourceName);
                    return (collection == null) ? Stream.empty() : collection.stream();
                });
    }

}
