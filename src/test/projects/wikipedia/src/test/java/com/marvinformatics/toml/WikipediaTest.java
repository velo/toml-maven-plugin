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

import com.google.common.collect.Lists;
import com.moandjiezana.toml.Toml;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

public class WikipediaTest {

    private Wikipedia wikipedia;

    @Before
    public void setup() {
        final Toml toml = new Toml().read(new File("src/main/resources/wikipedia.toml"));

        wikipedia = new Wikipedia(toml);
    }

    @Test
    public void title() {
        Assertions.assertThat(wikipedia.title())
                .isEqualTo("TOML Example");
    }

    @Test
    public void owner() {
        Assertions.assertThat(wikipedia.owner())
                .isNotNull();

        Assertions.assertThat(wikipedia.owner().name())
                .isEqualTo("Tom Preston-Werner");

        Assertions.assertThat(wikipedia.owner().dob())
                .hasYear(1979)
                .hasMonth(05);
    }

    @Test
    public void clients() {
        Assertions.assertThat(wikipedia.clients())
                .isNotNull();

        Assertions.assertThat(wikipedia.clients().hosts())
                .contains("alpha", "omega");

        Assertions.assertThat(wikipedia.clients().data())
                .contains(
                        Lists.<Object> newArrayList("gamma", "delta"),
                        Lists.<Object> newArrayList(1L, 2L));
    }

    @Test
    public void servers() {
        Assertions.assertThat(wikipedia.servers())
                .isNotNull();

        Assertions.assertThat(wikipedia.servers().alpha())
                .isNotNull();
        Assertions.assertThat(wikipedia.servers().alpha().ip())
                .isEqualTo("10.0.0.1");
        Assertions.assertThat(wikipedia.servers().alpha().dc())
                .isEqualTo("eqdc10");

        Assertions.assertThat(wikipedia.servers().beta())
                .isNotNull();
        Assertions.assertThat(wikipedia.servers().beta().ip())
                .isEqualTo("10.0.0.2");
        Assertions.assertThat(wikipedia.servers().beta().dc())
                .isEqualTo("eqdc10");
    }

}
