package com.rest.restaurantsystem.backup;

public class BackupGenerationException extends RuntimeException {
    public BackupGenerationException(Throwable cause) {
        super(cause.getMessage(), cause);
    }
}
