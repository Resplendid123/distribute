package org.example.server.domain.dto;

public record ConfigDto(
        String ip,
        Integer syncFrequencySeconds
) {
}
