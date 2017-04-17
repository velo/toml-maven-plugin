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

import com.moandjiezana.toml.Toml;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.sonatype.plexus.build.incremental.BuildContext;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractGeneratorMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${basedir}", readonly = true)
    private File basedir;

    @Parameter(defaultValue = "com.marvinformatics.toml", property = "package")
    private String packageName;

    @Parameter(property = "toml.sources")
    private List<File> sourceDirectories;

    @Parameter(defaultValue = "**/*.toml", property = "toml.includes")
    private String[] includes;

    @Parameter(property = "toml.excludes")
    private String[] excludes;

    @Parameter(defaultValue = "false", property = "toml.skip")
    private boolean skip;

    @Component
    private BuildContext buildContext;

    public AbstractGeneratorMojo() {
        super();
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping!");
            return;
        }

        if (sourceDirectories == null || sourceDirectories.isEmpty()) {
            sourceDirectories = resources().stream()
                    .map(resource -> resource.getDirectory())
                    .map(path -> new File(path))
                    .collect(Collectors.toList());
        }

        getLog().debug("Looking for: " + Arrays.toString(includes));
        getLog().debug("Excluding: " + Arrays.toString(excludes));

        List<File> files = sourceDirectories.stream()
                .peek(directory -> getLog().debug("Scanning: " + directory.getAbsolutePath()))
                .flatMap(directory -> {
                    DirectoryScanner scanner = new DirectoryScanner();
                    scanner.setIncludes(includes);
                    scanner.setExcludes(excludes);
                    scanner.addDefaultExcludes();
                    scanner.setBasedir(directory);
                    scanner.scan();
                    getLog().debug(Arrays.toString(scanner.getExcludedFiles()));
                    return Arrays.asList(scanner.getIncludedFiles()).stream()
                            .map(file -> new File(directory, file));
                })
                .filter(file -> buildContext.hasDelta(file))
                .peek(file -> getLog().debug("Toml file: " + file.getAbsolutePath()))
                .collect(Collectors.toList());

        for (File tomlFile : files) {
            Toml toml;
            try {
                toml = new Toml().read(tomlFile);
            } catch (Exception e) {
                throw new MojoExecutionException("Unable to parse toml file: " + tomlFile, e);
            }

            try {
                new Generator(FileUtils.basename(tomlFile.getName()).replaceAll("\\W", ""),
                        packageName,
                        toml,
                        outputDirectory(),
                        file -> buildContext.newFileOutputStream(file))
                                .generate();
            } catch (IOException e) {
                throw new MojoExecutionException("Unable to generate java sources for: " + tomlFile, e);
            }
        }

        addToSources();

    }

    protected abstract void addToSources();

    protected abstract File outputDirectory();

    protected abstract List<Resource> resources();

}
