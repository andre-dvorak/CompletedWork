package com.amica.billing;

import static java.util.function.Function.identity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import com.amica.billing.db.Persistence;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Offers a few useful queries on the loaded data;
 *  and allows a few modifications to the data set, which will be 
 *  saved back to the original data files.
 * 
 * @author Will Provost
 */
@Component
public class Billing {

	/**
	 * Reusable comparator of invoices by number.
	 */
	public static Comparator<Invoice> compareByNumber =
			Comparator.comparing(Invoice::getNumber);
	
	/**
	 * Reusable comparator of invoices by issue date.
	 */
	public static Comparator<Invoice> compareByDate =
			Comparator.comparing(Invoice::getIssueDate);
			
	private Persistence persistence;
	private List<Consumer<Customer>> customerListeners = new ArrayList<>();
	private List<Consumer<Invoice>> invoiceListeners = new ArrayList<>();
	
	/**
	 * Provide a persistence service, and we rely on that for 
	 * loading and saving customers and vinvoices. 
	 */
	public Billing(Persistence persistence) {
		this.persistence = persistence;
	}
	
	
	/**
	 * Add a listener for changes to customers.
	 */
	public synchronized void addCustomerListener(Consumer<Customer> listener) {
		customerListeners.add(listener);
	}
	
	/**
	 * Remove a registered listener for changes to customers.
	 */
	public synchronized void removeCustomerListener(Consumer<Customer> listener) {
		customerListeners.remove(listener);
	}
	
	/**
	 * Add a listener for changes to invoices.
	 */
	public synchronized void addInvoiceListener(Consumer<Invoice> listener) {
		invoiceListeners.add(listener);
	}
	
	/**
	 * Remove a registered listener for changes to invoices.
	 */
	public synchronized void removeInvoiceListener(Consumer<Invoice> listener) {
		invoiceListeners.remove(listener);
	}
	
	/**
	 * Create the new customer as given, save customers, and fire
	 * the customer-change event.
	 */
	public void createCustomer(String firstName, String lastName, Terms terms) {
		Customer customer = new Customer(firstName, lastName, terms);
		if (!getCustomers().containsKey(customer.getName())) {
			persistence.saveCustomer(customer);
			synchronized(this) {
				for (Consumer<Customer> listener : customerListeners) {
					listener.accept(customer);
				}
			}
		} else {
			throw new IllegalArgumentException
				("There is already a customer with the name " + 
					customer.getName());
		}
	}

	/**
	 * Create an invoice with the given data and add it to the set.
	 * Invoice number is generated; invoice date is assumed to be today.
	 * Save invoices and fire the invoice-change event.
	 */
	public Invoice createInvoice(String customerName, double amount) {
		
		int nextInvoiceNumber = getInvoices().keySet().stream()
				.mapToInt(Integer::intValue).max().orElse(0) + 1;

		if (getCustomers().containsKey(customerName)) {
			Invoice invoice = new Invoice(nextInvoiceNumber, 
					getCustomers().get(customerName), amount, 
					LocalDate.now());
			persistence.saveInvoice(invoice);
			synchronized(this) {
				for (Consumer<Invoice> listener : invoiceListeners) {
					listener.accept(invoice);
				}
			}
			return invoice;
		} else {
			throw new IllegalArgumentException("No such customer: " + customerName);
		}
	}

	/**
	 * Set today's date as the paid date for the invoice with the given number.
	 * Save invoices and fire the invoice-change event.
	 */
	public void payInvoice(int invoiceNumber) {
		
		if (getInvoices().containsKey(invoiceNumber)) {
			Invoice invoice = getInvoices().get(invoiceNumber);
			if (!invoice.getPaidDate().isPresent()) {
				invoice.setPaidDate(Optional.of(LocalDate.now()));
				persistence.saveInvoice(invoice);
				synchronized(this) {
					for (Consumer<Invoice> listener : invoiceListeners) {
						listener.accept(invoice);
					}
				}
			} else {
				throw new IllegalStateException("Invoice " + invoiceNumber + 
						" has already been paid.");
			}
		} else {
			throw new IllegalArgumentException("No such invoice: " + invoiceNumber);
		}
	}

	/**
	 * Return the raw map of customers.
	 */
	public Map<String,Customer> getCustomers() {
		return Collections.unmodifiableMap(persistence.getCustomers());
	}
	
	/**
	 * Return the raw list of invoices.
	 */
	public Map<Integer,Invoice> getInvoices() {
		return Collections.unmodifiableMap(persistence.getInvoices());
	}
	
	/**
	 * Return a stream of all invoices, sorted by number.
	 */
	public Stream<Invoice> getInvoicesOrderedByNumber() {
		return getInvoices().values().stream().sorted(compareByNumber);
	}
	
	/**
	 * Return a stream of all invoices, sorted by issue date.
	 */
	public Stream<Invoice> getInvoicesOrderedByDate() {
		return getInvoices().values().stream().sorted(compareByDate);
	}
	
	/**
	 * Return a stream of all invoices for a given customer.
	 */
	public Stream<Invoice> getInvoicesForCustomer(Customer customer) {
		return getInvoices().values().stream()
				.filter(inv -> inv.getCustomer().equals(customer))
				.sorted(compareByNumber);
	}
	
	/**
	 * Return a map of customers and their invoices, sorted by number.
	 */
	public Map<Customer,List<Invoice>> getInvoicesGroupedByCustomer() {
		return getInvoices().values().stream().sorted(compareByNumber)
			.collect(Collectors.groupingBy(Invoice::getCustomer, 
				Collectors.toList()));
	}
	
	/**
	 * Return a stream of overdue invoices, sorted by issue date.
	 */
	public Stream<Invoice> getOverdueInvoices(LocalDate asOf) {
		return getInvoices().values().stream()
				.filter(inv -> inv.isOverdue(asOf))
				.sorted(compareByDate);
	}
	
	/**
	 * Return the volume of business (sum of all invoices) for a customer. 
	 */
	public double getVolumeForCustomer(Customer customer) {
		return getInvoicesForCustomer(customer)
				.mapToDouble(Invoice::getAmount)
				.sum();
	}
	
	/**
	 * Return a map of customers and their volumes of business.
	 * The map is not sorted. 
	 */
	public Map<Customer,Double> getCustomersAndVolume() {
		return getCustomers().values().stream().collect(Collectors.toMap
			(identity(), this::getVolumeForCustomer));
	}
	
	/**
	 * This class encapsultes a customer and their volume of business,
	 * and implements the Comparable interface to sort objects of this type
	 * in descending order of volume.
	 */
	@Data
	@EqualsAndHashCode(of="customer")
	public class CustomerAndVolume implements Comparable<CustomerAndVolume>{
		private Customer customer;
		private double volume;
		
		public CustomerAndVolume(Customer customer) {
			this.customer = customer;
			this.volume = getVolumeForCustomer(customer);
		}
		
		public int compareTo(CustomerAndVolume other) {
			return -Double.compare(volume, other.getVolume());
		}
	}
	
	/**
	 * Return a stream customers and their volumes of business,
	 * sorted in descending order of volume.
	 */
	public Stream<CustomerAndVolume> getCustomersAndVolumeStream() {
		return getCustomers().values().stream().map(CustomerAndVolume::new).sorted();
	}
}
