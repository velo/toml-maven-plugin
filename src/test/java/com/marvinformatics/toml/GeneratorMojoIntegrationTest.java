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

import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import java.io.File;

import io.takari.maven.testing.TestResources;
import io.takari.maven.testing.executor.MavenRuntime;
import io.takari.maven.testing.executor.MavenRuntime.MavenRuntimeBuilder;
import io.takari.maven.testing.executor.MavenVersions;
import io.takari.maven.testing.executor.junit.MavenJUnitTestRunner;

@RunWith(MavenJUnitTestRunner.class)
@MavenVersions({ "3.3.9" })
public class GeneratorMojoIntegrationTest {

    @Rule
    public final TestResources resources = new TestResources();

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    public final MavenRuntime maven;

    public GeneratorMojoIntegrationTest(MavenRuntimeBuilder mavenBuilder) throws Exception {
        this.maven = mavenBuilder.withCliOptions("-B", "-U").build();
    }

    @Test
    public void success() throws Exception {
        maven.forProject(resources.getBasedir("wikipedia"))
                .withCliOption("-X")
                .execute("install")
                .assertErrorFreeLog()
                .assertLogText("toml-maven-plugin:0.1-SNAPSHOT:generate")
                .assertLogText("Writting com.marvinformatics.toml.Wikipedia")
                .assertLogText("Writting com.marvinformatics.toml.wikipedia.Owner")
                .assertLogText("Writting com.marvinformatics.toml.wikipedia.Database")
                .getBasedir();
    }

    @Test
    public void toml4jExamples() throws Exception {
        File project = maven.forProject(resources.getBasedir("toml4j"))
                .withCliOption("-X")
                .execute("com.marvinformatics.toml:toml-maven-plugin:test-generate",
                        "test-compile",
                        "-Dtoml.excludes=**/invalid/*,**/valid/*,**/hard_example_errors.toml")
                .assertErrorFreeLog()
                .assertLogText("toml-maven-plugin:0.1-SNAPSHOT:test-generate")
                .getBasedir();

        File classes = new File(project, "target/test-classes");
        Assertions.assertThat(classes)
                .exists();
        File hardBit = new File(classes, "com/marvinformatics/toml/hard_example/the/hard/Bit.class");
        Assertions.assertThat(hardBit)
                .exists();
    }

}
