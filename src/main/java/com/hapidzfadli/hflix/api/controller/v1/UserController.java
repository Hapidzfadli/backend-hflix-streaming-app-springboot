package com.hapidzfadli.hflix.api.controller.v1;


import com.hapidzfadli.hflix.api.dto.UserDTO;
import com.hapidzfadli.hflix.api.dto.WebResponseDTO;
import com.hapidzfadli.hflix.app.service.UserService;
import com.hapidzfadli.hflix.domain.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<WebResponseDTO<List<UserDTO>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "ASC") String direction,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status
    ) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        Page<User> usersPage;

        usersPage = userService.findAllUsers(pageable);

        List<UserDTO> userDTOS = usersPage.getContent().stream()
                .map(UserDTO::fromUser)
                .collect(Collectors.toList());

        WebResponseDTO<List<UserDTO>> response = WebResponseDTO.<List<UserDTO>>builder()
                .success(true)
                .message("User retrieved successfully")
                .data(userDTOS)
                .build();


        Map<String, Object> paginationInfo = createPaginationInfo(usersPage);
        response.setPagination(paginationInfo);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WebResponseDTO<UserDTO>> getUserById(@PathVariable Long id){
        User user = userService.findById(id).orElseThrow(() -> new RuntimeException("User not found with user id: " + id));

        WebResponseDTO<UserDTO> response = WebResponseDTO.success(
                UserDTO.fromUser(user),
                "user retrieved successfully"
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<WebResponseDTO<UserDTO>> createUser(@Valid @RequestBody UserDTO userDTO) {
        User user = userDTO.toUser();
        User createdUser = userService.createUser(user);

        WebResponseDTO<UserDTO> response = WebResponseDTO.success(
                UserDTO.fromUser(createdUser),
                "User created successfully"
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WebResponseDTO<UserDTO>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserDTO userDTO
    ) {
        User user = userDTO.toUser();
        User updatedUser = userService.updateUser(id, user);

        WebResponseDTO<UserDTO> response = WebResponseDTO.success(
                UserDTO.fromUser(updatedUser),
                "User updated successfully"
        );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<WebResponseDTO<Void>> deleteUser(
            @PathVariable Long id
    ) {
        userService.deleteUser(id);

        WebResponseDTO<Void> response = WebResponseDTO.<Void>builder()
                .success(true)
                .message("User deleted successfully")
                .build();

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<WebResponseDTO<UserDTO>> changeUserStatus(
            @PathVariable Long id,
            @RequestParam User.Status status) {

        User updatedUser = userService.changeUserStatus(id, status);

        WebResponseDTO<UserDTO> response = WebResponseDTO.success(
                UserDTO.fromUser(updatedUser),
                "User status updated successfully"
        );

        return ResponseEntity.ok(response);
    }



    private Map<String, Object> createPaginationInfo(Page<User> usersPage) {
        Map<String, Object> paginationInfo = new HashMap<>();
        paginationInfo.put("page", usersPage.getNumber());
        paginationInfo.put("size", usersPage.getSize());
        paginationInfo.put("totalElements", usersPage.getTotalElements());
        paginationInfo.put("totalPages", usersPage.getTotalPages());
        paginationInfo.put("isFirst", usersPage.isFirst());
        paginationInfo.put("isLast", usersPage.isLast());
        return paginationInfo;
    }
}
