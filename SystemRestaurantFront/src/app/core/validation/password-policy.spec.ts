import { FormControl, FormGroup } from '@angular/forms';

import { passwordsMatchValidator, PASSWORD_VALIDATORS } from './password-policy';

describe('password policy', () => {
  it('should reject a password without a number', () => {
    const password = new FormControl('sololetras', {
      nonNullable: true,
      validators: PASSWORD_VALIDATORS,
    });

    expect(password.hasError('pattern')).toBe(true);
  });

  it('should accept a password with the required length, letters and numbers', () => {
    const password = new FormControl('restaurante1', {
      nonNullable: true,
      validators: PASSWORD_VALIDATORS,
    });

    expect(password.valid).toBe(true);
  });

  it('should detect when the password confirmation does not match', () => {
    const form = new FormGroup(
      {
        password: new FormControl('restaurante1', { nonNullable: true }),
        confirmPassword: new FormControl('restaurante2', { nonNullable: true }),
      },
      { validators: passwordsMatchValidator },
    );

    expect(form.hasError('passwordMismatch')).toBe(true);
  });
});
