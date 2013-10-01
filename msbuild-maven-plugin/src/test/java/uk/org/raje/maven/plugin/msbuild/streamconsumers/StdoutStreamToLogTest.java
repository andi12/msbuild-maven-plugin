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
package uk.org.raje.maven.plugin.msbuild.streamconsumers;

import static org.junit.Assert.assertEquals;

import java.util.LinkedList;
import java.util.List;

import org.apache.maven.plugin.logging.Log;
import org.junit.Before;
import org.junit.Test;

/**
 * Test that the stream consumer sets appropriate log levels. 
 */
public class StdoutStreamToLogTest
{
    @Before
    public void setupLogger()
    {
        log = new TestLog();
        stdoutStreamToLog = new StdoutStreamToLog( log );
    }

    @Test
    public void errorMessages() throws Exception
    {
        String[] testData = new String[] {
                "error: message",
                "Error: message",
                "ERROR: message",
                "\t1>hello-world.cpp(7): error C2143: syntax error : missing ';' before 'return' "
                        + "[C:\\foo\\bar\\baz\\hello-world\\hello-world.vcxproj]",
                "c1xx : fatal error C1083: Cannot open source file: 'hello-world.cpp': "
                        + "No such file or directory [C:\\foo\\bar\\baz\\hello-world\\hello-world.vcxproj]"
        };
        testMessages( testData, log.errorMessages );
        assertEquals( 0, log.warnMessages.size() );
        assertEquals( 0, log.infoMessages.size() );
        assertEquals( 0, log.debugMessages.size() );
    }

    @Test
    public void notErrorMessages() throws Exception
    {
        String[] testData = new String[] {
                "error.cpp",
                "Creating library Release\\error.lib and object Release\\error.exp"
        };
        testMessages( testData, log.infoMessages );
        assertEquals( 0, log.warnMessages.size() );
        assertEquals( 0, log.errorMessages.size() );
        assertEquals( 0, log.debugMessages.size() );
    }

    @Test
    public void warningMessage() throws Exception
    {
        String[] testData = new String[] {
                "warning: message",
                "Warning: message",
                "WARNING: message",
                "hello-world.cpp(10): warning C4129: 'T' : unrecognized character escape sequence "
                        + "[C:\\foo\\bar\\baz\\hello-world\\hello-world.vcxproj]",
                "C:\\foo\\bar\\Dummy.h(5): warning C4005: 'macroAgain' : macro redefinition "
                        + "[C:\\foo\\bar\\baz\\hello-world\\hello-world.vcxproj]",
                "libbarMT.lib(hello-wrold.obj) : warning LNK4099: PDB 'vc100.pdb' was not found with "
                        + "'libbarMT.lib(hello-wrold.obj)' or at 'C:\\foo\\bar\\baz\\hello-world\\Debug\\vc100.pdb'; "
                        + "linking object as if no debug info [C:\\foo\\bar\\baz\\hello-world\\hello-world.vcxproj]"
        };
        testMessages( testData, log.warnMessages );
        assertEquals( 0, log.errorMessages.size() );
        assertEquals( 0, log.infoMessages.size() );
        assertEquals( 0, log.debugMessages.size() );
    }

    @Test
    public void notWarningMessages() throws Exception
    {
        String[] testData = new String[] {
                "warning.cpp",
                "custom-warning-test.cpp",
                "Creating library Release\\warning.lib and object Release\\warning.exp"
        };
        testMessages( testData, log.infoMessages );
        assertEquals( 0, log.warnMessages.size() );
        assertEquals( 0, log.errorMessages.size() );
        assertEquals( 0, log.debugMessages.size() );
    }

    private void testMessages( String[] testData, List<CharSequence> messages )
    {
        for ( int i = 0; i < testData.length; i++ )
        {
            stdoutStreamToLog.consumeLine( testData[i] );
            assertEquals( i + 1, messages.size() );
        }
    }

    private TestLog log;
    private StdoutStreamToLog stdoutStreamToLog;
    
    class TestLog implements Log
    {
        @Override
        public boolean isDebugEnabled()
        {
            return true;
        }

        @Override
        public void debug( CharSequence content )
        {
            debugMessages.add( content );
        }

        @Override
        public void debug( CharSequence content, Throwable error )
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void debug( Throwable error )
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isInfoEnabled()
        {
            return true;
        }

        @Override
        public void info( CharSequence content )
        {
            infoMessages.add( content );
        }

        @Override
        public void info( CharSequence content, Throwable error )
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void info( Throwable error )
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isWarnEnabled()
        {
            return true;
        }

        @Override
        public void warn( CharSequence content )
        {
            warnMessages.add( content );
        }

        @Override
        public void warn( CharSequence content, Throwable error )
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void warn( Throwable error )
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isErrorEnabled()
        {
            return true;
        }

        @Override
        public void error( CharSequence content )
        {
            errorMessages.add( content );
        }

        @Override
        public void error( CharSequence content, Throwable error )
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void error( Throwable error )
        {
            throw new UnsupportedOperationException();
        }
        
        List<CharSequence> errorMessages = new LinkedList<CharSequence>();
        List<CharSequence> warnMessages = new LinkedList<CharSequence>();
        List<CharSequence> infoMessages = new LinkedList<CharSequence>();
        List<CharSequence> debugMessages = new LinkedList<CharSequence>();
    }
}
