/**
 * Copyright (C) 2017 Marvin Herman Froeder (marvin@marvinformatics.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.marvinformatics.toml;

import com.marvinformatics.toml.wikipedia.Owner;
import com.moandjiezana.toml.Toml;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

public class WikipediaDefaultedTest {

    private Wikipedia wikipedia;

    @Before
    public void setup() {
        final Toml toml = new Toml().read("a = 1");

        wikipedia = new Wikipedia(toml);
    }

    @Test
    public void title() {
        Assertions.assertThat(wikipedia.title("a title"))
                .isEqualTo("a title");
    }

    @Test
    public void owner() {
        final Owner fallbackOwner = new Owner(new Toml().read("a = 1"));
        Assertions.assertThat(wikipedia.owner(fallbackOwner))
                .isNotNull();

        Assertions.assertThat(wikipedia.owner(fallbackOwner).name("me"))
                .isEqualTo("me");
    }

}