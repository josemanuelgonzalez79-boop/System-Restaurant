package com.rest.restaurantsystem.user;

import com.rest.restaurantsystem.exception.BadRequestException;
import com.rest.restaurantsystem.exception.ConflictException;
import com.rest.restaurantsystem.exception.ResourceNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserService(AppUserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public boolean setupRequired() {
        return repository.count() == 0;
    }

    @Transactional
    public UserResponse createInitialOwner(InitialSetupRequest request) {
        if (repository.count() > 0) {
            throw new ConflictException("La configuración inicial ya fue completada.");
        }

        AppUser owner = new AppUser(
                request.username(),
                passwordEncoder.encode(request.password()),
                request.fullName(),
                UserRole.OWNER
        );
        return UserResponse.from(repository.save(owner));
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(String username) {
        return UserResponse.from(findByUsername(username));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return repository.findAllByOrderByFullNameAsc()
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        if (repository.existsByUsernameIgnoreCase(request.username().trim())) {
            throw new ConflictException("Ya existe un usuario con ese nombre de acceso.");
        }

        AppUser user = new AppUser(
                request.username(),
                passwordEncoder.encode(request.password()),
                request.fullName(),
                request.role()
        );
        return UserResponse.from(repository.save(user));
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        AppUser user = getEntity(id);
        protectLastOwner(user, request.role(), user.isActive());
        user.update(request);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse changeActive(Long id, boolean active, String currentUsername) {
        AppUser user = getEntity(id);
        if (!active && user.getUsername().equalsIgnoreCase(currentUsername)) {
            throw new BadRequestException("No puedes desactivar tu propia cuenta.");
        }
        protectLastOwner(user, user.getRole(), active);
        user.setActive(active);
        return UserResponse.from(user);
    }

    @Transactional
    public void resetPassword(Long id, PasswordResetRequest request) {
        AppUser user = getEntity(id);
        user.changePassword(passwordEncoder.encode(request.password()));
    }

    AppUser findByUsername(String username) {
        return repository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el usuario."));
    }

    private AppUser getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el usuario."));
    }

    private void protectLastOwner(AppUser user, UserRole resultingRole, boolean resultingActive) {
        if (user.getRole() != UserRole.OWNER || !user.isActive()) {
            return;
        }
        boolean stopsBeingActiveOwner = resultingRole != UserRole.OWNER || !resultingActive;
        if (stopsBeingActiveOwner && repository.countByRoleAndActiveTrue(UserRole.OWNER) <= 1) {
            throw new BadRequestException("Debe permanecer al menos un propietario activo.");
        }
    }
}
