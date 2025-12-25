package org.example.server.domain.dto;

public record CommandDto(
        String agentId,
        String commandType,
        String commandContent
) {
}
