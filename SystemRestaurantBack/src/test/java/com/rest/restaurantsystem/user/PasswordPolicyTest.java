package com.rest.restaurantsystem.user;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordPolicyTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void rejectsPasswordWithoutANumber() {
        UserCreateRequest request = new UserCreateRequest(
                "mesero",
                "Mesero de prueba",
                UserRole.OPERATOR,
                "sololetras"
        );

        assertThat(validator.validate(request))
                .anySatisfy(violation -> {
                    assertThat(violation.getPropertyPath().toString()).isEqualTo("password");
                    assertThat(violation.getMessage()).isEqualTo(PasswordPolicy.COMPOSITION_MESSAGE);
                });
    }

    @Test
    void acceptsPasswordWithLettersAndNumbers() {
        PasswordResetRequest request = new PasswordResetRequest("restaurante1");

        assertThat(validator.validate(request)).isEmpty();
    }
}
