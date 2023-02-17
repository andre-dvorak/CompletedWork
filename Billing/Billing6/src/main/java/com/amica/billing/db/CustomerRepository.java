package com.amica.billing.db;

import com.amica.billing.Customer;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.stream.Stream;

public interface CustomerRepository extends PagingAndSortingRepository<Customer, String>{

    public Stream<Customer> streamAllBy();
    public Customer findByFirstNameAndLastName(String firstName, String lastName);

}
