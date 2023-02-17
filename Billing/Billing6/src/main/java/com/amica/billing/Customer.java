package com.amica.billing;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Simple JavaBean representing a customer.
 *
 * @author Will Provost
 */
@Getter
@EqualsAndHashCode(of={"firstName", "lastName"})
@NoArgsConstructor
public class Customer {
    private String _id;
    private String firstName;
    private String lastName;
    private Terms terms;

    public Customer(String firstName, String lastName, Terms terms){
        this.firstName = firstName;
        this.lastName = lastName;
        this.terms = terms;
    }

    public String getName() {
    	return firstName + " " + lastName;
    }
    
    @Override
    public String toString() {
    	return "Customer: " + getName();
    }
}
