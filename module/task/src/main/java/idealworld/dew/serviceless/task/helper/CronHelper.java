/*
 * Copyright 2021. gudaoxuri
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

package idealworld.dew.serviceless.task.helper;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Cron辅助类.
 *
 * @author gudaoxuri
 */
@Slf4j
public class CronHelper {

    private static final Scheduler scheduler;
    private static final Map<String, JobExecutor> JOB_EXECUTORS = new HashMap<>();
    private static final String JOB_KEY_SPLIT = "#";

    static {
        try {
            scheduler = new StdSchedulerFactory().getScheduler();
            scheduler.start();
        } catch (SchedulerException e) {
            throw new RuntimeException("Scheduler init error", e);
        }
    }

    @SneakyThrows
    public static void addJob(String name, String group, String cron, JobExecutor jobExecutor) {
        JOB_EXECUTORS.put(name + JOB_KEY_SPLIT + group, jobExecutor);
        JobDetail job = JobBuilder.newJob(SimpleJob.class)
                .withIdentity(name, group)
                .build();
        Trigger trigger = TriggerBuilder
                .newTrigger()
                .withSchedule(CronScheduleBuilder.cronSchedule(cron))
                .build();
        scheduler.scheduleJob(job, trigger);
        log.info("[CronHelper]Add job[{}-{}]:{}", group, name, cron);
    }

    @SneakyThrows
    public static void removeJob(String name, String group) {
        if (JOB_EXECUTORS.containsKey(name + JOB_KEY_SPLIT + group)) {
            scheduler.deleteJob(new JobKey(name, group));
            JOB_EXECUTORS.remove(name + JOB_KEY_SPLIT + group);
            log.info("[CronHelper]Remove job[{}-{}]", group, name);
        }
    }

    public static class SimpleJob implements Job {

        @Override
        public void execute(JobExecutionContext jobExecutionContext) {
            JOB_EXECUTORS.get(jobExecutionContext.getJobDetail().getKey().getName()
                    + JOB_KEY_SPLIT
                    + jobExecutionContext.getJobDetail().getKey().getGroup())
                    .execute(jobExecutionContext.getJobDetail().getKey().getName(),
                            jobExecutionContext.getJobDetail().getKey().getGroup());
        }

    }

    @FunctionalInterface
    public interface JobExecutor {

        void execute(String name, String group);

    }

}
