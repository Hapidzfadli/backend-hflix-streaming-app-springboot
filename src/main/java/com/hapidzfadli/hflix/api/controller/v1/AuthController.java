package com.hapidzfadli.hflix.api.controller.v1;
import com.hapidzfadli.hflix.api.dto.*;
import com.hapidzfadli.hflix.app.service.UserService;
import com.hapidzfadli.hflix.app.service.impl.JwtTokenProvider;
import com.hapidzfadli.hflix.config.JwtProperties;
import com.hapidzfadli.hflix.domain.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication API for login and registration")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private JwtProperties jwtProperties;

    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account with the provided details"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "User registered successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = WebResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = WebResponseDTO.class)
                    )
            )
    })
    @PostMapping("/register")
    public ResponseEntity<WebResponseDTO<UserDTO>> register(@Valid @RequestBody RegisterRequestDTO registerRequestDTO) {
        User user = new User();
        user.setUsername(registerRequestDTO.getUsername());
        user.setEmail(registerRequestDTO.getEmail());
        user.setPassword(registerRequestDTO.getPassword());
        user.setFullName(registerRequestDTO.getFullName());
        user.setRole(User.Role.USER);
        user.setStatus(User.Status.ACTIVE);

        User createdUser = userService.createUser(user);

        WebResponseDTO<UserDTO> response = WebResponseDTO.success(
                UserDTO.fromUser(createdUser),
                "User registered successfully"
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Authenticate user",
            description = "Authenticates a user and returns a JWT token"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Login successful",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = WebResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication failed",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = WebResponseDTO.class)
                    )
            )
    })
    @PostMapping("/login")
    public ResponseEntity<WebResponseDTO<JwtResponseDTO>> login(@Valid @RequestBody LoginRequestDTO loginRequest) {
        log.info("Authenticating user: {}", loginRequest.getUsername());

        // Authenticate the user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        // Set authentication in the security context
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Get user from database
        User user = userService.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate JWT token
        String jwt = tokenProvider.generateToken(user);

        // Update last login timestamp
        userService.updateLastLogin(user.getUsername());

        // Create response
        JwtResponseDTO jwtResponse = JwtResponseDTO.builder()
                .tokenType(jwtProperties.getTokenPrefix().trim())
                .accessToken(jwt)
                .expiresIn(jwtProperties.getExpirationsMs() / 1000)  // Convert to seconds
                .user(UserDTO.fromUser(user))
                .build();

        WebResponseDTO<JwtResponseDTO> response = WebResponseDTO.success(
                jwtResponse,
                "Login successful"
        );

        return ResponseEntity.ok(response);
    }
}