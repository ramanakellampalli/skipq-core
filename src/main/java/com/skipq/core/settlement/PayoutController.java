package com.skipq.core.settlement;

import com.skipq.core.settlement.dto.MarkPayoutSuccessRequest;
import com.skipq.core.settlement.dto.VendorPayoutResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/payouts")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class PayoutController {

    private final VendorPayoutRepository vendorPayoutRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final VendorLedgerRepository vendorLedgerRepository;

    @GetMapping
    public List<VendorPayoutResponse> listPayouts(
            @RequestParam(required = false) PayoutStatus status) {
        List<VendorPayout> payouts = status != null
                ? vendorPayoutRepository.findByStatusWithVendor(status)
                : vendorPayoutRepository.findAllWithVendor();
        return payouts.stream().map(VendorPayoutResponse::from).toList();
    }

    @PutMapping("/{id}/success")
    @Transactional
    public VendorPayoutResponse markSuccess(
            @PathVariable UUID id,
            @Valid @RequestBody MarkPayoutSuccessRequest request) {

        VendorPayout payout = vendorPayoutRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payout not found"));

        if (payout.getStatus() != PayoutStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Payout is already " + payout.getStatus());
        }

        payout.setStatus(PayoutStatus.SUCCESS);
        payout.setPayoutReference(request.payoutReference());
        payout.setAdminNote(request.adminNote());
        vendorPayoutRepository.save(payout);

        ledgerEntryRepository.markSettled(payout.getId());
        vendorLedgerRepository.upsertBalance(payout.getVendor().getId(), payout.getAmount().negate());

        return VendorPayoutResponse.from(payout);
    }

    @PutMapping("/{id}/failed")
    @Transactional
    public VendorPayoutResponse markFailed(
            @PathVariable UUID id,
            @RequestParam(required = false) String adminNote) {

        VendorPayout payout = vendorPayoutRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payout not found"));

        if (payout.getStatus() != PayoutStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Payout is already " + payout.getStatus());
        }

        payout.setStatus(PayoutStatus.FAILED);
        payout.setAdminNote(adminNote);
        vendorPayoutRepository.save(payout);

        ledgerEntryRepository.releaseReservation(payout.getId());

        return VendorPayoutResponse.from(payout);
    }
}
