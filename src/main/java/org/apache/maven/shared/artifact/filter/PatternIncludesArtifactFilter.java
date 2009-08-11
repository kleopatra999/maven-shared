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
package org.apache.maven.shared.artifact.filter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.StringUtils;

/**
 * TODO: include in maven-artifact in future
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @see StrictPatternIncludesArtifactFilter
 */
public class PatternIncludesArtifactFilter
    implements ArtifactFilter, StatisticsReportingArtifactFilter
{
    private final List positivePatterns;

    private final List negativePatterns;

    private final boolean actTransitively;

    private Set patternsTriggered = new HashSet();

    private List filteredArtifactIds = new ArrayList();

    public PatternIncludesArtifactFilter( List patterns )
    {
        this( patterns, false );
    }

    public PatternIncludesArtifactFilter( List patterns, boolean actTransitively )
    {
        this.actTransitively = actTransitively;
        List pos = new ArrayList();
        List neg = new ArrayList();
        if ( ( patterns != null ) && !patterns.isEmpty() )
        {
            for ( Iterator it = patterns.iterator(); it.hasNext(); )
            {
                String pattern = (String) it.next();

                if ( pattern.startsWith( "!" ) )
                {
                    neg.add( pattern.substring( 1 ) );
                }
                else
                {
                    pos.add( pattern );
                }
            }
        }

        positivePatterns = pos;
        negativePatterns = neg;
    }

    public boolean include( Artifact artifact )
    {
        boolean shouldInclude = patternMatches( artifact );

        if ( !shouldInclude )
        {
            addFilteredArtifactId( artifact.getId() );
        }

        return shouldInclude;
    }

    protected boolean patternMatches( Artifact artifact )
    {
        return ( positiveMatch( artifact ) == Boolean.TRUE ) || ( negativeMatch( artifact ) == Boolean.FALSE );
    }

    protected void addFilteredArtifactId( String artifactId )
    {
        filteredArtifactIds.add( artifactId );
    }

    private Boolean negativeMatch( Artifact artifact )
    {
        if ( ( negativePatterns == null ) || negativePatterns.isEmpty() )
        {
            return null;
        }
        else
        {
            return Boolean.valueOf( match( artifact, negativePatterns ) );
        }
    }

    protected Boolean positiveMatch( Artifact artifact )
    {
        if ( ( positivePatterns == null ) || positivePatterns.isEmpty() )
        {
            return null;
        }
        else
        {
            return Boolean.valueOf( match( artifact, positivePatterns ) );
        }
    }

    private boolean match( Artifact artifact, List patterns )
    {
        String shortId = ArtifactUtils.versionlessKey( artifact );
        String id = artifact.getDependencyConflictId();
        String wholeId = artifact.getId();

        if ( matchAgainst( wholeId, patterns, false ) )
        {
            return true;
        }

        if ( matchAgainst( id, patterns, false ) )
        {
            return true;
        }

        if ( matchAgainst( shortId, patterns, false ) )
        {
            return true;
        }

        if ( actTransitively )
        {
            List depTrail = artifact.getDependencyTrail();

            if ( ( depTrail != null ) && !depTrail.isEmpty() )
            {
                String trailStr = "," + StringUtils.join( depTrail.iterator(), "," );

                return matchAgainst( trailStr, patterns, true );
            }
        }

        return false;
    }

    private boolean matchAgainst( String value, List patterns, boolean regionMatch ) {
    	for (Iterator iterator = patterns.iterator(); iterator.hasNext();) {
			String pattern = (String) iterator.next();
			
			String[] patternTokens = pattern.split( ":" );
			String[] tokens = value.split( ":" );
			
			// fail immediately if pattern tokens outnumber tokens to match
	        boolean matched = ( patternTokens.length <= tokens.length );

	        for ( int i = 0; matched && i < patternTokens.length; i++ )
	        {
	            matched = matches( tokens[i], patternTokens[i] );
	        }
	        
//	        // case of starting '*' like '*:jar:*'
	        if (!matched && patternTokens.length < tokens.length && patternTokens.length>0 && "*".equals(patternTokens[0])) 
	        {
	        	matched=true;
		        for ( int i = 0; matched && i < patternTokens.length; i++ )
		        {
		            matched = matches( tokens[i+(tokens.length-patternTokens.length)], patternTokens[i] );
		        }
	        }

	        if (matched) {
	        	patternsTriggered.add( pattern );
                return true;
	        }
	        
	        if ( regionMatch && value.indexOf( pattern ) > -1 )
            {
                patternsTriggered.add( pattern );
                return true;
            }
			
		}
    	return false;
    	
    }

    /**
     * Gets whether the specified token matches the specified pattern segment.
     * 
     * @param token
     *            the token to check
     * @param pattern
     *            the pattern segment to match, as defined above
     * @return <code>true</code> if the specified token is matched by the specified pattern segment
     */
    private boolean matches( String token, final String pattern )
    {
    	boolean matches;

        // support full wildcard and implied wildcard
        if ( "*".equals( pattern ) || pattern.length() == 0 )
        {
            matches = true;
        }
        // support contains wildcard
        else if ( pattern.startsWith( "*" ) && pattern.endsWith( "*" ) )
        {
            String contains = pattern.substring( 1, pattern.length() - 1 );

            matches = ( token.indexOf( contains ) != -1 );
        }
        // support leading wildcard
        else if ( pattern.startsWith( "*" ) )
        {
            String suffix = pattern.substring( 1, pattern.length() );

            matches = token.endsWith( suffix );
        }
        // support trailing wildcard
        else if ( pattern.endsWith( "*" ) )
        {
            String prefix = pattern.substring( 0, pattern.length() - 1 );

            matches = token.startsWith( prefix );
        }
        // support versions range 
        else if ( pattern.startsWith( "[" ) || pattern.startsWith( "(" ))
        {
        	matches = isVersionIncludedInRange(token, pattern);
        }
        // support exact match
        else
        {
            matches = token.equals( pattern );
        }

        return matches;
    }
    
    private boolean isVersionIncludedInRange(final String version, final String range) {
    	try {
			return VersionRange.createFromVersionSpec(range).containsVersion(new DefaultArtifactVersion(version));
		} catch (InvalidVersionSpecificationException e) {
			return false;
		}
	}

    public void reportMissedCriteria( Logger logger )
    {
        // if there are no patterns, there is nothing to report.
        if ( !positivePatterns.isEmpty() || !negativePatterns.isEmpty() )
        {
            List missed = new ArrayList();
            missed.addAll( positivePatterns );
            missed.addAll( negativePatterns );

            missed.removeAll( patternsTriggered );

            if ( !missed.isEmpty() && logger.isWarnEnabled() )
            {
                StringBuffer buffer = new StringBuffer();

                buffer.append( "The following patterns were never triggered in this " );
                buffer.append( getFilterDescription() );
                buffer.append( ':' );

                for ( Iterator it = missed.iterator(); it.hasNext(); )
                {
                    String pattern = (String) it.next();

                    buffer.append( "\no  \'" ).append( pattern ).append( "\'" );
                }

                buffer.append( "\n" );

                logger.warn( buffer.toString() );
            }
        }
    }

    public String toString()
    {
        return "Includes filter:" + getPatternsAsString();
    }

    protected String getPatternsAsString()
    {
        StringBuffer buffer = new StringBuffer();
        for ( Iterator it = positivePatterns.iterator(); it.hasNext(); )
        {
            String pattern = (String) it.next();

            buffer.append( "\no \'" ).append( pattern ).append( "\'" );
        }

        return buffer.toString();
    }

    protected String getFilterDescription()
    {
        return "artifact inclusion filter";
    }

    public void reportFilteredArtifacts( Logger logger )
    {
        if ( !filteredArtifactIds.isEmpty() && logger.isDebugEnabled() )
        {
            StringBuffer buffer = new StringBuffer( "The following artifacts were removed by this "
                + getFilterDescription() + ": " );

            for ( Iterator it = filteredArtifactIds.iterator(); it.hasNext(); )
            {
                String artifactId = (String) it.next();

                buffer.append( '\n' ).append( artifactId );
            }

            logger.debug( buffer.toString() );
        }
    }

    public boolean hasMissedCriteria()
    {
        // if there are no patterns, there is nothing to report.
        if ( !positivePatterns.isEmpty() || !negativePatterns.isEmpty() )
        {
            List missed = new ArrayList();
            missed.addAll( positivePatterns );
            missed.addAll( negativePatterns );

            missed.removeAll( patternsTriggered );

            return !missed.isEmpty();
        }

        return false;
    }

}
