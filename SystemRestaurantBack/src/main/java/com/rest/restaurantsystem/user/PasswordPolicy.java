package com.rest.restaurantsystem.user;

final class PasswordPolicy {

    static final int MIN_LENGTH = 10;
    static final int MAX_LENGTH = 72;
    static final String REQUIRED_MESSAGE = "es obligatoria";
    static final String LENGTH_MESSAGE = "debe contener entre 10 y 72 caracteres";
    static final String COMPOSITION_REGEX = "^(?=.*\\p{L})(?=.*\\d).+$";
    static final String COMPOSITION_MESSAGE = "debe incluir al menos una letra y un número";

    private PasswordPolicy() {
    }
}
