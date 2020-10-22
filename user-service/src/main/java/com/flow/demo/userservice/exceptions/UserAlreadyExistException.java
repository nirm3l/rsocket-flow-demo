package com.flow.demo.userservice.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class UserAlreadyExistException extends ResponseStatusException {
    public UserAlreadyExistException() {
        super(HttpStatus.BAD_REQUEST, "User already exist");
    }
}
