package com.hapidzfadli.hflix.app.service;

import com.hapidzfadli.hflix.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface UserService {
    List<User> findAllUsers();
    Page<User> findAllUsers(Pageable pageable);
    Optional<User> findById(Long id);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    User createUser (User user);
    User updateUser(Long id, User user);
    void deleteUser(Long id);
    void updateLastLogin(String username);
    User changeUserStatus(Long id, User.Status status);
}
