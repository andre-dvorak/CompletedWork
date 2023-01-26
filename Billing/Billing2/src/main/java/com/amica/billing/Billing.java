package com.amica.billing;

import static java.util.function.Function.identity;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.amica.billing.parse.Parser;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.java.Log;

/**
 * This is the central component of the system.
 * It reads a file of {@link Customer}s and a file of {@link Invoice}s,
 * using configurable {@link Parser}s so as to to handle different file 
 * formats; offers a few useful queries on the loaded data;
 *  and allows a few modifications to the data set, which will be 
 *  saved back to the original data files.
 * 
 * @author Will Provost
 */
@Log
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
			
	private Path customersFile;
	private Path invoicesFile;
	private Parser parser;
	private Map<String,Customer> customers;
	private List<Invoice> invoices;
	private int nextInvoiceNumber;
	private List<Consumer<Customer>> customerListeners = new ArrayList<>();
	private List<Consumer<Invoice>> invoiceListeners = new ArrayList<>();
	
	/**
	 * Build a Billing object based on two filenames.
	 * We store off the paths, {@link ParserFactory find a parser}, 
	 * read the data into memory, and set the next
	 * available invoice number based on the maximum of what's issued so far.
	 */
	public Billing(String customersFilename, String invoicesFilename) {
		
		customersFile = Paths.get(customersFilename);
		invoicesFile = Paths.get(invoicesFilename);
		parser = ParserFactory.createParser(invoicesFilename);
		
		try ( Stream<String> customerLines = Files.lines(customersFile); ) {
			customers = parser.parseCustomers(customerLines)
					.collect(Collectors.toMap(Customer::getName, identity()));
			try ( Stream<String> invoiceLines = Files.lines(invoicesFile); ) {
				invoices = parser.parseInvoices(invoiceLines, customers)
						.collect(Collectors.toList());
				nextInvoiceNumber = invoices.stream()
						.mapToInt(Invoice::getNumber).max().orElse(0) + 1;
			}
		} catch (Exception ex) {
			log.log(Level.SEVERE, "Couldn't load from given filenames.", ex);
		}
		
	}
	
	/**
	 * Save updated data to the customers file.
	 */
	private void saveCustomers() {
		try ( PrintWriter out = new PrintWriter(new FileWriter(customersFile.toFile())); ) {
			parser.produceCustomers(customers.values().stream())
				.forEach(out::println);
		} catch (Exception ex) {
			log.log(Level.WARNING, ex, 
					() -> "Couldn't open " + customersFile + " in write mode.");
		}
	}

	/**
	 * Save updated data to the invoices file.
	 */
	private void saveInvoices() {
		try ( PrintWriter out = new PrintWriter(new FileWriter(invoicesFile.toFile())); ) {
			parser.produceInvoices(invoices.stream())
				.forEach(out::println);
		} catch (Exception ex) {
			log.log(Level.WARNING, ex, 
					() -> "Couldn't open " + invoicesFile + " in write mode.");
		}
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
		if (!customers.containsKey(customer.getName())) {
			customers.put(customer.getName(), customer);
			saveCustomers();
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
		
		if (customers.containsKey(customerName)) {
			Invoice invoice = new Invoice(nextInvoiceNumber++, 
					customers.get(customerName), amount, 
					LocalDate.now());
			invoices.add(invoice);
			saveInvoices();
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
		
		Optional<Invoice> found = invoices.stream()
				.filter(inv -> inv.getNumber() == invoiceNumber)
				.findAny();
		if (found.isPresent()) {
			Invoice invoice = found.get();
			if (!invoice.getPaidDate().isPresent()) {
				invoice.setPaidDate(Optional.of(LocalDate.now()));
				saveInvoices();
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
		return Collections.unmodifiableMap(customers);
	}
	
	/**
	 * Return the raw list of invoices.
	 */
	public List<Invoice> getInvoices() {
		return Collections.unmodifiableList(invoices);
	}
	
	/**
	 * Return a stream of all invoices, sorted by number.
	 */
	public Stream<Invoice> getInvoicesOrderedByNumber() {
		return invoices.stream().sorted(compareByNumber);
	}
	
	/**
	 * Return a stream of all invoices, sorted by issue date.
	 */
	public Stream<Invoice> getInvoicesOrderedByDate() {
		return invoices.stream().sorted(compareByDate);
	}
	
	/**
	 * Return a stream of all invoices for a given customer.
	 */
	public Stream<Invoice> getInvoicesForCustomer(Customer customer) {
		return invoices.stream()
				.filter(inv -> inv.getCustomer().equals(customer))
				.sorted(compareByNumber);
	}
	
	/**
	 * Return a map of customers and their invoices, sorted by number.
	 */
	public Map<Customer,List<Invoice>> getInvoicesGroupedByCustomer() {
		return invoices.stream().sorted(compareByNumber)
			.collect(Collectors.groupingBy(Invoice::getCustomer, 
				Collectors.toList()));
	}
	
	/**
	 * Return a stream of overdue invoices, sorted by issue date.
	 */
	public Stream<Invoice> getOverdueInvoices(LocalDate asOf) {
		return invoices.stream()
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
		return customers.values().stream().collect(Collectors.toMap
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
		return customers.values().stream().map(CustomerAndVolume::new).sorted();
	}
}
