package io.github.lischenerks.taskmanagement.exceptions;

import java.time.LocalDateTime;

public record ErrorResponseDto(String message, String detailedMessage, LocalDateTime errorTime) {}
