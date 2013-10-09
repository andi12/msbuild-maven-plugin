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
package uk.org.raje.maven.plugin.msbuild.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Calendar;

import org.junit.Test;

/**
 * Unit tests for VersionInfo Mojo configuration holder.
 */
public class VersionInfoConfigurationTest
{
    /**
     * No arg constructor should create a configuration with no company name 
     */
    @Test
    public void testNoargConstrutor()
    {
        VersionInfoConfiguration v = new VersionInfoConfiguration();
        assertEquals( false, v.skip() );
        assertNull( v.getCompanyName() );
        assertEquals( "Copyright (c) " + Calendar.getInstance().get( Calendar.YEAR ) + " null", v.getCopyright() );
    }

    /**
     * Test default copyright string building
     * @throws NoSuchFieldException if reflection to access private members fails
     * @throws IllegalAccessException if reflection to access private members fails
     */
    @Test
    public void testDefaultCopyright() throws NoSuchFieldException, IllegalAccessException
    {
        VersionInfoConfiguration v = new VersionInfoConfiguration();
        
        // Use reflection to set the private member
        Field companyNameField = VersionInfoConfiguration.class.getDeclaredField( "companyName" );
        companyNameField.setAccessible( true );
        companyNameField.set( v, "Test Org" );

        assertEquals( false, v.skip() );
        assertEquals( "Test Org",  v.getCompanyName() );
        assertEquals( "Copyright (c) " + Calendar.getInstance().get( Calendar.YEAR ) + " Test Org", v.getCopyright() );
    }

    /**
     * Test setting copyright string
     * @throws NoSuchFieldException if reflection to access private members fails
     * @throws IllegalAccessException if reflection to access private members fails
     */
    @Test
    public void testCopyright() throws NoSuchFieldException, IllegalAccessException
    {
        VersionInfoConfiguration v = new VersionInfoConfiguration();
        
        // Use reflection to set the private member
        Field companyNameField = VersionInfoConfiguration.class.getDeclaredField( "companyName" );
        companyNameField.setAccessible( true );
        companyNameField.set( v, "Test Org" );
        Field copyrightField = VersionInfoConfiguration.class.getDeclaredField( "copyright" );
        copyrightField.setAccessible( true );
        copyrightField.set( v, "Custom copyright" );

        assertEquals( false, v.skip() );
        assertEquals( "Test Org",  v.getCompanyName() );
        assertEquals( "Custom copyright", v.getCopyright() );
    }

    /**
     * Test setting the template file 
     * @throws NoSuchFieldException if reflection to access private members fails
     * @throws IllegalAccessException if reflection to access private members fails
     */
    @Test
    public void testTemplate() throws NoSuchFieldException, IllegalAccessException
    {
        VersionInfoConfiguration v = new VersionInfoConfiguration();

        // Use reflection to set the private member
        Field companyNameField = VersionInfoConfiguration.class.getDeclaredField( "template" );
        companyNameField.setAccessible( true );
        companyNameField.set( v, new File( "test-template.rc" ) );

        assertEquals( false, v.skip() );
        assertNull( v.getCompanyName() );
        assertEquals( new File( "test-template.rc" ), v.getTemplate() );
    }

    /**
     * Test default output file
     */
    @Test
    public void testDefaultOutputFile()
    {
        VersionInfoConfiguration v = new VersionInfoConfiguration();

        assertEquals( false, v.skip() );
        assertNull( v.getCompanyName() );
        assertEquals( new File( "maven-version-info.rc" ), v.getOutputFile() );
    }

    /**
     * Test setting the output file 
     * @throws NoSuchFieldException if reflection to access private members fails
     * @throws IllegalAccessException if reflection to access private members fails
     */
    @Test
    public void testOutputFile() throws NoSuchFieldException, IllegalAccessException
    {
        VersionInfoConfiguration v = new VersionInfoConfiguration();

        // Use reflection to set the private member
        Field companyNameField = VersionInfoConfiguration.class.getDeclaredField( "outputFile" );
        companyNameField.setAccessible( true );
        companyNameField.set( v, new File( "my-version-info.rc" ) );

        assertEquals( false, v.skip() );
        assertNull( v.getCompanyName() );
        assertEquals( new File( "my-version-info.rc" ), v.getOutputFile() );
    }
}
