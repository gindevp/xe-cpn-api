package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.Itinerary;
import com.mycompany.myapp.repository.ItineraryRepository;
import com.mycompany.myapp.service.dto.ItineraryDTO;
import com.mycompany.myapp.service.mapper.ItineraryMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link Itinerary}.
 */
@Service
@Transactional
public class ItineraryService {

    private static final Logger LOG = LoggerFactory.getLogger(ItineraryService.class);

    private final ItineraryRepository itineraryRepository;
    private final ItineraryMapper itineraryMapper;

    public ItineraryService(ItineraryRepository itineraryRepository, ItineraryMapper itineraryMapper) {
        this.itineraryRepository = itineraryRepository;
        this.itineraryMapper = itineraryMapper;
    }

    public ItineraryDTO save(ItineraryDTO itineraryDTO) {
        LOG.debug("Request to save Itinerary : {}", itineraryDTO);
        Itinerary itinerary = itineraryMapper.toEntity(itineraryDTO);
        itinerary = itineraryRepository.save(itinerary);
        return itineraryMapper.toDto(itinerary);
    }

    public ItineraryDTO update(ItineraryDTO itineraryDTO) {
        LOG.debug("Request to update Itinerary : {}", itineraryDTO);
        Itinerary itinerary = itineraryMapper.toEntity(itineraryDTO);
        itinerary = itineraryRepository.save(itinerary);
        return itineraryMapper.toDto(itinerary);
    }

    public Optional<ItineraryDTO> partialUpdate(ItineraryDTO itineraryDTO) {
        LOG.debug("Request to partially update Itinerary : {}", itineraryDTO);
        return itineraryRepository
            .findById(itineraryDTO.getId())
            .map(existing -> {
                itineraryMapper.partialUpdate(existing, itineraryDTO);
                return existing;
            })
            .map(itineraryRepository::save)
            .map(itineraryMapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<ItineraryDTO> findAll(Long branchId, boolean activeOnly) {
        LOG.debug("Request to get Itineraries branchId={} activeOnly={}", branchId, activeOnly);
        return itineraryRepository
            .findFiltered(branchId, activeOnly)
            .stream()
            .map(itineraryMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    @Transactional(readOnly = true)
    public Optional<ItineraryDTO> findOne(Long id) {
        LOG.debug("Request to get Itinerary : {}", id);
        return itineraryRepository.findOneWithBranch(id).map(itineraryMapper::toDto);
    }

    public void delete(Long id) {
        LOG.debug("Request to delete Itinerary : {}", id);
        itineraryRepository.deleteById(id);
    }
}
