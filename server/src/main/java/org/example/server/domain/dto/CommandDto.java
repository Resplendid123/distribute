package org.example.server.domain.dto;

public record CommandDto(
        Long id,
        String commandType,
        String commandContent
) {
}
