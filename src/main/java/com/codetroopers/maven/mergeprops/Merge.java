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

import org.codehaus.plexus.util.DirectoryScanner;

import java.io.File;
import java.util.Arrays;
import java.util.List;


/**
 * The POJO to hold the merging configuration.
 *
 * @author <a href="mailto:cedric@gatay.fr">cgatay</a>
 */
public class Merge {
    /**
     * The target file (locale prefix will automatically be appended before the last dot).
     *
     * @parameter
     * @required
     */
    private String target;

    /**
     * The pattern to include in the merge
     * @parameter
     * @required
     */
    private String pattern;

    /**
     * Whether the build should fail when the count between files does not match
     * @parameter
     */
    private Boolean failOnCountMismatch = true;

    /**
     * The name of the file to exclude from key checking
     * @parameter
     */
    private String[] excludeKeyCheck;

    /**
     * Returns the target file where the result of the merging should be saved.
     *
     * @return The target file.
     */
    public String getTarget() {
        return target;
    }

    public Boolean getFailOnCountMismatch() {
        return failOnCountMismatch;
    }

    /**
     * Returns the files that are to be merged.
     *
     * @return The files to merge.
     * @param directory
     */
    public List<String> getFileNames(final File directory) {
        return scanDirectory(directory);
    }

    private List<String> scanDirectory(final File directory) {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(directory);
        if (pattern != null) {
            scanner.setIncludes(new String[]{pattern});
        }
        scanner.scan();

        return Arrays.asList(scanner.getIncludedFiles());
    }

    public String[] getExcludeKeyCheck() {
        return excludeKeyCheck;
    }
}
