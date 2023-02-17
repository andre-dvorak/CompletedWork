package com.amica.billing;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;

/**
 * This class formats plain-text reports using a few of the queries
 * available in the {@link Billing} class.

 * @author Will Provost
 */
@Getter
@Log
@Component
public class Reporter {
	
	public static boolean DIVERT_TO_SYSOUT = false;
	
	public static final String FILENAME_INVOICES_BY_NUMBER = 
			"invoices_by_number.txt";
	public static final String FILENAME_INVOICES_BY_CUSTOMER = 
			"invoices_by_customer.txt";
	public static final String FILENAME_OVERDUE_INVOICES = 
			"overdue_invoices.txt";
	public static final String FILENAME_CUSTOMERS_AND_VOLUME = 
			"customer_and_volume.txt";
	
	/**
	 * Factory for a date formatter that we use consistently in a few reports. 
	 */
	public static DateTimeFormatter getFormatter() {
		return DateTimeFormatter.ofPattern("MM/dd/yyyy");
	}
	
	/**
	 * Helper method to format an invoice as a row in a table. 
	 */
	public static String formatInvoice(Invoice invoice) {
		DateTimeFormatter formatter = getFormatter();
		return String.format("%4d  %-24s  %10s  %,10.2f  %10s", 
				invoice.getNumber(), 
				invoice.getCustomer().getName(),
				invoice.getIssueDate().format(formatter), 
				invoice.getAmount(),
				invoice.getPaidDate().map(formatter::format).orElse(""));
	}
	
	/**
	 * Helpder method to format an overdue invoice as a row in a table.
	 * Usese {@link #formatInvoice formatInvoice} and extends with a column
	 * for the date on which the invoice was or is due.
	 */
	public static String formatOverdueInvoice(Invoice invoice) {
		return String.format("%s  %10s", formatInvoice(invoice), 
				invoice.getDueDate().format(getFormatter()));
	}
	
	private Billing billing;
	
	@Value("${Reporter.outputFolder}")
	@Setter
	private Path outputFolder;
	
	@Setter
	private LocalDate asOf = LocalDate.of(2022, 1, 8);
	
	/**
	 * Create a reporter object with reference to a {@link Billing} object,
	 * a target folder to contain generated reports, and a date based on which
	 * we'll consider unpaid invoices to see if they're overdue. 
	 */
	public Reporter(Billing billing) {
		this.billing = billing;
		billing.addCustomerListener(this::onCustomerChanged);
		billing.addInvoiceListener(this::onInvoiceChanged);
	}
	
	/**
	 * Handles the customer-change event fired by the billing object.
	 * This triggers the re-generation of the customers-and-volume report. 
	 */
	private void onCustomerChanged(Customer customer) {
		reportCustomersAndVolume();
	}
	
	/**
	 * Handles the invoice-change event fired by the billing object.
	 * This triggers the re-generation of all reports. 
	 */
	private void onInvoiceChanged(Invoice invoice) {
		reportInvoicesOrderedByNumber();
		reportInvoicesGroupedByCustomer();
		reportOverdueInvoices();
		reportCustomersAndVolume();
	}
	
	/**
	 * Helper method to create a writer from a filename.
	 * This observes the {@link #DIVERT_TO_SYSOUT} switch:
	 * if this value is true, then the returned writer delegates to standard out;
	 * if it's false, it delegates to a file of the given name. 
	 */
	private PrintWriter getWriter(String filename) {
		if (DIVERT_TO_SYSOUT) {
			class NonCloser extends PrintWriter {
				
				public NonCloser() {
					super(System.out);
				}
				
				@Override
				public void close() {
					println();
					flush();
				}
			}
			return new NonCloser(); 
		} else {
			try {
				return new PrintWriter(new FileWriter
						(outputFolder.resolve(filename).toFile()));
			} catch (Exception ex) {
				
				log.log(Level.SEVERE, ex,
						() -> "Couldn't open " + filename + " in write mode.");
			}
		}
		
		return null;
	}
	
	/**
	 * Generates a report of all invoices, ordered by number.
	 */
	public void reportInvoicesOrderedByNumber() {
		try ( PrintWriter out = getWriter(FILENAME_INVOICES_BY_NUMBER); ) {
			out.println("All invoices, ordered by invoice number");
			out.println("=".repeat(66));
			out.println();
			out.format("       %-24s %10s  %10s  %10s%n", 
					"Customer", "Issued", "Amount", "Paid");
			out.println("-".repeat(4) + "  " + "-".repeat(24) + "  " + 
					"-".repeat(10) + "  " + "-".repeat(10) + "  " + "-".repeat(10));
			
			out.println(billing.getInvoicesOrderedByNumber()
					.map(Reporter::formatInvoice)
					.collect(Collectors.joining("\n")));
		}
	}
	
	/**
	 * Generates a report of all invoices, grouped by customer and 
	 * then ordered by number.
	 */
	public void reportInvoicesGroupedByCustomer() {
		try ( PrintWriter out = getWriter(FILENAME_INVOICES_BY_CUSTOMER); ) {
			out.println("All invoices, grouped by customer and ordered by invoice number");
			out.println("=".repeat(66));
			out.println();
			out.format("       %-24s %10s  %10s  %10s%n", 
					"Customer", "Issued", "Amount", "Paid");
			out.println("-".repeat(4) + "  " + "-".repeat(24) + "  " + 
					"-".repeat(10) + "  " + "-".repeat(10) + "  " + "-".repeat(10));
			
			Map<Customer,List<Invoice>> data = 
					billing.getInvoicesGroupedByCustomer();
			for (Customer customer : data.keySet()) {
				out.println();
				out.println(customer.getName());
				out.println(data.get(customer).stream().map(Reporter::formatInvoice)
						.collect(Collectors.joining("\n")));
			}
		}
	}
	
	/**
	 * Generates a report of overdue invoices, ordered by invoice date.
	 */
	public void reportOverdueInvoices() {
		try ( PrintWriter out = getWriter(FILENAME_OVERDUE_INVOICES); ) {
			out.println("Overdue invoices, ordered by issue date");
			out.println("=".repeat(78));
			out.println();
			out.format("       %-24s %10s  %10s  %10s  %10s%n", 
					"Customer", "Issued", "Amount", "Paid", "Due");
			out.println("-".repeat(4) + "  " + "-".repeat(24) + "  " + 
					"-".repeat(10) + "  " + "-".repeat(10) + "  " + 
					"-".repeat(10) + "  " + "-".repeat(10));
			
			out.println(billing.getOverdueInvoices(asOf)
					.map(Reporter::formatOverdueInvoice)
					.collect(Collectors.joining("\n")));
		}
		}
	
	/**
	 * Generates a report of all customers and their total volume of business.
	 */
	public void reportCustomersAndVolume() {
		try ( PrintWriter out = getWriter(FILENAME_CUSTOMERS_AND_VOLUME); ) {
			out.println("All customers and total volume of business");
			out.println("=".repeat(66));
			out.println();
			out.format("%-24s  %12s%n", "Customer", "Volume");
			out.println("-".repeat(24) + "  " + "-".repeat(12));

			// Using the method that returns a stream -- results are ordered:
			billing.getCustomersAndVolumeStream()
					.forEach(cv -> out.format("%-24s  %,12.2f%n",
							cv.getCustomer().getName(), cv.getVolume()));
			
			// Using the method that returns a map -- results are not ordered:
//			for (Map.Entry<Customer,Double> entry : 
//					billing.getCustomersAndVolume().entrySet()) {
//				out.format("%-24s  %,12.2f%n", 
//						entry.getKey().getName(), entry.getValue());
//			}
		}
	}
}
