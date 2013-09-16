/*
 * Copyright 2013 Andrew Everitt, Andrew Heckford, Daniele Masato
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.org.raje.maven.plugin.msbuild;

import java.io.File;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import uk.org.raje.maven.plugin.msbuild.configuration.BuildPlatform;
import uk.org.raje.maven.plugin.msbuild.configuration.CppCheckConfiguration;
import uk.org.raje.maven.plugin.msbuild.configuration.CxxTestConfiguration;

/**
 * Abstract base class for the msbuild-maven-plugin which defines all configuration properties exposed.
 */
public abstract class AbstractMSBuildPluginMojo extends AbstractMojo
{
    /**
     * The MavenProject for the current build.
     */
    @Parameter( defaultValue = "${project}" )
    protected MavenProject mavenProject;

    /**
     * The project (.vcxproj) or solution (.sln) file to build.
     */
    @Parameter(
            readonly = false,
            required = true )
    protected File projectFile;

    /**
     * The set of platforms and configurations to build.
     */
    @Parameter(
            readonly = false,
            required = false )
    protected List<BuildPlatform> platforms;

    /**
     * The set of targets to build.
     */
    @Parameter(
            readonly = false,
            required = false )
    protected List<String> targets;

    /**
     * The path to MSBuild.
     */
    @Parameter(
            property = "msbuild.path",
            readonly = false,
            required = true )
    protected File msbuildPath;

    @Parameter
    protected CppCheckConfiguration cppCheck = new CppCheckConfiguration();
    
    @Parameter
    protected CxxTestConfiguration cxxTest = new CxxTestConfiguration();
}
