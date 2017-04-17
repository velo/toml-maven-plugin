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

import static com.google.common.base.Charsets.UTF_8;

import com.google.common.base.CaseFormat;
import com.google.common.base.Strings;
import com.moandjiezana.toml.Toml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

public class Generator {

    @FunctionalInterface
    public static interface OutputCreator {

        OutputStream openStream(File file) throws IOException;

    }

    private static final Logger log = LoggerFactory.getLogger(Generator.class);

    private static final List<String> JAVA_KEYWORDS = Arrays.asList("abstract", "assert", "boolean",
            "break", "byte", "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else", "extends", "false",
            "final", "finally", "float", "for", "goto", "if", "implements",
            "import", "instanceof", "int", "interface", "long", "native",
            "new", "null", "package", "private", "protected", "public",
            "return", "short", "static", "strictfp", "super", "switch",
            "synchronized", "this", "throw", "throws", "transient", "true",
            "try", "void", "volatile", "while");

    private final String fileName;

    private final String packageName;

    private final Toml toml;

    private final File outputDirectory;

    private final OutputCreator fileStreamCreator;

    public Generator(String fileName, String packageName, Toml toml, File outputDirectory,
            OutputCreator fileStreamCreator) {
        super();
        this.fileName = fileName;
        this.packageName = packageName;
        this.toml = toml;
        this.outputDirectory = outputDirectory;
        this.fileStreamCreator = fileStreamCreator;
    }

    public void generate() throws IOException {
        final File destination = new File(outputDirectory, packageName.replace(".", "/"));

        destination.mkdirs();

        String className = asClassName(fileName);
        log.info("Generating source for " + className);

        File targetFile = new File(destination, className + ".java");
        try (OutputStream fileStrean = fileStreamCreator.openStream(targetFile);
                PrintWriter pw = new PrintWriter(new OutputStreamWriter(fileStrean, UTF_8));) {
            log.info("Writting {}.{} at {} ", packageName, className, targetFile.getAbsolutePath());

            pw.printf("package %s;", packageName);
            pw.println();

            pw.printf("public class %s {\n", className);
            pw.println();

            pw.printf("  private final com.moandjiezana.toml.Toml toml;\n");
            pw.println();

            pw.printf("  public %s (com.moandjiezana.toml.Toml toml) {\n", className);
            pw.printf("    this.toml = toml;\n", className);
            pw.printf("  }\n", className);
            pw.println();

            pw.printf("  public %s (com.moandjiezana.toml.Toml toml, %s defaultValue) {\n", className, className);
            pw.printf("    if (toml == null) {\n", className);
            pw.printf("      this.toml = defaultValue.toml;\n", className);
            pw.printf("    } else {\n", className);
            pw.printf("      this.toml = toml;\n", className);
            pw.printf("    }\n", className);
            pw.printf("  }\n", className);
            pw.println();

            Set<Entry<String, Object>> entries = toml.entrySet();
            for (Entry<String, Object> entry : entries) {
                String fieldName = wrapReservedWords(entry.getKey());
                Object value = entry.getValue();

                String type = type(fieldName, value, false);
                log.debug("Adding field {}:{}", fieldName, type);
                pw.printf("  public %s %s(){\n", type, fieldName.replaceAll("\\W", ""));
                pw.printf("    return %s;\n", accessor(fieldName, value));
                pw.printf("  }\n");

                pw.printf("  public %s %s(%s defaultValue){\n", type, fieldName.replaceAll("\\W", ""),
                        type(fieldName, value, true));
                pw.printf("    return %s;\n", defaultValueAccessor(fieldName, value));
                pw.printf("  }\n");
                pw.println();
            }

            pw.printf("}\n", className);
        }
    }

    private String wrapReservedWords(String key) {
        if (JAVA_KEYWORDS.contains(key))
            return key + "_f";
        return key;
    }

