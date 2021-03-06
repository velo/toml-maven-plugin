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

import org.apache.maven.model.Resource;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.util.List;

@Mojo(name = "test-generate", defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES)
public class TestGeneratorMojo extends AbstractGeneratorMojo {

    @Parameter(defaultValue = "${project.testResources}", readonly = true)
    private List<Resource> testResources;

    @Parameter(defaultValue = "${project.build.directory}/generated-test-sources/toml", property = "toml.testOutputDirectory")
    private File testOutputDirectory;

    protected void addToSources() {
        project.addTestCompileSourceRoot(outputDirectory().getAbsolutePath());
    }

    protected List<Resource> resources() {
        return testResources;
    }

    public File outputDirectory() {
        return testOutputDirectory;
    }

}
