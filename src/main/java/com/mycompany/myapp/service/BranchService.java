package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.Branch;
import com.mycompany.myapp.repository.BranchRepository;
import com.mycompany.myapp.service.dto.BranchDTO;
import com.mycompany.myapp.service.mapper.BranchMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link Branch}.
 */
@Service
@Transactional
public class BranchService {

    private static final Logger LOG = LoggerFactory.getLogger(BranchService.class);

    private final BranchRepository branchRepository;
    private final BranchMapper branchMapper;

    public BranchService(BranchRepository branchRepository, BranchMapper branchMapper) {
        this.branchRepository = branchRepository;
        this.branchMapper = branchMapper;
    }

    public BranchDTO save(BranchDTO branchDTO) {
        LOG.debug("Request to save Branch : {}", branchDTO);
        Branch branch = branchMapper.toEntity(branchDTO);
        branch = branchRepository.save(branch);
        return branchMapper.toDto(branch);
    }

    public BranchDTO update(BranchDTO branchDTO) {
        LOG.debug("Request to update Branch : {}", branchDTO);
        Branch branch = branchMapper.toEntity(branchDTO);
        branch = branchRepository.save(branch);
        return branchMapper.toDto(branch);
    }

    public Optional<BranchDTO> partialUpdate(BranchDTO branchDTO) {
        LOG.debug("Request to partially update Branch : {}", branchDTO);
        return branchRepository
            .findById(branchDTO.getId())
            .map(existing -> {
                branchMapper.partialUpdate(existing, branchDTO);
                return existing;
            })
            .map(branchRepository::save)
            .map(branchMapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<BranchDTO> findAll(boolean activeOnly) {
        LOG.debug("Request to get all Branches activeOnly={}", activeOnly);
        List<Branch> list = activeOnly ? branchRepository.findAllByActiveTrueOrderByNameAsc() : branchRepository.findAll();
        return list.stream().map(branchMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    @Transactional(readOnly = true)
    public Optional<BranchDTO> findOne(Long id) {
        LOG.debug("Request to get Branch : {}", id);
        return branchRepository.findById(id).map(branchMapper::toDto);
    }

    public void delete(Long id) {
        LOG.debug("Request to delete Branch : {}", id);
        branchRepository.deleteById(id);
    }
}
