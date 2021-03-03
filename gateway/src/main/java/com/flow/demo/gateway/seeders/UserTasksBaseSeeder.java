package com.flow.demo.gateway.seeders;

import com.flow.demo.gateway.generated.model.Task;
import com.flow.demo.gateway.generated.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

abstract class UserTasksBaseSeeder implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserTasksBaseSeeder.class);

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        seed();
    }

    public void seed() {
        int usersCount = 200, tasksCount = 3;

        long start = System.currentTimeMillis();

        Flux.range(1, usersCount)
                .map(i -> getUser(String.format("User%d", i), String.format("user%s@test.com",
                        UUID.randomUUID().toString())))
                .flatMap(this::seedUser)
                .flatMap(user -> Flux.range(1, tasksCount)
                        .map(i -> getTask(user, i))
                        .flatMap(this::seedTask))
                .doFinally(value -> LOGGER.info("Seeding completed in {}!", System.currentTimeMillis() - start))
                .subscribe();
    }

    abstract Mono<User> seedUser(User user);

    abstract Mono<Task> seedTask(Task task);

    protected User getUser(String name, String email) {
        return getUser(name, email, User.StatusEnum.ACTIVE);
    }

    protected User getUser(String name, String email, User.StatusEnum status) {
        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setStatus(status);

        return user;
    }

    protected Task getTask(User user, int i) {
        Task task = new Task();
        task.setCreatedById(user.getId());
        task.setName("Task " + i + " by user " + user.getName());

        return task;
    }

}
