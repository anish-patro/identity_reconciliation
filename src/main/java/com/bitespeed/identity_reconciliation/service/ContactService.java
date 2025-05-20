package com.bitespeed.identity_reconciliation.service;

import com.bitespeed.identity_reconciliation.dto.ContactRequest;
import com.bitespeed.identity_reconciliation.dto.ContactResponse;

public interface ContactService {
    ContactResponse identifyContact(ContactRequest request);
}
