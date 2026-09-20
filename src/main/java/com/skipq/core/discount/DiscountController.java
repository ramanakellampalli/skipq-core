package com.skipq.core.discount;

import com.skipq.core.discount.dto.AttachItemsRequest;
import com.skipq.core.discount.dto.CreateDiscountRequest;
import com.skipq.core.discount.dto.DiscountResponse;
import com.skipq.core.discount.dto.UpdateDiscountRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('VENDOR')")
public class DiscountController {

    private final DiscountService discountService;

    @GetMapping("/api/v1/vendor/discounts")
    public List<DiscountResponse> list(@AuthenticationPrincipal UserDetails userDetails) {
        return discountService.list(userId(userDetails));
    }

    @PostMapping("/api/v1/vendor/discounts")
    @ResponseStatus(HttpStatus.CREATED)
    public DiscountResponse create(@AuthenticationPrincipal UserDetails userDetails,
                                   @Valid @RequestBody CreateDiscountRequest req) {
        return discountService.create(userId(userDetails), req);
    }

    @PatchMapping("/api/v1/vendor/discounts/{id}")
    public DiscountResponse update(@AuthenticationPrincipal UserDetails userDetails,
                                   @PathVariable UUID id,
                                   @RequestBody UpdateDiscountRequest req) {
        return discountService.update(userId(userDetails), id, req);
    }

    @PostMapping("/api/v1/vendor/discounts/{id}/items")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void attachItems(@AuthenticationPrincipal UserDetails userDetails,
                            @PathVariable UUID id,
                            @Valid @RequestBody AttachItemsRequest req) {
        discountService.attachItems(userId(userDetails), id, req);
    }

    @DeleteMapping("/api/v1/vendor/discounts/{id}/items/{menuItemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void detachItem(@AuthenticationPrincipal UserDetails userDetails,
                           @PathVariable UUID id,
                           @PathVariable UUID menuItemId) {
        discountService.detachItem(userId(userDetails), id, menuItemId);
    }

    @DeleteMapping("/api/v1/vendor/discounts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserDetails userDetails,
                       @PathVariable UUID id) {
        discountService.delete(userId(userDetails), id);
    }

    private UUID userId(UserDetails userDetails) {
        return UUID.fromString(userDetails.getUsername());
    }
}
