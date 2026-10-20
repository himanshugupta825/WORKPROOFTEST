package com.workproof.repository;

import com.workproof.model.Customer;
import com.workproof.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByEmail(String email);
    Optional<Customer> findByUser(User user);
    Optional<Customer> findByUserId(Integer userId);
}

