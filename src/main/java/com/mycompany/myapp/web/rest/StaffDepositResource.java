package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.service.finance.StaffDepositService;
import com.mycompany.myapp.service.finance.StaffDepositService.DepositAccount;
import com.mycompany.myapp.service.finance.StaffDepositService.MyCandidate;
import com.mycompany.myapp.service.finance.StaffDepositService.MyReceipt;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** App NV: Nộp tiền (tự lập phiếu thu của mình + QR VietQR + ảnh chuyển khoản). Web: cấu hình tài khoản nhận. */
@RestController
public class StaffDepositResource {

    private final StaffDepositService staffDepositService;

    public StaffDepositResource(StaffDepositService staffDepositService) {
        this.staffDepositService = staffDepositService;
    }

    @GetMapping("/api/my-deposits/candidates")
    public List<MyCandidate> candidates() {
        return staffDepositService.myCandidates();
    }

    public record CreateRequest(List<String> orderCodes) {}

    @PostMapping("/api/my-deposits")
    @ResponseStatus(HttpStatus.CREATED)
    public MyReceipt create(@RequestBody(required = false) CreateRequest body) {
        return staffDepositService.createMyReceipt(body == null ? null : body.orderCodes());
    }

    @GetMapping("/api/my-deposits")
    public List<MyReceipt> list() {
        return staffDepositService.myReceipts();
    }

    public record ProofRequest(String image) {}

    @PostMapping("/api/my-deposits/{receiptCode}/transfer-proof")
    public MyReceipt uploadProof(@PathVariable String receiptCode, @RequestBody(required = false) ProofRequest body) {
        return staffDepositService.uploadTransferProof(receiptCode, body == null ? null : body.image());
    }

    @GetMapping("/api/my-deposits/{receiptCode}/transfer-proof")
    public Map<String, String> proof(@PathVariable String receiptCode) {
        String image = staffDepositService.myProofImage(receiptCode);
        return image == null ? Map.of() : Map.of("image", image);
    }

    @GetMapping("/api/admin/deposit-account")
    public DepositAccount getAccount() {
        return staffDepositService.getAccount();
    }

    @PutMapping("/api/admin/deposit-account")
    public DepositAccount putAccount(@RequestBody(required = false) DepositAccount body) {
        return staffDepositService.putAccount(body);
    }
}
