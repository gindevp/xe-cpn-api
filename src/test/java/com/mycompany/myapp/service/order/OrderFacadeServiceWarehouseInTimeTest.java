package com.mycompany.myapp.service.order;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OrderFacadeServiceWarehouseInTimeTest {

    @Test
    void customerCreateIsNotWarehouseIn() {
        assertThat(OrderFacadeService.warehouseInActions("customer")).doesNotContain("CREATE");
        assertThat(OrderFacadeService.warehouseInActions("CUSTOMER")).doesNotContain("CREATE");
    }

    @Test
    void staffCreateStillCountsAsWarehouseIn() {
        assertThat(OrderFacadeService.warehouseInActions("xe01495")).contains("CREATE");
        assertThat(OrderFacadeService.warehouseInActions(null)).contains("CREATE");
    }
}
