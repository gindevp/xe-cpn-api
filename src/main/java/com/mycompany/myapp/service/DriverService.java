package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.Driver;
import com.mycompany.myapp.repository.DriverRepository;
import com.mycompany.myapp.repository.TripRepository;
import com.mycompany.myapp.repository.VehicleRepository;
import com.mycompany.myapp.service.dto.DriverDTO;
import com.mycompany.myapp.service.mapper.DriverMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.mycompany.myapp.domain.Driver}.
 */
@Service
@Transactional
public class DriverService {

    private static final Logger LOG = LoggerFactory.getLogger(DriverService.class);

    private final DriverRepository driverRepository;

    private final DriverMapper driverMapper;

    private final TripRepository tripRepository;

    private final VehicleRepository vehicleRepository;

    public DriverService(
        DriverRepository driverRepository,
        DriverMapper driverMapper,
        TripRepository tripRepository,
        VehicleRepository vehicleRepository
    ) {
        this.driverRepository = driverRepository;
        this.driverMapper = driverMapper;
        this.tripRepository = tripRepository;
        this.vehicleRepository = vehicleRepository;
    }

    /**
     * Save a driver.
     *
     * @param driverDTO the entity to save.
     * @return the persisted entity.
     */
    public DriverDTO save(DriverDTO driverDTO) {
        LOG.debug("Request to save Driver : {}", driverDTO);
        if (driverDTO.getId() == null && driverDTO.getFullName() != null) {
            Optional<Driver> hidden = driverRepository
                .findFirstByFullNameIgnoreCase(driverDTO.getFullName().trim())
                .filter(d -> Boolean.FALSE.equals(d.getActive()));
            if (hidden.isPresent()) {
                hidden.get().setActive(true);
                return driverMapper.toDto(driverRepository.save(hidden.get()));
            }
        }
        Driver driver = driverMapper.toEntity(driverDTO);
        driver = driverRepository.save(driver);
        return driverMapper.toDto(driver);
    }

    /**
     * Update a driver.
     *
     * @param driverDTO the entity to save.
     * @return the persisted entity.
     */
    public DriverDTO update(DriverDTO driverDTO) {
        LOG.debug("Request to update Driver : {}", driverDTO);
        Driver driver = driverMapper.toEntity(driverDTO);
        driver = driverRepository.save(driver);
        return driverMapper.toDto(driver);
    }

    /**
     * Partially update a driver.
     *
     * @param driverDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<DriverDTO> partialUpdate(DriverDTO driverDTO) {
        LOG.debug("Request to partially update Driver : {}", driverDTO);

        return driverRepository
            .findById(driverDTO.getId())
            .map(existingDriver -> {
                driverMapper.partialUpdate(existingDriver, driverDTO);

                return existingDriver;
            })
            .map(driverRepository::save)
            .map(driverMapper::toDto);
    }

    /**
     * Get all the drivers.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<DriverDTO> findAll() {
        LOG.debug("Request to get all Drivers");
        return driverRepository.findAll().stream().map(driverMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one driver by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<DriverDTO> findOne(Long id) {
        LOG.debug("Request to get Driver : {}", id);
        return driverRepository.findById(id).map(driverMapper::toDto);
    }

    /**
     * Delete the driver by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete Driver : {}", id);
        Optional<Driver> existing = driverRepository.findById(id);
        if (existing.isPresent() && (tripRepository.existsByDriver_Id(id) || vehicleRepository.existsByDefaultDriver_Id(id))) {
            // Chuyến / xe còn tham chiếu tài xế (FK) — ẩn khỏi danh sách thay vì xoá.
            Driver d = existing.get();
            d.setActive(false);
            driverRepository.save(d);
            return;
        }
        driverRepository.deleteById(id);
    }
}
