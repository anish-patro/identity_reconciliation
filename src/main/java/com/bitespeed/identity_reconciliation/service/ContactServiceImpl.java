package com.bitespeed.identity_reconciliation.service;

import com.bitespeed.identity_reconciliation.dto.ContactRequest;
import com.bitespeed.identity_reconciliation.dto.ContactResponse;
import com.bitespeed.identity_reconciliation.enums.LinkPrecedence;
import com.bitespeed.identity_reconciliation.model.Contact;
import com.bitespeed.identity_reconciliation.repository.ContactRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {
    private final ContactRepository contactRepository;

    public ContactResponse identifyContact(ContactRequest contactRequest) {
        String email = contactRequest.getEmail();
        String phone = contactRequest.getPhoneNumber();
        List<Contact> matchingContacts = contactRepository.findByEmailOrPhoneNumber(email, phone);
        if (matchingContacts.isEmpty()) {
            Contact newContact = createPrimaryContact(email, phone);
            return buildResponse(newContact, List.of());
        }
        Set<Contact> allRelated = getAllRelatedContacts(matchingContacts);
        Contact primary = getPrimaryContact(allRelated);
        boolean isAlreadyPresent = checkIfAlreadyExists(allRelated, email, phone);
        if (!isAlreadyPresent) {
            Contact newSecondary = createSecondaryContact(email, phone, primary.getId());
            allRelated.add(newSecondary);
        }
        updateLinkedContacts(allRelated, primary);
        return buildResponse(primary, allRelated);
    }

    private Contact createPrimaryContact(String email, String phone) {
        Contact newContact = Contact.builder()
                .email(email)
                .phoneNumber(phone)
                .linkPrecedence(LinkPrecedence.PRIMARY)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return contactRepository.save(newContact);
    }

    private Contact createSecondaryContact(String email, String phone, Integer primaryId) {
        Contact newSecondary = Contact.builder()
                .email(email)
                .phoneNumber(phone)
                .linkPrecedence(LinkPrecedence.SECONDARY)
                .linkedId(primaryId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return contactRepository.save(newSecondary);
    }

    private Set<Contact> getAllRelatedContacts(List<Contact> initialMatches) {
        Set<Contact> all = new HashSet<>(initialMatches);
        for (Contact c : initialMatches) {
            if (c.getLinkedId() != null) {
                all.addAll(contactRepository.findByLinkedId(c.getLinkedId()));
            } else {
                all.addAll(contactRepository.findByLinkedId(c.getId()));
            }
        }
        return all;
    }

    private Contact getPrimaryContact(Set<Contact> contacts) {
        return contacts.stream()
                .filter(c -> c.getLinkPrecedence() == LinkPrecedence.PRIMARY)
                .min(Comparator.comparing(Contact::getCreatedAt))
                .orElseThrow();
    }

    private boolean checkIfAlreadyExists(Set<Contact> contacts, String email, String phone) {
        return contacts.stream().anyMatch(c ->
                Objects.equals(c.getEmail(), email) && Objects.equals(c.getPhoneNumber(), phone));
    }

    private void updateLinkedContacts(Set<Contact> contacts, Contact primary) {
        for (Contact contact : contacts) {
            if (!contact.getId().equals(primary.getId()) &&
                    (!Objects.equals(contact.getLinkedId(), primary.getId()) ||
                            contact.getLinkPrecedence() != LinkPrecedence.SECONDARY)) {

                contact.setLinkPrecedence(LinkPrecedence.SECONDARY);
                contact.setLinkedId(primary.getId());
                contact.setUpdatedAt(LocalDateTime.now());
                contactRepository.save(contact);
            }
        }
    }

    private ContactResponse buildResponse(Contact primary, Collection<Contact> allContacts) {
        List<String> emails = allContacts.stream()
                .map(Contact::getEmail)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        List<String> phoneNumbers = allContacts.stream()
                .map(Contact::getPhoneNumber)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        List<Integer> secondaryIds = allContacts.stream()
                .filter(c -> !c.getId().equals(primary.getId()))
                .map(Contact::getId)
                .distinct()
                .collect(Collectors.toList());

        return ContactResponse.builder()
                .contact(ContactResponse.ContactDTO.builder()
                        .primaryContactId(primary.getId())
                        .emails(emails)
                        .phoneNumbers(phoneNumbers)
                        .secondaryContactIds(secondaryIds)
                        .build())
                .build();
    }
}
