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
@AllArgsConstructor
public class Customer {
    private String firstName;
    private String lastName;
    private Terms terms;
    
    public String getName() {
    	return firstName + " " + lastName;
    }
    
    @Override
    public String toString() {
    	return "Customer: " + getName();
    }
}
