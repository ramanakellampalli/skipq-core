package com.skipq.core.discount;

import com.skipq.core.discount.dto.AttachItemsRequest;
import com.skipq.core.discount.dto.CreateDiscountRequest;
import com.skipq.core.discount.dto.DiscountResponse;
import com.skipq.core.discount.dto.UpdateDiscountRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscountControllerTest {

    @Mock DiscountService discountService;
    @InjectMocks DiscountController controller;

    private UserDetails userWith(UUID id) {
        UserDetails ud = mock(UserDetails.class);
        when(ud.getUsername()).thenReturn(id.toString());
        return ud;
    }

    private DiscountResponse stubResponse(UUID id) {
        return new DiscountResponse(
                id, "Summer Sale", DiscountType.PERCENTAGE, new BigDecimal("10"),
                DiscountScope.ITEM, true, 0, null, null, 0, LocalDateTime.now()
        );
    }

    // ── list ──────────────────────────────────────────────────────────────────

    @Test
    void list_delegatesToService_returnsResponses() {
        UUID userId = UUID.randomUUID();
        DiscountResponse resp = stubResponse(UUID.randomUUID());
        when(discountService.list(userId)).thenReturn(List.of(resp));

        List<DiscountResponse> result = controller.list(userWith(userId));

        assertThat(result).containsExactly(resp);
        verify(discountService).list(userId);
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void create_delegatesToService_returnsCreatedResponse() {
        UUID userId = UUID.randomUUID();
        var req  = new CreateDiscountRequest("Flash", DiscountType.PERCENTAGE, new BigDecimal("15"), null, null);
        DiscountResponse resp = stubResponse(UUID.randomUUID());
        when(discountService.create(userId, req)).thenReturn(resp);

        DiscountResponse result = controller.create(userWith(userId), req);

        assertThat(result).isEqualTo(resp);
        verify(discountService).create(userId, req);
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    void update_delegatesToService_returnsUpdatedResponse() {
        UUID userId     = UUID.randomUUID();
        UUID discountId = UUID.randomUUID();
        var req  = new UpdateDiscountRequest("New Name", null, null, null);
        DiscountResponse resp = stubResponse(discountId);
        when(discountService.update(userId, discountId, req)).thenReturn(resp);

        DiscountResponse result = controller.update(userWith(userId), discountId, req);

        assertThat(result).isEqualTo(resp);
        verify(discountService).update(userId, discountId, req);
    }

    // ── attachItems ───────────────────────────────────────────────────────────

    @Test
    void attachItems_delegatesToService() {
        UUID userId     = UUID.randomUUID();
        UUID discountId = UUID.randomUUID();
        UUID itemId     = UUID.randomUUID();
        var req = new AttachItemsRequest(List.of(itemId));

        controller.attachItems(userWith(userId), discountId, req);

        verify(discountService).attachItems(userId, discountId, req);
    }

    // ── detachItem ────────────────────────────────────────────────────────────

    @Test
    void detachItem_delegatesToService() {
        UUID userId     = UUID.randomUUID();
        UUID discountId = UUID.randomUUID();
        UUID itemId     = UUID.randomUUID();

        controller.detachItem(userWith(userId), discountId, itemId);

        verify(discountService).detachItem(userId, discountId, itemId);
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_delegatesToService() {
        UUID userId     = UUID.randomUUID();
        UUID discountId = UUID.randomUUID();

        controller.delete(userWith(userId), discountId);

        verify(discountService).delete(userId, discountId);
    }
}
