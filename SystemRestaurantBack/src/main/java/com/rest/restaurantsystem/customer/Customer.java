package com.rest.restaurantsystem.customer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(name = "restaurant_customers")
class Customer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(length = 30)
    private String phone;
    @Column(name = "phone_key", length = 15, unique = true)
    private String phoneKey;
    @Column(length = 120)
    private String email;
    @Column(length = 500)
    private String preferences;
    @Column(name = "dietary_notes", length = 300)
    private String dietaryNotes;
    @Column(nullable = false)
    private boolean active = true;
    @Version
    private long version;
    @Column(name = "created_by_username", nullable = false, updatable = false, length = 80)
    private String createdByUsername;
    @Column(name = "updated_by_username", nullable = false, length = 80)
    private String updatedByUsername;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Customer() { }

    Customer(CustomerSaveRequest request, String username, String phoneKey) {
        createdByUsername = username;
        update(request, username, phoneKey);
    }

    void update(CustomerSaveRequest request, String username, String phoneKey) {
        name = request.name().trim();
        phone = clean(request.phone());
        this.phoneKey = phoneKey;
        email = clean(request.email());
        preferences = clean(request.preferences());
        dietaryNotes = clean(request.dietaryNotes());
        updatedByUsername = username;
    }

    void setActive(boolean value, String username) {
        active = value;
        updatedByUsername = username;
    }

    @PrePersist void onCreate() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }

    Long getId() { return id; }
    String getName() { return name; }
    String getPhone() { return phone; }
    String getPhoneKey() { return phoneKey; }
    String getEmail() { return email; }
    String getPreferences() { return preferences; }
    String getDietaryNotes() { return dietaryNotes; }
    boolean isActive() { return active; }
    long getVersion() { return version; }
    String getCreatedByUsername() { return createdByUsername; }
    String getUpdatedByUsername() { return updatedByUsername; }
    Instant getCreatedAt() { return createdAt; }
    Instant getUpdatedAt() { return updatedAt; }

    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
