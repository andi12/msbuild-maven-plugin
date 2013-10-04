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

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.apache.maven.plugin.logging.Log;

/**
 * 
 */
final class LoggingHandler extends Handler 
{
    /**
     * @param klass
     */
    public LoggingHandler( Class<?> klass ) 
    {
        javaLogger = Logger.getLogger( klass.getName() );
        javaLogger.addHandler( this );
    }
    
    public void setMavenLogger( Log mavenLogger )
    {
        this.mavenLogger = mavenLogger;
        
        if ( mavenLogger.isDebugEnabled() ) 
        {
            javaLogger.setLevel( Level.FINE );
        } 
        else if ( mavenLogger.isInfoEnabled() ) 
        {
            javaLogger.setLevel( Level.INFO );
        } 
        else if ( mavenLogger.isWarnEnabled() ) 
        {
            javaLogger.setLevel( Level.WARNING );
        } 
        else 
        {
            javaLogger.setLevel( Level.SEVERE );
        }
    }

    @Override
    public void publish( LogRecord record ) 
    {
        int level = record.getLevel().intValue();

        if ( level <= Level.FINE.intValue() ) 
        {
            mavenLogger.debug( record.getMessage() );
        } 
        else if ( level <= Level.INFO.intValue() ) 
        {
            mavenLogger.info( record.getMessage() );
        } 
        else if ( level <= Level.WARNING.intValue() ) 
        {
            mavenLogger.warn( record.getMessage() );
        } 
        else 
        {
            mavenLogger.error( record.getMessage() );
        }
    }

    @Override
    public void flush() 
    {
    }

    @Override
    public void close() 
    {
    }
    
    private Log mavenLogger;
    private Logger javaLogger;
}
