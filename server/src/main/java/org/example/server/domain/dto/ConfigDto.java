package org.example.server.domain.dto;

public record ConfigDto(
        Long id,
        String remarkName,
        Integer syncFrequencySeconds
) {
}
