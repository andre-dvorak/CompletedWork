package com.amica.billing;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.util.Optional;

@Data
@AllArgsConstructor
public class Invoice {

    private int number;
    private double amount;
    private LocalDate invoiceDate;
    private Optional<LocalDate> paidDate;
    private Customer customer;

}
