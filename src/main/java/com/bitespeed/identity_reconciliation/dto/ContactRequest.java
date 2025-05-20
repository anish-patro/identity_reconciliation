package com.bitespeed.identity_reconciliation.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactRequest {
    private String email;
    private String phoneNumber;
}
