/*
 * Copyright 2009 Martin Grotzke
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an &quot;AS IS&quot; BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package de.javakaffee.web.msm;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.lang.reflect.Method;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import de.javakaffee.web.msm.Statistics.MinMaxAvgProbe;

/**
 * Test the {@link Statistics}.
 *
 * @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a>
 */
public class StatisticsTest {

    public static void main( final String[] args ) {
        final Method[] methods = Statistics.class.getMethods();
        for ( final Method method : methods ) {
            if ( method.getName().startsWith( "get" )) {
                System.out.println( method.getName() );
            }
        }
    }

    @DataProvider( name = "methodNamesProvider" )
    public Object[][] createStatisticMethodNames() {
        return new Object[][] {
                { "getRequestsWithBackupFailure", "requestWithBackupFailure" },
                { "getRequestsWithTomcatFailover", "requestWithTomcatFailover" },
                { "getRequestsWithMemcachedFailover", "requestWithMemcachedFailover" },
                { "getRequestsWithoutSession", "requestWithoutSession" },
                { "getRequestsWithoutSessionAccess", "requestWithoutSessionAccess" },
                { "getRequestsWithoutAttributesAccess", "requestWithoutAttributesAccess" },
                { "getRequestsWithoutSessionModification", "requestWithoutSessionModification" },
                { "getRequestsWithSession", "requestWithSession" },
                { "getNonStickySessionsPingFailed", "nonStickySessionsPingFailed" },
                { "getNonStickySessionsReadOnlyRequest", "nonStickySessionsReadOnlyRequest" }
        };
    }

    @Test( dataProvider = "methodNamesProvider" )
    public void testCounts( final String getterMethod, final String updateMethod ) throws Exception {
        final Statistics cut = Statistics.create();
        final Method getMethod = Statistics.class.getMethod( getterMethod );
        assertEquals( ((Long)getMethod.invoke( cut )).longValue(), 0 );
        Statistics.class.getMethod( updateMethod ).invoke( cut );
        assertEquals( ((Long)getMethod.invoke( cut )).longValue(), 1 );
    }

    @Test
    public void testDisabledRequestWithBackup() {
        final Statistics cut = Statistics.create( false );
        assertEquals( cut.getRequestsWithBackupFailure(), 0 );
        cut.requestWithBackupFailure();
        assertEquals( cut.getRequestsWithBackupFailure(), 0 );
    }

    @Test
    public void testMinMaxAvgProbe() {
        final MinMaxAvgProbe cut = new MinMaxAvgProbe();
        assertValues( cut, 0, 0, 0, 0 );

        cut.register( 1 );
        assertValues( cut, 1, 1, 1, 1 );

        cut.register( 1 );
        assertValues( cut, 2, 1, 1, 1 );

        cut.register( 4 );
        assertValues( cut, 3, 1, 4, 2 );

        cut.register( 0 );
        assertValues( cut, 4, 0, 4, 1.5 );
    }

    @Test
    public void testWatch() throws Exception {
        final Statistics cut = Statistics.create();
        final Statistics.Watch watch = cut.stopWatch(Statistics.StatsType.BACKUP);
        Thread.sleep(1000);
        watch.stop();
        assertEquals(Statistics.StatsType.BACKUP,watch.getStatsType());
        assertTrue(watch.getTime() >= 1 && watch.getTime() <=2);
    }

    @Test
    public void testDoubleStopWatch() throws Exception {
       final Statistics cut = Statistics.create();
       final Statistics.Watch watch = cut.stopWatch(Statistics.StatsType.BACKUP);
       watch.stop();
       try {
           watch.stop() ;
           fail("Watch don't disallow double stop ");
       } catch (IllegalStateException ie) {
           assertTrue(true);
       }
    }

    private void assertValues( final MinMaxAvgProbe cut, final int count, final int min, final int max, final double avg ) {
        assertEquals( cut.getCount(), count );
        assertEquals( cut.getMin(), min );
        assertEquals( cut.getMax(), max );
        assertEquals( cut.getAvg(), avg, 0.0000001 );
    }

}
