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

import com.google.common.base.CaseFormat;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
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
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class AbstractGeneratorMojo extends AbstractMojo implements ConfigSource {

    @Component
    private BuildContext buildContext;

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${basedir}", readonly = true)
    private File basedir;

    @Parameter(defaultValue = "com.marvinformatics.toml", property = "toml.package")
    private String packageName;

    @Parameter(property = "toml.sources")
    private List<File> sourceDirectories;

    @Parameter(defaultValue = "**/*.toml", property = "toml.includes")
    private String[] includes;

    @Parameter(property = "toml.excludes")
    private String[] excludes;

    @Parameter(defaultValue = "false", property = "toml.skip")
    private boolean skip;

    @Parameter(defaultValue = "", property = "toml.classPrefix")
    private String classPrefix;

    @Parameter(defaultValue = "", property = "toml.classSuffix")
    private String classSuffix;

    @Parameter(defaultValue = "${project.build.sourceEncoding}", property = "toml.encoding")
    private String encoding;

    @Parameter(defaultValue = "LOWER_UNDERSCORE", property = "toml.tableCase")
    private String tableCase;

    @Parameter(defaultValue = "LOWER_UNDERSCORE", property = "toml.fieldCase")
    private String fieldCase;

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
                        packageName(),
                        toml,
                        file -> buildContext.newFileOutputStream(file),
                        this).generate();
            } catch (IOException e) {
                throw new MojoExecutionException("Unable to generate java sources for: " + tomlFile, e);
            }
        }

        addToSources();

    }

    protected abstract void addToSources();

    protected abstract List<Resource> resources();

    @Override
    public String classPrefix() {
        return Optional.ofNullable(classPrefix)
                .orElse("");
    }

    public String packageName() {
        return Optional.ofNullable(packageName)
                .filter(string -> !Strings.isNullOrEmpty(string))
                .orElse("com.marvinformatics.toml");
    }

    @Override
    public String classSuffix() {
        return Optional.ofNullable(classSuffix)
                .orElse("");
    }

    @Override
    public Charset encoding() {
        return Optional.ofNullable(encoding)
                .filter(string -> !Strings.isNullOrEmpty(string))
                .map(encoding -> Charset.forName(encoding))
                .orElse(Charsets.UTF_8);
    }

    @Override
    public CaseFormat tableCase(CaseFormat tableCase) {
        return caseOf(this.tableCase, tableCase);
    }

    @Override
    public CaseFormat fieldCase(CaseFormat fieldCase) {
        return caseOf(this.fieldCase, fieldCase);
    }

    private CaseFormat caseOf(String caseFormat, CaseFormat defaultCaseFormat) {
        return Optional.ofNullable(caseFormat)
                .filter(string -> !Strings.isNullOrEmpty(string))
                .map(value -> CaseFormat.valueOf(value))
                .orElse(defaultCaseFormat);
    }

}
