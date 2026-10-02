package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.service.invoice.TaxCodeLookupService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Tra tên / địa chỉ doanh nghiệp theo MST — điền sẵn form xuất hoá đơn. */
@RestController
@RequestMapping("/api/tax-codes")
public class TaxCodeLookupResource {

    private final TaxCodeLookupService taxCodeLookupService;

    public TaxCodeLookupResource(TaxCodeLookupService taxCodeLookupService) {
        this.taxCodeLookupService = taxCodeLookupService;
    }

    @GetMapping("/{taxCode}")
    public Map<String, Object> lookup(@PathVariable("taxCode") String taxCode) {
        return taxCodeLookupService.lookup(taxCode);
    }
}
