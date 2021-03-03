package com.flow.demo.gateway.benchmark;

import com.flow.demo.gateway.controllers.TaskController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class Benchmark implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Benchmark.class);

    private final TaskController taskController;

    public Benchmark(TaskController taskController) {
        this.taskController = taskController;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long start = System.currentTimeMillis();

        Mono.when(
                taskController.getFullTasksV2().flatMapMany(HttpEntity::getBody).collectList()
                        .repeat(100)
                        .doFinally(value -> LOGGER.info("Benchmark completed in {} for V2!", System.currentTimeMillis() - start))
                        .subscribeOn(Schedulers.boundedElastic()),
                taskController.getFullTasks().flatMapMany(HttpEntity::getBody).collectList()
                        .repeat(100)
                        .doFinally(value -> LOGGER.info("Benchmark completed in {}!", System.currentTimeMillis() - start))
                        .subscribeOn(Schedulers.boundedElastic())
        ).subscribe();
    }
}
