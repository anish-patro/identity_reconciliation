package com.bitespeed.identity_reconciliation.repository;

import com.bitespeed.identity_reconciliation.model.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ContactRepository extends JpaRepository<Contact, Integer> {
    List<Contact> findByEmail(String email);

    List<Contact> findByPhoneNumber(String phoneNumber);

    @Query("SELECT c FROM Contact c WHERE (:email IS NULL OR c.email = :email) OR (:phone IS NULL OR c.phoneNumber = :phone)")
    List<Contact> findByEmailOrPhoneNumber(@Param("email") String email, @Param("phone") String phone);

    List<Contact> findByLinkedId(Integer linkedId);
}
