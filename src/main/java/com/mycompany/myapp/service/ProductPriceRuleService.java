package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.ProductPriceRule;
import com.mycompany.myapp.repository.ProductPriceRuleRepository;
import com.mycompany.myapp.service.dto.ProductPriceRuleDTO;
import com.mycompany.myapp.service.mapper.ProductPriceRuleMapper;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.mycompany.myapp.domain.ProductPriceRule}.
 */
@Service
@Transactional
public class ProductPriceRuleService {

    private static final Logger LOG = LoggerFactory.getLogger(ProductPriceRuleService.class);

    private final ProductPriceRuleRepository productPriceRuleRepository;

    private final ProductPriceRuleMapper productPriceRuleMapper;

    public ProductPriceRuleService(ProductPriceRuleRepository productPriceRuleRepository, ProductPriceRuleMapper productPriceRuleMapper) {
        this.productPriceRuleRepository = productPriceRuleRepository;
        this.productPriceRuleMapper = productPriceRuleMapper;
    }

    /**
     * Save a productPriceRule.
     *
     * @param productPriceRuleDTO the entity to save.
     * @return the persisted entity.
     */
    public ProductPriceRuleDTO save(ProductPriceRuleDTO productPriceRuleDTO) {
        LOG.debug("Request to save ProductPriceRule : {}", productPriceRuleDTO);
        assertGroupNameAllowed(productPriceRuleDTO.getGroupName());
        ProductPriceRule productPriceRule = productPriceRuleMapper.toEntity(productPriceRuleDTO);
        productPriceRule = productPriceRuleRepository.save(productPriceRule);
        return productPriceRuleMapper.toDto(productPriceRule);
    }

    /**
     * Update a productPriceRule.
     *
     * @param productPriceRuleDTO the entity to save.
     * @return the persisted entity.
     */
    public ProductPriceRuleDTO update(ProductPriceRuleDTO productPriceRuleDTO) {
        LOG.debug("Request to update ProductPriceRule : {}", productPriceRuleDTO);
        assertGroupNameAllowed(productPriceRuleDTO.getGroupName());
        ProductPriceRule productPriceRule = productPriceRuleMapper.toEntity(productPriceRuleDTO);
        productPriceRule = productPriceRuleRepository.save(productPriceRule);
        return productPriceRuleMapper.toDto(productPriceRule);
    }

    /**
     * Partially update a productPriceRule.
     *
     * @param productPriceRuleDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<ProductPriceRuleDTO> partialUpdate(ProductPriceRuleDTO productPriceRuleDTO) {
        LOG.debug("Request to partially update ProductPriceRule : {}", productPriceRuleDTO);

        return productPriceRuleRepository
            .findById(productPriceRuleDTO.getId())
            .map(existingProductPriceRule -> {
                productPriceRuleMapper.partialUpdate(existingProductPriceRule, productPriceRuleDTO);
                assertGroupNameAllowed(existingProductPriceRule.getGroupName());
                return existingProductPriceRule;
            })
            .map(productPriceRuleRepository::save)
            .map(productPriceRuleMapper::toDto);
    }

    /**
     * Get all the productPriceRules.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<ProductPriceRuleDTO> findAll() {
        LOG.debug("Request to get all ProductPriceRules");
        return productPriceRuleRepository
            .findAll()
            .stream()
            .map(productPriceRuleMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one productPriceRule by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<ProductPriceRuleDTO> findOne(Long id) {
        LOG.debug("Request to get ProductPriceRule : {}", id);
        return productPriceRuleRepository.findById(id).map(productPriceRuleMapper::toDto);
    }

    /**
     * Delete the productPriceRule by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete ProductPriceRule : {}", id);
        productPriceRuleRepository.deleteById(id);
    }

    /**
     * "Khác" is the built-in create-order type for custom product names — not a configurable group.
     */
    static void assertGroupNameAllowed(String groupName) {
        if (groupName != null && groupName.trim().equalsIgnoreCase("Khác")) {
            throw new BadRequestAlertException("Nhóm hàng Khác trùng loại nhập theo tên khi tạo đơn", "productPriceRule", "reservedGroup");
        }
    }
}
