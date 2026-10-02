package com.mycompany.myapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mycompany.myapp.domain.Driver;
import com.mycompany.myapp.domain.Vehicle;
import com.mycompany.myapp.repository.DriverRepository;
import com.mycompany.myapp.repository.OfficeRepository;
import com.mycompany.myapp.repository.TripRepository;
import com.mycompany.myapp.repository.VehicleRepository;
import com.mycompany.myapp.service.audit.AuditRecorder;
import com.mycompany.myapp.service.mapper.DriverMapper;
import com.mycompany.myapp.service.mapper.VehicleMapper;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VehicleDriverDeleteTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private VehicleMapper vehicleMapper;

    @Mock
    private OfficeRepository officeRepository;

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private DriverMapper driverMapper;

    @Mock
    private AuditRecorder auditRecorder;

    @Mock
    private TripRepository tripRepository;

    private VehicleService vehicleService;
    private DriverService driverService;

    @BeforeEach
    void setUp() {
        vehicleService = new VehicleService(
            vehicleRepository,
            vehicleMapper,
            officeRepository,
            driverRepository,
            auditRecorder,
            tripRepository
        );
        driverService = new DriverService(driverRepository, driverMapper, tripRepository, vehicleRepository);
    }

    private static Vehicle vehicle() {
        Vehicle v = new Vehicle();
        v.setId(7L);
        v.setPlateNumber("29E03207");
        v.setCapacityKg(new BigDecimal("500"));
        v.setActive(true);
        return v;
    }

    @Test
    void vehicleWithTrips_isDeactivatedNotDeleted() {
        Vehicle v = vehicle();
        when(vehicleRepository.findOneWithRefs(7L)).thenReturn(Optional.of(v));
        when(tripRepository.existsByVehicle_Id(7L)).thenReturn(true);

        vehicleService.delete(7L);

        assertThat(v.getActive()).isFalse();
        verify(vehicleRepository).save(v);
        verify(vehicleRepository, never()).deleteById(any());
    }

    @Test
    void unusedVehicle_isDeleted() {
        when(vehicleRepository.findOneWithRefs(7L)).thenReturn(Optional.of(vehicle()));
        when(tripRepository.existsByVehicle_Id(7L)).thenReturn(false);

        vehicleService.delete(7L);

        verify(vehicleRepository).deleteById(7L);
    }

    @Test
    void driverOnTripsOrVehicle_isDeactivatedNotDeleted() {
        Driver d = new Driver();
        d.setId(3L);
        d.setActive(true);
        when(driverRepository.findById(3L)).thenReturn(Optional.of(d));
        when(tripRepository.existsByDriver_Id(3L)).thenReturn(false);
        when(vehicleRepository.existsByDefaultDriver_Id(3L)).thenReturn(true);

        driverService.delete(3L);

        assertThat(d.getActive()).isFalse();
        verify(driverRepository).save(d);
        verify(driverRepository, never()).deleteById(any());
    }

    @Test
    void unusedDriver_isDeleted() {
        Driver d = new Driver();
        d.setId(3L);
        when(driverRepository.findById(3L)).thenReturn(Optional.of(d));
        when(tripRepository.existsByDriver_Id(3L)).thenReturn(false);
        when(vehicleRepository.existsByDefaultDriver_Id(3L)).thenReturn(false);

        driverService.delete(3L);

        verify(driverRepository).deleteById(3L);
    }
}
