package com.gin.pixivmanager2.config;

import com.gin.pixivmanager2.util.TasksUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class TaskExecutePool {
    @Bean
    public ThreadPoolTaskExecutor requestExecutor() {
        return TasksUtil.getExecutor("Request", 10);
    }

    @Bean
    public ThreadPoolTaskExecutor initExecutor() {
        return TasksUtil.getExecutor("init", 10);
    }

    @Bean
    public ThreadPoolTaskExecutor defaultExecutor() {
        return TasksUtil.getExecutor("default", 1);
    }

    @Bean
    public ThreadPoolTaskExecutor downloadExecutor() {
        return TasksUtil.getExecutor("download", 10);
    }


    @Bean(name = "myThreadPoolTaskScheduler")
    public TaskScheduler getTaskScheduler() {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(10);
        taskScheduler.setThreadNamePrefix("scheduler-");
        taskScheduler.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        //调度器shutdown被调用时等待当前被调度的任务完成
        taskScheduler.setWaitForTasksToCompleteOnShutdown(true);
        //等待时长
        taskScheduler.setAwaitTerminationSeconds(60);
        return taskScheduler;
    }
}
