package com.hapidzfadli.hflix.app.service.impl;

import com.hapidzfadli.hflix.app.service.UserService;
import com.hapidzfadli.hflix.domain.entity.User;
import com.hapidzfadli.hflix.domain.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public Page<User> findAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public Optional<User> findByUsername(String username){
        return userRepository.findByUsername(username);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    @Transactional
    public User createUser(User user) {
        if(userRepository.existsByUsername(user.getUsername())){
            throw new IllegalArgumentException("Username already exist");
        }

        if(userRepository.existsByEmail(user.getEmail())){
            throw new IllegalArgumentException("Email already exist");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User updateUser(Long id, User user){
        User existingUser = userRepository.findById(id)
                .orElseThrow(()-> new EntityNotFoundException("User not found"));

        if(user.getUsername() != null && !user.getUsername().equals(existingUser.getUsername())){
            if(userRepository.existsByUsername(user.getUsername())){
                throw new IllegalArgumentException("Username already exist");
            }
        }

        if(user.getEmail() != null && !user.getEmail().equals(existingUser.getEmail())){
            if(userRepository.existsByEmail(user.getEmail())){
                throw new IllegalArgumentException("Email already exist");
            }
        }

        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        if (user.getFullName() != null) {
            existingUser.setFullName(user.getFullName());
        }

        if (user.getRole() != null) {
            existingUser.setRole(user.getRole());
        }

        if (user.getStatus() != null) {
            existingUser.setStatus(user.getStatus());
        }

        if (user.getProfilePicture() != null) {
            existingUser.setProfilePicture(user.getProfilePicture());
        }

        return userRepository.save(existingUser);
    }


    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new EntityNotFoundException("User not found with ID: " + id);
        }
        userRepository.deleteById(id);
    }


    @Transactional
    public void updateLastLogin(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found with username: " + username));

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public User changeUserStatus(Long id, User.Status status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + id));

        user.setStatus(status);
        return userRepository.save(user);
    }
}
