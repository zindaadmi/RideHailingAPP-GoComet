package com.interview.gocomet.GoComet.DAW.config;

import com.interview.gocomet.GoComet.DAW.model.Driver;
import com.interview.gocomet.GoComet.DAW.model.DriverStatus;
import com.interview.gocomet.GoComet.DAW.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Data initializer for development/testing.
 * Creates sample drivers when application starts.
 * Only runs in 'dev' or 'default' profile.
 */
@Component
@Profile({"dev", "default"})
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {
    
    private final DriverRepository driverRepository;
    
    @Override
    public void run(String... args) {
        // Only initialize if no drivers exist
        if (driverRepository.count() == 0) {
            log.info("Initializing sample drivers...");
            
            createDriver("DRIVER-1", "John Doe", "1234567890", "DL-01-AB-1234", "SEDAN", 
                        28.7041, 77.1025);
            createDriver("DRIVER-2", "Jane Smith", "1234567891", "DL-02-CD-5678", "SUV", 
                        28.7050, 77.1030);
            createDriver("DRIVER-3", "Bob Johnson", "1234567892", "DL-03-EF-9012", "HATCHBACK", 
                        28.7060, 77.1040);
            createDriver("DRIVER-4", "Alice Williams", "1234567893", "DL-04-GH-3456", "SEDAN", 
                        28.7070, 77.1050);
            createDriver("DRIVER-5", "Charlie Brown", "1234567894", "DL-05-IJ-7890", "SUV", 
                        28.7080, 77.1060);
            
            log.info("Sample drivers initialized successfully!");
        } else {
            log.info("Drivers already exist. Skipping initialization.");
        }
    }
    
    private void createDriver(String driverId, String name, String phoneNumber, 
                              String vehicleNumber, String vehicleType,
                              Double latitude, Double longitude) {
        Driver driver = Driver.builder()
            .driverId(driverId)
            .name(name)
            .phoneNumber(phoneNumber)
            .vehicleNumber(vehicleNumber)
            .vehicleType(vehicleType)
            .status(DriverStatus.AVAILABLE)
            .latitude(latitude)
            .longitude(longitude)
            .lastLocationUpdate(LocalDateTime.now())
            .build();
        
        driverRepository.save(driver);
        log.debug("Created driver: {}", driverId);
    }
}

