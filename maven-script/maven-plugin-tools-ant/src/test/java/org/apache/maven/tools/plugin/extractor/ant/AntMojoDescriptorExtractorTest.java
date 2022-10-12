package org.apache.maven.tools.plugin.extractor.ant;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import junit.framework.TestCase;
import org.apache.maven.plugin.descriptor.InvalidPluginDescriptorException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.path.PathTranslator;
import org.apache.maven.tools.plugin.DefaultPluginToolsRequest;
import org.apache.maven.tools.plugin.PluginToolsRequest;
import org.apache.maven.tools.plugin.extractor.ExtractionException;
import org.codehaus.plexus.component.repository.ComponentRequirement;

public class AntMojoDescriptorExtractorTest
    extends TestCase
{
    
    public void testBasicMojoExtraction_CheckInjectedParametersAndRequirements()
        throws InvalidPluginDescriptorException, ExtractionException
    {
        Map scriptMap = buildTestMap( "basic" );
        
        PluginDescriptor pd = new PluginDescriptor();
        
        pd.setArtifactId( "test-plugin" );
        pd.setGroupId( "org.mytest" );
        pd.setVersion( "1" );
        pd.setGoalPrefix( "mytest" );
        
        PluginToolsRequest request = new DefaultPluginToolsRequest( new MavenProject(), pd );
        
        List metadata = new AntMojoDescriptorExtractor().extractMojoDescriptorsFromMetadata( scriptMap, request );
        
        assertEquals( 2, metadata.size() );
        
        for ( Iterator it = metadata.iterator(); it.hasNext(); )
        {
            MojoDescriptor desc = (MojoDescriptor) it.next();
            
            if ( "test".equals( desc.getGoal() ) )
            {
                assertTrue( desc.getImplementation().indexOf( ":" ) < 0 );
            }
            else if ( "test2".equals( desc.getGoal() ) )
            {
                assertTrue( desc.getImplementation().endsWith( ":test2" ) );
            }
            
            List params = desc.getParameters();
            Map paramMap = new HashMap();
            for ( Iterator paramIterator = params.iterator(); paramIterator.hasNext(); )
            {
                Parameter param = (Parameter) paramIterator.next();
                paramMap.put( param.getName(), param );
            }
            
            assertNotNull( "Mojo descriptor: " + desc.getGoal() + " is missing 'basedir' parameter.", paramMap.get( "basedir" ) );
            assertNotNull( "Mojo descriptor: " + desc.getGoal() + " is missing 'messageLevel' parameter.", paramMap.get( "messageLevel" ) );
            assertNotNull( "Mojo descriptor: " + desc.getGoal() + " is missing 'project' parameter.", paramMap.get( "project" ) );
            assertNotNull( "Mojo descriptor: " + desc.getGoal() + " is missing 'session' parameter.", paramMap.get( "session" ) );
            assertNotNull( "Mojo descriptor: " + desc.getGoal() + " is missing 'mojoExecution' parameter.", paramMap.get( "mojoExecution" ) );
            
            List components = desc.getRequirements();

            assertNotNull( components );
            assertEquals( 1, components.size() );
            
            ComponentRequirement req = (ComponentRequirement) components.get( 0 );
            assertEquals( "Mojo descriptor: " + desc.getGoal() + " is missing 'PathTranslator' component requirement.", PathTranslator.class.getName(), req.getRole() );
        }
    }

    private Map buildTestMap( String resourceDirName )
    {
        try
        {
            Map result = new HashMap();

            ClassLoader cloader = Thread.currentThread().getContextClassLoader();
            URL mojosXmlUrl = cloader.getResource( resourceDirName + "/test.mojos.xml" );

            if ( mojosXmlUrl == null )
            {
                fail( "No classpath resource named: '" + resourceDirName + "/test.mojos.xml' could be found." );
            }

            // TODO As of JDK 7, replace with Paths.get( resource.toURI() ).toFile()
            File mojosXml = new File( mojosXmlUrl.toURI() );
            File dir = mojosXml.getParentFile();

            Set scripts = new HashSet();
            String[] listing = dir.list();
            for ( int i = 0; listing != null && i < listing.length; i++ )
            {
                if ( listing[i].endsWith( ".mojos.xml" ) )
                {
                    File f = new File( dir, listing[i] ).getAbsoluteFile();

                    scripts.add( f );
                }
            }

            result.put( dir.getAbsolutePath(), scripts );

            return result;
        }
        catch ( final URISyntaxException e )
        {
            throw new AssertionError( e );
        }
    }

    // TODO

}
