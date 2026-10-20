package com.workproof.service;

import com.workproof.model.Location;
import com.workproof.repository.LocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class LocationService {

    private final LocationRepository locationRepository;

    public LocationService(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    @Transactional(readOnly = true)
    public List<Location> getAllLocations() {
        return locationRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Optional<Location> getLocationById(Long id) {
        return locationRepository.findById(id);
    }

    @Transactional
    public Location saveLocation(Location location) {
        return locationRepository.save(location);
    }

    @Transactional(readOnly = true)
    public long countLocations() {
        return locationRepository.count();
    }
}

