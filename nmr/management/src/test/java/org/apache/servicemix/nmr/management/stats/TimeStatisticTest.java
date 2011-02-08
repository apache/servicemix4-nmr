/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.nmr.management.stats;

public class TimeStatisticTest extends StatisticTestSupport {

    private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TimeStatisticTest.class);

    /**
     * Use case for TimeStatisticImpl class.
     * @throws Exception
     */
    public void testStatistic() throws Exception {
        TimeStatistic stat = new TimeStatistic("myTimer", "millis", "myDescription");
        assertStatistic(stat, "myTimer", "millis", "myDescription");

        assertEquals(0, stat.getUpdateCount());

        stat.updateValue(100);
        assertEquals(1, stat.getUpdateCount());
        assertEquals(100, stat.getMinTime());
        assertEquals(100, stat.getMaxTime());

        stat.updateValue(403);
        assertEquals(2, stat.getUpdateCount());
        assertEquals(100, stat.getMinTime());
        assertEquals(403, stat.getMaxTime());

        stat.updateValue(50);
        assertEquals(3, stat.getUpdateCount());
        assertEquals(50, stat.getMinTime());
        assertEquals(403, stat.getMaxTime());


        assertEquals(553, stat.getValue());

        Thread.sleep(500);

        stat.updateValue(10);

        assertLastTimeNotStartTime(stat);

        logger.info("Stat is: {}", stat);

        stat.reset();

        assertEquals(0, stat.getUpdateCount());
        assertEquals(0, stat.getMinTime());
        assertEquals(0, stat.getMaxTime());
        assertEquals(0, stat.getValue());

        stat.updateValue(100);
        assertEquals(1, stat.getUpdateCount());
        assertEquals(100, stat.getMinTime());
        assertEquals(100, stat.getMaxTime());
        assertEquals(100, stat.getValue());

    }
}
