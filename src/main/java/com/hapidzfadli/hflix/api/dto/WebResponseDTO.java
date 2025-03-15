package com.hapidzfadli.hflix.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cglib.core.Local;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebResponseDTO<T> {

    @Builder.Default
    private boolean success = true;

    @Builder.Default
    private String message = "Success";
    private T data;
    private Map<String, Object> pagination;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    public static <T> WebResponseDTO<T> success(T data) {
        return WebResponseDTO.<T>builder()
                .success(true)
                .message("Success")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> WebResponseDTO<T> success(T data, String message){
        return WebResponseDTO.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> WebResponseDTO<List<T>> fromPage(Page<T> page){
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", page.getNumber());
        pagination.put("size", page.getSize());
        pagination.put("totalElements", page.getTotalElements());
        pagination.put("totalPages", page.getTotalPages());
        pagination.put("isFirst", page.isFirst());
        pagination.put("isLast", page.isLast());

        return WebResponseDTO.<List<T>>builder()
                .success(true)
                .message("Success")
                .data(page.getContent())
                .pagination(pagination)
                .timestamp(LocalDateTime.now())
                .build();

    }

    public static <T> WebResponseDTO<List<T>> paginated(List<T> content, int page, int size, long total) {
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", page);
        pagination.put("size", size);
        pagination.put("totalElements", total);
        pagination.put("totalPages", (int) Math.ceil((double) total / size));
        pagination.put("isFirst", page == 0);
        pagination.put("isLast", (page + 1) * size >= total);

        return WebResponseDTO.<List<T>>builder()
                .success(true)
                .message("Success")
                .data(content)
                .pagination(pagination)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // Static factory method for error responses
    public static <T> WebResponseDTO<T> error(String errorMessage) {
        return WebResponseDTO.<T>builder()
                .success(false)
                .message(errorMessage)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // Error response with data (for validation errors, etc.)
    public static <T> WebResponseDTO<T> error(String errorMessage, T data) {
        return WebResponseDTO.<T>builder()
                .success(false)
                .message(errorMessage)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

}
