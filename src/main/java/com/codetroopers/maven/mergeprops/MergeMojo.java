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

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.AbstractMojoExecutionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Merges multiple properties files into one.
 *
 * @author <a href="mailto:cedric@gatay.fr">cgatay</a>
 * @goal merge
 * @phase generate-resources
 * @requiresProject
 */
public class MergeMojo extends AbstractMojo {
    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The directory to place generated files. Don't forget the last slash !
     *
     * @parameter default-value="${project.build.directory}/generated-resources/i18n"
     */
    private File outputDirectory;
    /**
     * The directory to scan for files
     * @parameter default-value="${project.basedir}"
     */
    private File directory;
    /**
     * The properties files to merge.
     * For example :
     * <pre>
     *     <merges>
     *       <merge>
     *         <target>l10n.properties</target>
     *         <pattern>src/main/resources/*.l10n*.properties</pattern>
     *         <excludeKeyCheck>
     *           <excludeKeyCheck>language</excludeKeyCheck>
     *           <excludeKeyCheck>GENERAL</excludeKeyCheck>
     *         </excludeKeyCheck>
     *       </merge>
     *      </merges>
     * </pre>
     *
     * @parameter
     * @required
     */
    private Merge[] merges;

    /**
     * @see org.apache.maven.plugin.AbstractMojo#execute()
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final String resourcePath = attachResourcePathToBuild();
        List<AbstractMojoExecutionException> exceptions = new ArrayList<AbstractMojoExecutionException>();
        for (Merge merge : merges) {
            try {
                new MergeProperty(merge, resourcePath, directory, getLog()).merge();
            } catch (AbstractMojoExecutionException e) {
                exceptions.add(e);
            }
        }
        if (!exceptions.isEmpty()){
            for (AbstractMojoExecutionException exception : exceptions) {
                getLog().error(exception.getMessage());
                getLog().error(exception.getLongMessage());
            }
            throw new MojoFailureException("Unable to merge properties, please check the logs");
        }
    }

    private String attachResourcePathToBuild() {
        final String resourcePath = outputDirectory.getPath();
        if (!containsDirectory(project.getResources(), resourcePath)) {
            Resource resource = new Resource();

            resource.setDirectory(resourcePath);
            resource.addExclude("**/*.java");
            project.getResources().add(resource);
        }
        return resourcePath;
    }

    /**
     * Gets whether the specified list of resources contains a resource with the specified directory.
     *
     * @param resources the list of resources to examine
     * @param directory the resource directory to look for
     * @return {@code true} if the list of resources contains a resource with the specified directory
     */
    public static boolean containsDirectory(List<Resource> resources, String directory) {
        for (Resource resource : resources) {
            if (directory.equals(resource.getDirectory())) {
                return true;
            }
        }
        return false;
    }
}
