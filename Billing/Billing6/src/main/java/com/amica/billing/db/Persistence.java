package com.amica.billing.db;

import java.util.Map;

import com.amica.billing.Customer;
import com.amica.billing.Invoice;

/**
 * Strategy for loading and saving data.
 * 
 * @author Will Provost
 */
public interface Persistence {
	
	/**
	 * Returns a map of all customers, keyed by name.
	 */
	public Map<String,Customer> getCustomers();
	
	/**
	 * returns a map of all invoices, keyed by number.
	 */
	public Map<Integer,Invoice> getInvoices();
	
	/**
	 * Updates a customer with the same name, or inserts a new customer.
	 */
	public void saveCustomer(Customer customer);
	
	/**
	 * Updates an invoice with the same number, or inserts a new invoice.
	 */
	public void saveInvoice(Invoice invoice);
}
