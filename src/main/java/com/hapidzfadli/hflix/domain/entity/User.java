package com.hapidzfadli.hflix.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "username is required")
    @Size(min = 3, max = 50, message = "username must between 3 and 50 character")
    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @NotBlank(message = "email is required")
    @Column(unique = true, nullable = false, length = 100)
    @Email(message = "email should be valid")
    private String email;

    @NotBlank(message = "password is required")
    @Column(nullable = false, length = 255)
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "Password must be at least 8 characters long and include one uppercase letter, one lowercase letter, " +
                    "one digit, and one special character")
    private String password;


    @Size(max = 100, message = "Full name must be less than 100 characters")
    @Column(name = "full_name", length = 100)
    private String fullName;

    @Enumerated(EnumType.STRING)
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;

    @Column(name = "profile_picture")
    private String profilePicture;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;


    public enum Role {
        ADMIN,
        USER,
    }

    public enum Status {
        ACTIVE,
        INACTIVE,
        SUSPENDED
    }

}