    private String accessor(String propertyName, Object value) {
        String fieldName = propertyName.replace("\"", "\\\"");
        if (value instanceof String)
            return String.format("toml.getString(\"%s\")", fieldName);

        if (value instanceof Boolean)
            return String.format("toml.getBoolean(\"%s\")", fieldName);

        if (value instanceof Long)
            return String.format("toml.getLong(\"%s\")", fieldName);

        if (value instanceof Date)
            return String.format("toml.getDate(\"%s\")", fieldName);

        if (value instanceof Double)
            return String.format("toml.getDouble(\"%s\")", fieldName);

        if (value instanceof List)
            return String.format("toml.getList(\"%s\")", fieldName);

        if (value instanceof Toml) {
            String subpackage = appendPackage(packageName, this.fileName);
            return String.format("new %s(toml.getTable(\"%s\"))",
                    subpackage + "." + asClassName(fieldName),
                    fieldName);
        }

        throw new IllegalArgumentException("Unable to handle " + value);
    }

    private String defaultValueAccessor(String propertyName, Object value) {
        String fieldName = propertyName.replace("\"", "\\\"");
        if (value instanceof String)
            return String.format("toml.getString(\"%s\", defaultValue)", fieldName);

        if (value instanceof Boolean)
            return String.format("toml.getBoolean(\"%s\", defaultValue)", fieldName);

        if (value instanceof Long)
            return String.format("toml.getLong(\"%s\", defaultValue)", fieldName);

        if (value instanceof Date)
            return String.format("toml.getDate(\"%s\", defaultValue)", fieldName);

        if (value instanceof Double)
            return String.format("toml.getDouble(\"%s\", defaultValue)", fieldName);

        if (value instanceof List)
            return String.format("toml.getList(\"%s\", defaultValue)", fieldName);

        if (value instanceof Toml) {
            String subpackage = appendPackage(packageName, this.fileName);
            return String.format("new %s(toml.getTable(\"%s\"), defaultValue)",
                    subpackage + "." + asClassName(fieldName),
                    fieldName);
        }

        throw new IllegalArgumentException("Unable to handle " + value);
    }

    private String asClassName(String fileName) {
        return fileCaseFormat(fileName).to(CaseFormat.UPPER_CAMEL, fileName.replaceAll("\\W", ""));
    }

    private String type(String name, Object value, boolean asParameter) throws IOException {
        if (value instanceof com.moandjiezana.toml.Toml) {
            Toml table = (Toml) value;

            String subpackage = appendPackage(packageName, this.fileName);
            new Generator(name, subpackage, table, outputDirectory, fileStreamCreator).generate();
            return subpackage + "." + asClassName(name);
        }

        if (value instanceof List) {
            List<?> content = (List<?>) value;

            Class<?> listType = listType(content);

            if (listType == null)
                return "<T> java.util.List<T>";
            if (List.class.isAssignableFrom(listType))
                if (asParameter)
                    return "java.util.List<java.util.List<T>>";
                else
                    return "<T> java.util.List<java.util.List<T>>";
            else
                return String.format("java.util.List<%s>", listType.getName());
        }
        return value.getClass().getName();
    }

    private Class<?> listType(List<?> content) {
        Set<Class<?>> listTypes = content.stream()
                .map(item -> item.getClass())
                .collect(Collectors.toSet());

        if (listTypes.size() == 1)
            return listTypes.iterator().next();

        log.debug("Unable to determine list type for: {}\n got this classes: {}", content, listTypes);

        return null;
    }

    private String appendPackage(String basePackage, String itemToAppend) {
        if (Strings.isNullOrEmpty(basePackage))
            return itemToAppend.toLowerCase();

        return basePackage + "." + itemToAppend.toLowerCase();
    }

    private CaseFormat fileCaseFormat(String fileName) {
        if (fileName.contains("-"))
            return CaseFormat.LOWER_HYPHEN;
        if (fileName.contains("_"))
            return CaseFormat.LOWER_UNDERSCORE;

        return CaseFormat.LOWER_CAMEL;
    }

}
