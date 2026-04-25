package com.skipq.core.student;

import com.skipq.core.student.dto.StudentMenuResponse;
import com.skipq.core.order.OrderService;
import com.skipq.core.order.dto.OrderResponse;
import com.skipq.core.order.dto.PlaceOrderRequest;
import com.skipq.core.student.dto.StudentSyncResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/student")
@PreAuthorize("hasRole('STUDENT')")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final OrderService orderService;

    @GetMapping("/sync")
    public StudentSyncResponse sync(@AuthenticationPrincipal UserDetails userDetails) {
        return studentService.sync(userId(userDetails));
    }

    @GetMapping("/menu/{vendorId}")
    public StudentMenuResponse getMenu(@PathVariable UUID vendorId) {
        return studentService.getAvailableMenu(vendorId);
    }

    @PostMapping("/orders")
    public OrderResponse placeOrder(@AuthenticationPrincipal UserDetails userDetails,
                                    @Valid @RequestBody PlaceOrderRequest request) {
        return orderService.placeOrder(userId(userDetails), request);
    }

    @DeleteMapping("/account")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(@AuthenticationPrincipal UserDetails userDetails) {
        studentService.deleteAccount(userId(userDetails));
    }

    private UUID userId(UserDetails userDetails) {
        return UUID.fromString(userDetails.getUsername());
    }
}
