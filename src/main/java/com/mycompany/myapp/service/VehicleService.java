package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.Driver;
import com.mycompany.myapp.domain.Vehicle;
import com.mycompany.myapp.repository.DriverRepository;
import com.mycompany.myapp.repository.OfficeRepository;
import com.mycompany.myapp.repository.VehicleRepository;
import com.mycompany.myapp.service.dto.VehicleDTO;
import com.mycompany.myapp.service.mapper.VehicleMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.mycompany.myapp.domain.Vehicle}.
 */
@Service
@Transactional
public class VehicleService {

    private static final Logger LOG = LoggerFactory.getLogger(VehicleService.class);

    private final VehicleRepository vehicleRepository;

    private final VehicleMapper vehicleMapper;

    private final OfficeRepository officeRepository;

    private final DriverRepository driverRepository;

    public VehicleService(
        VehicleRepository vehicleRepository,
        VehicleMapper vehicleMapper,
        OfficeRepository officeRepository,
        DriverRepository driverRepository
    ) {
        this.vehicleRepository = vehicleRepository;
        this.vehicleMapper = vehicleMapper;
        this.officeRepository = officeRepository;
        this.driverRepository = driverRepository;
    }

    /**
     * Save a vehicle.
     *
     * @param vehicleDTO the entity to save.
     * @return the persisted entity.
     */
    public VehicleDTO save(VehicleDTO vehicleDTO) {
        LOG.debug("Request to save Vehicle : {}", vehicleDTO);
        Vehicle vehicle = vehicleMapper.toEntity(vehicleDTO);
        attachRefs(vehicleDTO, vehicle);
        vehicle = vehicleRepository.save(vehicle);
        return vehicleMapper.toDto(vehicle);
    }

    /**
     * Update a vehicle.
     *
     * @param vehicleDTO the entity to save.
     * @return the persisted entity.
     */
    public VehicleDTO update(VehicleDTO vehicleDTO) {
        LOG.debug("Request to update Vehicle : {}", vehicleDTO);
        Vehicle vehicle = vehicleMapper.toEntity(vehicleDTO);
        attachRefs(vehicleDTO, vehicle);
        vehicle = vehicleRepository.save(vehicle);
        return vehicleMapper.toDto(vehicle);
    }

    /**
     * Partially update a vehicle.
     *
     * @param vehicleDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<VehicleDTO> partialUpdate(VehicleDTO vehicleDTO) {
        LOG.debug("Request to partially update Vehicle : {}", vehicleDTO);

        return vehicleRepository
            .findById(vehicleDTO.getId())
            .map(existingVehicle -> {
                vehicleMapper.partialUpdate(existingVehicle, vehicleDTO);
                attachRefs(vehicleDTO, existingVehicle);

                return existingVehicle;
            })
            .map(vehicleRepository::save)
            .map(vehicleMapper::toDto);
    }

    /**
     * Get all the vehicles.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<VehicleDTO> findAll() {
        LOG.debug("Request to get all Vehicles");
        return vehicleRepository.findAllWithRefs().stream().map(vehicleMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one vehicle by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<VehicleDTO> findOne(Long id) {
        LOG.debug("Request to get Vehicle : {}", id);
        return vehicleRepository.findOneWithRefs(id).map(vehicleMapper::toDto);
    }

    private void attachRefs(VehicleDTO dto, Vehicle vehicle) {
        if (dto.getOffice() != null && dto.getOffice().getId() != null) {
            vehicle.setOffice(officeRepository.getReferenceById(dto.getOffice().getId()));
        } else if (dto.getOffice() != null && dto.getOffice().getCode() != null && !dto.getOffice().getCode().isBlank()) {
            vehicle.setOffice(officeRepository.findOneByCode(dto.getOffice().getCode().trim()).orElse(null));
        } else {
            vehicle.setOffice(null);
        }
        if (dto.getDefaultDriver() != null && dto.getDefaultDriver().getId() != null) {
            vehicle.setDefaultDriver(driverRepository.getReferenceById(dto.getDefaultDriver().getId()));
        } else if (
            dto.getDefaultDriver() != null &&
            dto.getDefaultDriver().getFullName() != null &&
            !dto.getDefaultDriver().getFullName().isBlank()
        ) {
            String name = dto.getDefaultDriver().getFullName().trim();
            vehicle.setDefaultDriver(
                driverRepository
                    .findFirstByFullNameIgnoreCase(name)
                    .orElseGet(() -> {
                        Driver d = new Driver();
                        String code = "VEH-" + Integer.toHexString(name.toLowerCase().hashCode()).toUpperCase();
                        if (code.length() > 30) {
                            code = code.substring(0, 30);
                        }
                        d.setDriverCode(code);
                        d.setFullName(name.length() <= 100 ? name : name.substring(0, 100));
                        d.setActive(true);
                        return driverRepository.save(d);
                    })
            );
        } else {
            vehicle.setDefaultDriver(null);
        }
    }

    /**
     * Delete the vehicle by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete Vehicle : {}", id);
        vehicleRepository.deleteById(id);
    }
}
