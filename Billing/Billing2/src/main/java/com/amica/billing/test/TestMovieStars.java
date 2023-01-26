package com.amica.billing.test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import com.amica.billing.Billing;
import com.amica.billing.Customer;
import com.amica.billing.Invoice;
import com.amica.billing.Reporter;
import com.amica.billing.Terms;
import com.amica.billing.parse.FlatParser;
import com.amica.billing.parse.Parser;

/**
 * Test program that asks a {@link Billing} to load flat-format data and produce
 * a series of reports.
 *
 * @author Will Provost
 */
public class TestMovieStars {

	public static final String CUSTOMERS_FILENAME = 
			"data/movie_Stars/customers.flat";
	public static final String INVOICES_FILENAME = 
			"data/movie_Stars/invoices.flat";
	public static final String OUTPUT_FOLDER = "reports/movie_Stars";
	
	public static void assertThat(boolean condition, String error) {
		if (!condition) {
			System.out.println("    ASSERTION FAILED: " + error);
		}
	}

	public static void assertEqual(Object actual, Object expected, String error) {
		if (!actual.equals(expected)) {
			System.out.format("    ASSERTION FAILED: " + error + "%n", actual);
		}
	}

	public static void testParser() {
		System.out.println("Testing the parser ...");

		Parser parser = new FlatParser();
		Stream<String> customerData = Stream.of(
				"Myrna       Loy         CREDIT_60 ",
				"William     Powell      CASH      "
			);
		Stream<Customer> customers = parser.parseCustomers(customerData);
		assertThat(customers.anyMatch(c -> c.getName().equals("William Powell")),
				"There should be a customer named \"William Powell\".");
		
		Stream<String> invoiceData = Stream.of(
				" 523Edward      Brophy        266.00100120102020",
				" 505Edward      Brophy         34.00091220      "
			);
		Map<String,Customer> customerMap = new HashMap<>();
		Customer customer = new Customer("Edward", "Brophy", Terms.CASH);
		customerMap.put(customer.getName(), customer);
		Stream<Invoice> invoices = parser.parseInvoices(invoiceData, customerMap);
		assertThat(invoices.anyMatch(inv -> inv.getNumber() == 505),
				"There shuld be an invoice with the number 505.");

		System.out.println();
	}
	
	public static void testBilling() {
		System.out.println("Testing the Billing object ...");

		final String EXPECTED_CUSTOMER = "Myrna Loy";
		Billing billing = new Billing(CUSTOMERS_FILENAME, INVOICES_FILENAME);
		Map<String,Customer> customers = billing.getCustomers();
		assertEqual(customers.keySet().size(), 13, 
				"There should be 13 customers, was %s.");
		assertThat(customers.containsKey(EXPECTED_CUSTOMER), 
				"There should be a customer named \"" + EXPECTED_CUSTOMER + "\".");
		
		assertEqual(customers.get(EXPECTED_CUSTOMER).getTerms(), Terms.CREDIT_60, 
				"Jerry Reed's payment terms should be CREDIT_60, was %s.");

		System.out.println();
	}
	
	public static void testReporter() {
		System.out.println("Testing the Reporter object ...");
		System.out.println();

		// Set to true to see reports in console output,
		// or false to generate report files:
		Reporter.DIVERT_TO_SYSOUT = false;

		Billing billing = new Billing(CUSTOMERS_FILENAME, INVOICES_FILENAME);
		Reporter reporter = new Reporter
				(billing, OUTPUT_FOLDER, LocalDate.of(2021, 12, 1));
		
		reporter.reportInvoicesOrderedByNumber();
		reporter.reportInvoicesGroupedByCustomer();
		reporter.reportOverdueInvoices();
		reporter.reportCustomersAndVolume();
		
		billing.createCustomer("Lionel", "Barrymore", Terms.CASH);
		billing.createInvoice("Lionel Barrymore", 9.99);
		billing.createInvoice("Porter Hall", 133);
		billing.payInvoice(958);		
	}
	
	public static void main(String[] args) {
		
		File workspace = new File("data/movie_Stars");
		workspace.mkdir();
		try {
			// This sets up data files just for this test, fresh copy each time,
			// and assures that the folders are in place to hold reports:
			Files.copy(Paths.get("data/customers.flat"), Paths.get(CUSTOMERS_FILENAME),
					StandardCopyOption.REPLACE_EXISTING);
			Files.copy(Paths.get("data/invoices.flat"), Paths.get(INVOICES_FILENAME),
					StandardCopyOption.REPLACE_EXISTING);
			Files.createDirectories(Paths.get(OUTPUT_FOLDER));
			
			testParser();
			testBilling();
			testReporter();
			
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
