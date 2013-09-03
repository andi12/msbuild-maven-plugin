package uk.org.raje.maven.plugin.msbuild;
/*
 * Copyright 2013 Andrew Everitt, Andrew Heckford, Daniele Daniele
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

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;

public class MSBuildMojoConfigurationTest extends AbstractMojoTestCase {

    public final void testMissingMSBuildConfiguration() throws Exception {
        File pom = getTestFile(
                "src/test/resources/unit/configurations/no-msbuild-pom.xml");
        assertNotNull(pom);
        assertTrue(pom.exists());

        MSBuildMojo msbuildMojo = (MSBuildMojo) lookupMojo("msbuild", pom);
        assertNotNull(msbuildMojo);
        try {
            msbuildMojo.execute();
            fail();
        } catch (MojoExecutionException mee) {
            assertEquals("MSBuild could not be found. You need to configure it "
                    + "in the plugin configuration section in the pom file "
                    + "using <msbuild.path>...</msbuild.path> or "
                    + "<properties><msbuild.path>...</msbuild.path></properties> "
                    + "or on command-line using -Dmsbuild.path=... or by setting "
                    + "the environment variable MSBUILD_PATH", mee.getMessage());
        }
    }

    public final void testMissingProjectConfiguration() throws Exception {
        File pom = getTestFile(
                "src/test/resources/unit/configurations/missing-project-pom.xml");
        assertNotNull(pom);
        assertTrue(pom.exists());

        MSBuildMojo msbuildMojo = (MSBuildMojo) lookupMojo("msbuild", pom);
        assertNotNull(msbuildMojo);
        try {
            msbuildMojo.execute();
            fail();
        } catch (MojoExecutionException mee) {
            assertEquals("Missing projectFile, please check your configuration",
                    mee.getMessage());
        }
    }

    public final void testMinimalConfiguration() throws Exception {
        File pom = getTestFile(
                "src/test/resources/unit/configurations/minimal-pom.xml");
        assertNotNull(pom);
        assertTrue(pom.exists());

        MSBuildMojo msbuildMojo = (MSBuildMojo) lookupMojo("msbuild", pom);
        assertNotNull(msbuildMojo);
        msbuildMojo.execute();
    }

    public final void testPlatformsConfiguration() throws Exception {
        File pom = getTestFile(
                "src/test/resources/unit/configurations/platforms-pom.xml");
        assertNotNull(pom);
        assertTrue(pom.exists());

        MSBuildMojo msbuildMojo = (MSBuildMojo) lookupMojo("msbuild", pom);
        assertNotNull(msbuildMojo);
        //assertEquals(String[]("Win32"), msbuildMojo.platforms);
        msbuildMojo.execute();

    }

    public final void testConfigurationsConfiguration() throws Exception {
        File pom = getTestFile(
                "src/test/resources/unit/configurations/configurations-pom.xml");
        assertNotNull(pom);
        assertTrue(pom.exists());

        MSBuildMojo msbuildMojo = (MSBuildMojo) lookupMojo("msbuild", pom);
        assertNotNull(msbuildMojo);
        //assertEquals(String[]("Win32"), msbuildMojo.platforms);
        msbuildMojo.execute();

    }
}
