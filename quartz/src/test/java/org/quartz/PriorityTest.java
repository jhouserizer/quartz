/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 * Copyright IBM Corp. 2024, 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.quartz;

import java.util.Calendar;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.triggers.SimpleTriggerImpl;
import org.quartz.spi.MutableTrigger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test Trigger priority support.
 */
class PriorityTest  {

    private static CountDownLatch latch;
    private static StringBuffer result;

    @SuppressWarnings("deprecation")
    public static class TestJob implements StatefulJob {
        public void execute(JobExecutionContext context)
                throws JobExecutionException {
            result.append(context.getTrigger().getKey().getName());
            latch.countDown();
        }
    }

    @BeforeEach
    protected void setUp() {
        PriorityTest.latch = new CountDownLatch(2);
        PriorityTest.result = new StringBuffer();
    }

    @SuppressWarnings("deprecation")
    @Test
    void testSameDefaultPriority() throws Exception {
        Properties config = new Properties();
        config.setProperty("org.quartz.threadPool.threadCount", "1");
        config.setProperty("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");

        Scheduler sched = new StdSchedulerFactory(config).getScheduler();

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, 1);

        MutableTrigger trig1 = new SimpleTriggerImpl("T1", null, cal.getTime());
        MutableTrigger trig2 = new SimpleTriggerImpl("T2", null, cal.getTime());

        JobDetail jobDetail = new JobDetailImpl("JD", null, TestJob.class);

        sched.scheduleJob(jobDetail, trig1);

        trig2.setJobKey(new JobKey(jobDetail.getKey().getName()));
        sched.scheduleJob(trig2);

        sched.start();

        latch.await();

        assertEquals("T1T2", result.toString());

        sched.shutdown();
    }

    @SuppressWarnings("deprecation")
    @Test
    void testDifferentPriority() throws Exception {
        Properties config = new Properties();
        config.setProperty("org.quartz.threadPool.threadCount", "1");
        config.setProperty("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");

        Scheduler sched = new StdSchedulerFactory(config).getScheduler();

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, 1);

        MutableTrigger trig1 = new SimpleTriggerImpl("T1", null, cal.getTime());
        trig1.setPriority(5);

        MutableTrigger trig2 = new SimpleTriggerImpl("T2", null, cal.getTime());
        trig2.setPriority(10);

        JobDetail jobDetail = new JobDetailImpl("JD", null, TestJob.class);

        sched.scheduleJob(jobDetail, trig1);

        trig2.setJobKey(new JobKey(jobDetail.getKey().getName(), null));
        sched.scheduleJob(trig2);

        sched.start();

        latch.await();

        assertEquals("T2T1", result.toString());

        sched.shutdown();
    }
}
