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

import java.io.IOException;
import java.io.Writer;

import org.codehaus.plexus.util.cli.StreamConsumer;

/**
 * Stream consumer to write to the supplied Writer
 */
public class StreamToWriter implements StreamConsumer
{

    public StreamToWriter( Writer writer )
    {
        this.writer = writer;
    }

    @Override
    public void consumeLine ( String line )
    {
        try
        {
            writer.write( line );
        }
        catch ( IOException ioe )
        {
            ioe.printStackTrace();
        }
    }

    private Writer writer;
}
