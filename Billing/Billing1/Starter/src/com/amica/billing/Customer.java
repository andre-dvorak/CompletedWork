package com.amica.billing;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Customer {

    private String firstName;
    private String lastName;
    private Terms terms;

    public String getName(){
        return firstName + " " + lastName;
    }

}
