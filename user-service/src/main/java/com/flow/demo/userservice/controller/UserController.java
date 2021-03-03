package com.flow.demo.userservice.controller;

import com.flow.demo.userservice.generated.api.UserApi;
import com.flow.demo.userservice.generated.model.User;
import com.flow.demo.userservice.service.UserService;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
public class UserController implements UserApi {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    public Mono<ResponseEntity<Flux<User>>> getUsers(
            @Valid @RequestParam(value = "onlyActive", required = false, defaultValue = "false") Boolean onlyActive,
            @Valid @RequestParam(value = "ids", required = false) List<UUID> ids, ServerWebExchange exchange) {
        return Mono.just(
                ResponseEntity.ok(
                        Flux.fromIterable(userService.getUsers(onlyActive, ids))
                )
        );
    }

    public Mono<ResponseEntity<User>> getUser(
            @PathVariable("relatedUserId") UUID relatedUserId, ServerWebExchange exchange) {
        return Mono.just(
                ResponseEntity.ok(
                        userService.getUser(relatedUserId)
                )
        );
    }

    public Mono<ResponseEntity<User>> createUser(@Valid @RequestBody Mono<User> user, ServerWebExchange exchange) {
        return user.map(u -> ResponseEntity.ok(
                userService.createUser(u)
        ));
    }

    public Mono<ResponseEntity<Void>> deleteUser(
            @PathVariable("relatedUserId") UUID relatedUserId, ServerWebExchange exchange) {
        userService.deleteUser(relatedUserId);

        return Mono.just(ResponseEntity.ok().build());
    }

    public Mono<ResponseEntity<User>> updateUser(
            @PathVariable("relatedUserId") UUID relatedUserId,
            @Valid @RequestBody Mono<User> user, ServerWebExchange exchange) {
        return user.map(u -> ResponseEntity.ok(
                userService.updateUser(relatedUserId, u)));
    }

    @MessageMapping("/users/{userId}")
    public Mono<User> user(@DestinationVariable UUID userId, @Header(
            value = "method", required = false) HttpMethod method) {

        User user;

        if(HttpMethod.DELETE == method) {
            user = userService.deleteUser(userId);
        } else {
            user = userService.getUser(userId);
        }

        if(user != null) {
            return Mono.just(user);
        }

        return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @MessageMapping("/users")
    public Mono<?> users(@Payload(required = false) User user) {
        if(user != null) {
            return Mono.just(userService.createUser(user));
        }

        return Mono.just(userService.getUsers(false, null));
    }

    @MessageMapping("/stream/users")
    public Flux<User> users() {
        return Flux.fromIterable(userService.getUsers(false, null));
    }
}
