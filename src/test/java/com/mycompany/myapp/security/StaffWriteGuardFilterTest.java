package com.mycompany.myapp.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.server.ResponseStatusException;

class StaffWriteGuardFilterTest {

    private final StaffAccessService access = mock(StaffAccessService.class);
    private final StaffWriteGuardFilter filter = new StaffWriteGuardFilter(access);

    private MockHttpServletResponse post(String path) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", path);
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, new MockFilterChain());
        return res;
    }

    @Test
    void manualInvoiceIssue_acceptsDeliveredScreenOrInvoiceScreen() throws Exception {
        MockHttpServletResponse res = post("/api/orders/BC0110ABCD/invoice/issue");
        assertThat(res.getStatus()).isEqualTo(200);
        verify(access).requireScreenWrite(ScreenKey.GIAO_THANH_CONG, ScreenKey.QUAN_LY_HOA_DON);
    }

    @Test
    void manualInvoiceIssue_noGrant_forbidden() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "no"))
            .when(access)
            .requireScreenWrite(ScreenKey.GIAO_THANH_CONG, ScreenKey.QUAN_LY_HOA_DON);
        assertThat(post("/api/orders/BC0110ABCD/invoice/issue").getStatus()).isEqualTo(403);
    }

    @Test
    void invoiceMark_onlyInvoiceScreen() throws Exception {
        post("/api/invoices/mark");
        verify(access).requireScreenWrite(ScreenKey.QUAN_LY_HOA_DON);
    }
}
