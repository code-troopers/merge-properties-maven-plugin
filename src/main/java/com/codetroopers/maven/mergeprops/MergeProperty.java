/*
 * Copyright 2013 Code-Troopers.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this work except in compliance with the License.
 * You may obtain a copy of the License in the LICENSE file, or at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codetroopers.maven.mergeprops;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:cedric@gatay.fr">cgatay</a>
 */
public class MergeProperty {
    public static final String ERROR_IGNORE_FLAG = "i18n.error.ignore";
    protected static Pattern prefixPattern = Pattern.compile(".*_(.*)\\..*");
    private final Merge merge;
    private final String resourcePath;
    private final File directory;
    private final Log log;

    public MergeProperty(Merge merge, final String resourcePath, final File directory, final Log log) {
        this.merge = merge;
        this.resourcePath = resourcePath;
        this.directory = directory;
        this.log = log;
    }

    public void merge() throws MojoExecutionException, MojoFailureException {
        final Map<String, Properties> propertiesMap = loadFiles();
        processProperties(propertiesMap, merge, log);
        saveToFile(propertiesMap);
    }

    @VisibleForTesting
    static void processProperties(final Map<String, Properties> propertiesMap, final Merge merge, final Log log)
            throws MojoFailureException {
        Map<String, Integer> localeSizeMap = Maps.newHashMap();
        for (Map.Entry<String, Properties> entry : propertiesMap.entrySet()) {
            List<String> emptyProperties = Lists.newArrayList();
            for (String key : entry.getValue().stringPropertyNames()) {
                final String property = entry.getValue().getProperty(key);
                if (Strings.isNullOrEmpty(property)){
                    emptyProperties.add(key);
                }
                entry.getValue().setProperty(key, property.replaceAll("([^'])'([^'])", "$1''$2"));
            }
            localeSizeMap.put(entry.getKey(), entry.getValue().size());
            if (log != null) {
                if (!emptyProperties.isEmpty()){
                    log.info("Found empty properties for final file " + merge.getTarget() 
                             + " [" + entry.getKey() + "] : \n" 
                             + Joiner.on("\n").join(emptyProperties));
                }
                log.info("=> Final file " + merge.getTarget() + " [" + entry.getKey() + "] contains " +
                         entry.getValue().size() + " entries");
            }
        }
        checkCountMismatch(propertiesMap, merge, localeSizeMap);
    }

    private static void checkCountMismatch(final Map<String, Properties> propertiesMap, final Merge merge,
                                           final Map<String, Integer> localeSizeMap) throws MojoFailureException {
        //if we got the same numbers of keys, the set will flatten every values
        if (shouldFailIfNoMatchFromProperty()
            && merge.getFailOnCountMismatch()
            && new HashSet<Integer>(localeSizeMap.values()).size() != 1){
            
            final HashMultiset<String> multiset = HashMultiset.create();
            for (Map.Entry<String, Properties> entry : propertiesMap.entrySet()) {
                multiset.addAll(Maps.fromProperties(entry.getValue()).keySet());
            }
            final int bundlesAmount = propertiesMap.keySet().size();
            final Set<String> lonelyKeys = Sets.newHashSet(Collections2.filter(multiset, new Predicate<String>() {
                @Override
                public boolean apply(final String input) {
                    return multiset.count(input) != bundlesAmount;
                }
            }));
            throw new MojoFailureException(lonelyKeys, 
                                           "Invalid property count for file : " + merge.getTarget(), 
                                           "Lonely keys are : \n" + Joiner.on("\n").join(lonelyKeys));
        }
    }

    private static Boolean shouldFailIfNoMatchFromProperty() {
        return !Boolean.valueOf(System.getProperty(ERROR_IGNORE_FLAG, "false"));
    }

