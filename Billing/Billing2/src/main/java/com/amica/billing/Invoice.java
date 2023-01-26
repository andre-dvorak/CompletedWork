package com.amica.billing;

import java.time.LocalDate;
import java.util.Optional;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Simple JavaBean representing an invoice.
 *
 * @author Will Provost
 */
@Data
@EqualsAndHashCode(of="number")
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {
	private int number;
    private Customer customer;
    private double amount;
    private LocalDate issueDate;
    private Optional<LocalDate> paidDate;
    
    public Invoice(int number, Customer customer, double amount, 
    		LocalDate issueDate) {
    	this(number, customer, amount, issueDate, Optional.empty());
    }
    
    public Invoice(int number, Customer customer, double amount, 
    		LocalDate issueDate, LocalDate paidDate) {
    	this(number, customer, amount, issueDate, Optional.of(paidDate));
    }
    
    public LocalDate getDueDate() {
    	int daysAllowed = customer.getTerms().getDays();
    	return issueDate.plusDays(daysAllowed);
    }
    
    public boolean isOverdue(LocalDate asOf) {
		LocalDate endDate = paidDate.orElse(asOf);
		return endDate.isAfter(getDueDate());		
    }

    @Override
    public String toString() {
    	return "Invoice " + number;
    }
}
