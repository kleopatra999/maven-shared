package org.apache.maven.shared.utils.cli;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import org.apache.maven.shared.utils.Os;

import junit.framework.TestCase;

public class CommandLineUtilsTest
    extends TestCase
{

    /**
     * Tests that case-insensitive environment variables are normalized to upper case.
     */
    public void testGetSystemEnvVarsCaseInsensitive()
    {
        Properties vars = CommandLineUtils.getSystemEnvVars( false );
        for ( Object o : vars.keySet() )
        {
            String variable = (String) o;
            assertEquals( variable.toUpperCase( Locale.ENGLISH ), variable );
        }
    }

    public void testEnsureCaseSensitivity()
        throws Exception
    {
        Map<String, String> data = new HashMap<String, String>();
        data.put( "abz", "cool" );
        assertTrue( CommandLineUtils.ensureCaseSensitivity( data, false ).containsKey( "ABZ" ) );
        assertTrue( CommandLineUtils.ensureCaseSensitivity( data, true ).containsKey( "abz" ) );
    }

    /**
     * Tests that environment variables on Windows are normalized to upper case. Does nothing on Unix platforms.
     */
    public void testGetSystemEnvVarsWindows()
        throws Exception
    {
        if ( !Os.isFamily( Os.FAMILY_WINDOWS ) )
        {
            return;
        }
        Properties vars = CommandLineUtils.getSystemEnvVars();
        for ( Object o : vars.keySet() )
        {
            String variable = (String) o;
            assertEquals( variable.toUpperCase( Locale.ENGLISH ), variable );
        }
    }

    /**
     * Tests the splitting of a command line into distinct arguments.
     */
    public void testTranslateCommandline()
        throws Exception
    {
        assertCmdLineArgs( new String[] {}, null );
        assertCmdLineArgs( new String[] {}, "" );

        assertCmdLineArgs( new String[] { "foo", "bar" }, "foo bar" );
        assertCmdLineArgs( new String[] { "foo", "bar" }, "   foo   bar   " );

        assertCmdLineArgs( new String[] { "foo", " double quotes ", "bar" }, "foo \" double quotes \" bar" );
        assertCmdLineArgs( new String[] { "foo", " single quotes ", "bar" }, "foo ' single quotes ' bar" );

        assertCmdLineArgs( new String[] { "foo", " \" ", "bar" }, "foo ' \" ' bar" );
        assertCmdLineArgs( new String[] { "foo", " ' ", "bar" }, "foo \" ' \" bar" );
    }

    private void assertCmdLineArgs( String[] expected, String cmdLine )
        throws Exception
    {
        String[] actual = CommandLineUtils.translateCommandline( cmdLine );
        assertNotNull( actual );
        assertEquals( expected.length, actual.length );
        assertEquals( Arrays.asList( expected ), Arrays.asList( actual ) );
    }

}
