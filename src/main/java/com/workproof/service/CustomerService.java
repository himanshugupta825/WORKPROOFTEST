package com.workproof.service;

import com.workproof.model.Customer;
import com.workproof.model.User;
import com.workproof.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Customer> getCustomerById(Long id) {
        return customerRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Customer> getCustomerByUser(User user) {
        if (user == null) return Optional.empty();
        return customerRepository.findByUser(user)
                .or(() -> customerRepository.findByEmail(user.getEmail()));
    }

    @Transactional
    public Customer saveCustomer(Customer customer) {
        return customerRepository.save(customer);
    }

    @Transactional(readOnly = true)
    public long countCustomers() {
        return customerRepository.count();
    }
}

