package com.skipq.core.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shared")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @PutMapping("/device-token")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void registerDeviceToken(@AuthenticationPrincipal UserDetails userDetails,
                                    @RequestBody Map<String, String> body) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        userRepository.findById(userId).ifPresent(user -> {
            user.setFcmToken(body.get("token"));
            userRepository.save(user);
        });
    }
}
