package com.bitespeed.identity_reconciliation.controller;

import com.bitespeed.identity_reconciliation.dto.ContactRequest;
import com.bitespeed.identity_reconciliation.dto.ContactResponse;
import com.bitespeed.identity_reconciliation.service.ContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/identify")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @PostMapping
    public ResponseEntity<?> identify(@RequestBody ContactRequest request) {
        String email = request.getEmail();
        String phone = request.getPhoneNumber();

        if ((email == null || email.isBlank()) && (phone == null || phone.isBlank())) {
            return ResponseEntity.badRequest().body("At least email or phoneNumber must be provided");
        }

        ContactResponse response = contactService.identifyContact(request);
        return ResponseEntity.ok(response);
    }
}
