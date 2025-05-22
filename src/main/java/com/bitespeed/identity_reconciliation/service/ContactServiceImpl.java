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
        System.out.println("Matching Contacts:");
        matchingContacts.forEach(c -> System.out.println(
                "ID: " + c.getId() + ", Email: " + c.getEmail() + ", Phone: " + c.getPhoneNumber() +
                        ", LinkPrecedence: " + c.getLinkPrecedence() + ", LinkedId: " + c.getLinkedId())
        );

        if (matchingContacts.isEmpty()) {
            Contact newContact = createPrimaryContact(email, phone);
            return buildResponse(newContact, List.of());
        }

        Set<Contact> allRelated = getAllRelatedContacts(matchingContacts);
        System.out.println("All Related Contacts:");
        allRelated.forEach(c -> System.out.println(
                "ID: " + c.getId() + ", Email: " + c.getEmail() + ", Phone: " + c.getPhoneNumber() +
                        ", LinkPrecedence: " + c.getLinkPrecedence() + ", LinkedId: " + c.getLinkedId())
        );

        Contact primary = getPrimaryContact(allRelated);
        System.out.println("Primary Contact: ID = " + primary.getId() + ", Email = " + primary.getEmail() + ", Phone = " + primary.getPhoneNumber());

        boolean isAlreadyPresent = checkIfAlreadyExists(allRelated, email, phone);
        System.out.println("Is already present? " + isAlreadyPresent);

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
                Contact primary = contactRepository.findById(c.getLinkedId()).orElse(null);
                if (primary != null) {
                    all.add(primary);
                }
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
                .orElse(
                        contacts.stream()
                                .min(Comparator.comparing(Contact::getCreatedAt))
                                .orElseThrow(() -> new RuntimeException("No contacts found"))
                );
    }

    private boolean checkIfAlreadyExists(Set<Contact> contacts, String email, String phone) {
        return contacts.stream().anyMatch(c ->
                (email != null && email.equals(c.getEmail())) ||
                        (phone != null && phone.equals(c.getPhoneNumber())));
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
        Set<String> emailsSet = allContacts.stream()
                .map(Contact::getEmail)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<String> phoneNumbersSet = allContacts.stream()
                .map(Contact::getPhoneNumber)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<String> emails = new ArrayList<>(emailsSet);
        List<String> phoneNumbers = new ArrayList<>(phoneNumbersSet);

        // Move primary's email/phone to the front if present
        if (primary.getEmail() != null && emails.remove(primary.getEmail())) {
            emails.add(0, primary.getEmail());
        }
        if (primary.getPhoneNumber() != null && phoneNumbers.remove(primary.getPhoneNumber())) {
            phoneNumbers.add(0, primary.getPhoneNumber());
        }

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
