package com.flow.demo.userservice.service;

import com.fasterxml.uuid.Generators;
import com.flow.demo.userservice.exceptions.UserAlreadyExistException;
import com.flow.demo.userservice.exceptions.UserNotFoundException;
import com.flow.demo.userservice.generated.model.User;
import com.flow.demo.userservice.generated.model.UserSettings;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final Map<UUID, User> users = new ConcurrentSkipListMap<>();

    public Collection<User> getUsers(Boolean onlyActive, List<UUID> uuids) {
        return users.values().stream().filter(user -> {
            if(uuids != null && uuids.size() > 0 && !uuids.contains(user.getId())) {
                return false;
            }

            if(onlyActive && user.getStatus() == User.StatusEnum.INACTIVE) {
                return false;
            }

            return true;
        }).collect(Collectors.toList());
    }

    public User getUser(UUID uuid) {
        User u = users.get(uuid);

        if(u == null) {
            throw new UserNotFoundException();
        }

        return u;
    }

    public User deleteUser(UUID userId) {
        User user = users.remove(userId);

        if(user == null) {
            throw new UserNotFoundException();
        }

        return user;
    }

    public User createUser(User user) {
        Optional<User> existing = users.values().stream()
                .filter(u -> u.getEmail().equals(user.getEmail())).findFirst();

        if(existing.isPresent()) {
            throw new UserAlreadyExistException();
        } else {
            user.setId(Generators.timeBasedGenerator().generate());
            user.setCreatedAt(OffsetDateTime.now());
            user.setSettings(new UserSettings());

            if(user.getStatus() == null) {
                user.setStatus(User.StatusEnum.ACTIVE);
            }

            users.put(user.getId(), user);

            return user;
        }
    }

    public User updateUser(UUID userId, User user) {
        Optional<User> existing = users.values().stream()
                .filter(u -> u.getId().equals(userId)).findFirst();

        if(existing.isEmpty()) {
            throw new UserNotFoundException();
        } else {
            User existingUser = existing.get();

            if(user.getName() != null) {
                existingUser.setName(user.getName());
            }

            if(user.getEmail() != null) {
                existingUser.setEmail(user.getEmail());
            }

            if(user.getStatus() != null) {
                existingUser.setStatus(user.getStatus());
            }

            existingUser.setUpdatedAt(OffsetDateTime.now());
            existingUser.setUpdatedBy(UUID.randomUUID()); // Should be real user id

            return existingUser;
        }
    }
}
