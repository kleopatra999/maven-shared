package org.apache.maven.shared.jarsigner;

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

import junit.framework.TestCase;
import org.apache.maven.shared.utils.io.FileUtils;

import java.io.File;

/**
 * Created on 11/8/13.
 *
 * @author Tony Chemit <chemit@codelutin.com>
 * @version $Id$
 * @since 1.1
 */
public class JarSignerUtilTest
    extends TestCase
{

    // Fix MSHARED-277
    public void testUnsignArchive()
        throws Exception
    {

        File file = new File( "src/test/javax.persistence_2.0.5.v201212031355.jar" );
        File target = new File( "target/", file.getName() );

        if ( target.exists() )
        {
            FileUtils.forceDelete( target );
        }

        FileUtils.copyFile( file, target );

        assertTrue( JarSignerUtil.isArchiveSigned( target ) );

        JarSignerUtil.unsignArchive( target );

        assertFalse( JarSignerUtil.isArchiveSigned( target ) );

    }
}
