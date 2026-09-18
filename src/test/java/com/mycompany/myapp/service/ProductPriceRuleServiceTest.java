package com.mycompany.myapp.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import org.junit.jupiter.api.Test;

class ProductPriceRuleServiceTest {

    @Test
    void rejectsReservedGroupKhac() {
        assertThatThrownBy(() -> ProductPriceRuleService.assertGroupNameAllowed("Khác"))
            .isInstanceOf(BadRequestAlertException.class)
            .hasMessageContaining("Khác");
        assertThatThrownBy(() -> ProductPriceRuleService.assertGroupNameAllowed("  khác ")).isInstanceOf(BadRequestAlertException.class);
    }

    @Test
    void allowsConfiguredGroupNames() {
        ProductPriceRuleService.assertGroupNameAllowed("Điện tử");
        ProductPriceRuleService.assertGroupNameAllowed(null);
        ProductPriceRuleService.assertGroupNameAllowed("Thực phẩm");
    }
}
