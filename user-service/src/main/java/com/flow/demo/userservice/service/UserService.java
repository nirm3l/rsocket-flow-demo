package com.flow.demo.userservice.service;

import com.flow.demo.userservice.exceptions.UserAlreadyExistException;
import com.flow.demo.userservice.exceptions.UserNotFoundException;
import com.flow.demo.userservice.generated.model.User;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final Map<UUID, User> users = new LinkedHashMap<>();

    @PostConstruct
    void init() {
        User user1 = getUser("User1", "user1@demo.com");
        User user2 = getUser("User2", "user2@demo.com");
        User user3 = getUser("User3", "user3@demo.com", User.StatusEnum.INACTIVE);

        users.put(user1.getId(), user1);
        users.put(user2.getId(), user2);
        users.put(user3.getId(), user3);
    }

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

    private User getUser(String name, String email) {
        return getUser(name, email, User.StatusEnum.ACTIVE);
    }

    private User getUser(String name, String email, User.StatusEnum status) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setCreatedAt(OffsetDateTime.now());
        user.setEmail(email);
        user.setStatus(status);
        user.setName(name);

        return user;
    }

    public void deleteUser(UUID userId) {
        if(users.remove(userId) == null) {
            throw new UserNotFoundException();
        }
    }

    public User createUser(User user) {
        Optional<User> existing = users.values().stream()
                .filter(u -> u.getEmail().equals(user.getEmail())).findFirst();

        if(existing.isPresent()) {
            throw new UserAlreadyExistException();
        } else {
            user.setId(UUID.randomUUID());
            user.setCreatedAt(OffsetDateTime.now());

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
