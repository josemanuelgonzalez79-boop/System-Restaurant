package com.rest.restaurantsystem.restaurant;

import org.springframework.data.jpa.repository.JpaRepository;

interface RestaurantSettingsRepository extends JpaRepository<RestaurantSettings, Long> {
}