    private void saveToFile(final Map<String, Properties> mergedProperties) throws MojoExecutionException {
        final String targetPropertiesFileName = merge.getTarget();
        final String suffix = extractFileSuffix(targetPropertiesFileName);
        final String prefix = extractFilePrefix(targetPropertiesFileName);
        File generated = new File(resourcePath);
        if (!generated.exists()) {
            final boolean mkdirs = generated.mkdirs();
            if (!mkdirs) {
                throw new MojoExecutionException("Could not create directory : " + resourcePath);
            }
        }
        for (Map.Entry<String, Properties> propertiesEntry : mergedProperties.entrySet()) {
            File out = new File(resourcePath + File.separator + prefix + "_" + propertiesEntry.getKey() + "." + suffix);
            OutputStream output = null;
            try {

                if (out.exists() && !out.delete()) {
                    throw new MojoExecutionException("Could not remove file: " + out.getAbsolutePath());
                }

                if (!out.createNewFile()) {
                    throw new MojoExecutionException("Could not create file: " + out.getAbsolutePath());
                }

                output = new FileOutputStream(out);
                propertiesEntry.getValue().store(output, out.getName());
            } catch (FileNotFoundException e) {
                throw new MojoExecutionException("Could not find file: " + out.getAbsolutePath(), e);
            } catch (IOException e) {
                throw new MojoExecutionException("Could not write to file: " + out.getAbsolutePath(), e);
            } finally {
                if (output != null) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        // no can do
                    }
                }
            }
        }

    }

    private Map<String, Properties> loadFiles() throws MojoExecutionException, MojoFailureException {
        Map<String, Properties> outMap = new LinkedHashMap<String, Properties>();
        for (String propertyFileName : merge.getFileNames(directory)) {
            InputStream input = null;
            try {
                String locale = extractLocaleFromFileName(propertyFileName);
                final String fullFilePath = directory + File.separator + propertyFileName;
                input = new FileInputStream(fullFilePath);
                final boolean b = checkKeys(propertyFileName, merge.getExcludeKeyCheck(), fullFilePath, log);

                if (log.isDebugEnabled()) {
                    log.debug("Should include properties : " + b);
                }
                if (b) {
                    getMergedPropertiesForLocale(outMap, locale).load(input);
                }
            } catch (FileNotFoundException e) {
                throw new MojoExecutionException("Could not find file: " + propertyFileName, e);
            } catch (IOException e) {
                throw new MojoExecutionException("Could not read from file: " + propertyFileName, e);
            } finally {
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException e) {
                        // nothing to do
                    }
                }
            }
        }
        return outMap;
    }

    private Properties getMergedPropertiesForLocale(final Map<String, Properties> outMap, final String locale) {
        if (!outMap.containsKey(locale)) {
            outMap.put(locale, new Properties());
        }
        return outMap.get(locale);
    }

    static String extractLocaleFromFileName(final String propertyFileName) {
        final Pattern compile = MergeProperty.prefixPattern;
        final Matcher matcher = compile.matcher(propertyFileName);
        if (matcher.matches()) {
            if (matcher.groupCount() > 0) {
                return matcher.group(1);
            }
        }
        return "";
    }

    static boolean checkKeys(final String propertyFileName, final String[] excludeKeyCheck,
                             final String fileName, final Log log) throws MojoExecutionException, MojoFailureException {
        if (propertyFileName == null) {
            throw new NullPointerException("PropertyFileName can not be null ! ");
        }
        // Do not reuse the existing inputstream as the load method forwards it and there is no reset() in FIS
        String prefixToConsider = extractFilePrefix(propertyFileName);
        if (log.isDebugEnabled()) {
            log.debug("Prefix to consider : " + prefixToConsider);
        }
        if (excludeKeyCheck != null) {
            for (String s : excludeKeyCheck) {
                if (prefixToConsider.matches(s)) {
                    log.info("Found propertyFileName without prefix checking, including... [" + propertyFileName +
                             "]");
                    return true;
                }
            }
        }
        Properties props = new Properties();
        InputStream input = null;
        try {
            input = new FileInputStream(fileName);
            props.load(input);
            return !containsInvalidPrefix(prefixToConsider, props);
        } catch (IOException e) {
            throw new MojoExecutionException("Could not read from file: " + propertyFileName, e);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ioe) {
                    //do nothing
                }
            }
        }
    }

    static boolean containsInvalidPrefix(final String prefixToConsider, final Properties props)
            throws MojoFailureException {
        final String prefix = prefixToConsider + ".";
        for (String s : props.stringPropertyNames()) {
            if (!s.startsWith(prefix)) {
                throw new MojoFailureException(
                        "An invalid property key has been found [" + s + " not beginning with " + prefix + "]");
            }
        }
        return false;
    }

    static String extractFilePrefix(final String s) {
        //Pattern.quote handles escaping special chars (windows path separator)
        final String[] splittedPath = s.split(Pattern.quote(File.separator));
        return splittedPath[splittedPath.length - 1].split("[\\._]")[0];
    }

    static String extractFileSuffix(final String s) {
        final String[] split = s.split("[\\._]");
        return split[split.length - 1];
    }
}
