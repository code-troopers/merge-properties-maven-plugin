# Merge Properties Maven Plugin [![Build Status](https://buildhive.cloudbees.com/job/code-troopers/job/merge-properties-maven-plugin/badge/icon)](https://buildhive.cloudbees.com/job/code-troopers/job/merge-properties-maven-plugin/)

This project provides a way to merge multiple properties files into one.

It features : 

 * auto-escaping of single quotes (for use with `MessageFormat`)
 * consistency check (warning on empty values, warning on missing keys)
 * property prefix name checking

# Hot to use it

This is package as a Maven Plugin, to use it you will need to add the following to your project pom : 

    <plugin>
        <groupId>com.code-troopers</groupId>
        <artifactId>merge-properties-maven-plugin</artifactId>
        <version>1.0</version>
        <executions>
            <execution>
                <id>merge</id>
                <phase>generate-resources</phase>
                <goals>
                    <goal>merge</goal>
                </goals>
                <configuration>
                    <merges>
                        <merge>
                            <target>language.properties</target>
                            <pattern>src/main/resources/l10n/*.properties</pattern>
                            <excludeKeyCheck>
                                <excludeKeyCheck>FileWhereCheckShouldNotBeDone</excludeKeyCheck>
                            </excludeKeyCheck>
                            <failOnCountMismatch>true</failOnCountMismatch>
                        </merge>
                        <merge>
                            <target>help.properties</target>
                            <pattern>src/main/resources/help/*.properties</pattern>
                            <failOnCountMismatch>false</failOnCountMismatch>
                        </merge>
                    </merges>
                </configuration>
            </execution>
        </executions>
    </plugin>

In this example, the files in `src/main/resources/l10n` will be merged to the `language.properties` file (aggregated by locale). 
The file named `FileWhereCheckShouldNotBeDone.properties` will not have its property keys validated. 
If there is not the same amount of keys in every language bundle, the build will fail (default behavior).

You can bypass the failing build by running maven with the `i18n.error.ignore` flag set : `mvn compile -Di18n.error.ignore=true`
    
# Bug tracker

Have a bug? Please create an issue here on GitHub!

https://github.com/code-troopers/merge-properties-maven-plugin/issues


# Special notes

Thanks to Cloudbees Buildhive for providing a free Jenkins instance.

# Copyright and license

Copyright 2013 Code-Troopers.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this work except in compliance with the License.
You may obtain a copy of the License in the LICENSE file, or at:

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
