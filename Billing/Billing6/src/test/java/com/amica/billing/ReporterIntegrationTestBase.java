package com.amica.billing;


import static com.amica.billing.TestUtility.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base class for integration tests for the {@link Reporter}.
 * Derived classes will set up persistence services to support
 * {@link Billing} objects that can be wired into our test target.
 * We provide helper methods and test cases.
 * 
 * @author Will Provost
 */
public class ReporterIntegrationTestBase {
	
	public static final String EXPECTED_FOLDER = 
			"src/test/resources/expected/integration_test";
	
	public static final String NEW_CUSTOMER_FIRST_NAME = "Merle";
	public static final String NEW_CUSTOMER_LAST_NAME = "Haggard";
	public static final Terms NEW_CUSTOMER_TERMS = Terms.CASH;
	public static final String NEW_CUSTOMER_NAME = 
			NEW_CUSTOMER_FIRST_NAME + " " + NEW_CUSTOMER_LAST_NAME;
	
	public static final int NEW_INVOICE_NUMBER = 125;
	public static final String NEW_INVOICE_CUSTOMER_FIRST_NAME = "John";
	public static final String NEW_INVOICE_CUSTOMER_LAST_NAME = "Hiatt";
	public static final Terms NEW_INVOICE_CUSTOMER_TERMS = Terms.CREDIT_90;
	public static final double NEW_INVOICE_AMOUNT = 999.0;
	public static final String NEW_INVOICE_CUSTOMER_NAME = 
		NEW_INVOICE_CUSTOMER_FIRST_NAME + " " + 
			NEW_INVOICE_CUSTOMER_LAST_NAME;
	
	public static final int PAID_INVOICE_NUMBER = 107;
	public static final String PAID_INVOICE_CUSTOMER_FIRST_NAME = "Glen";
	public static final String PAID_INVOICE_CUSTOMER_LAST_NAME = "Campbell";
	public static final Terms PAID_INVOICE_CUSTOMER_TERMS = Terms.CREDIT_60;
	public static final double PAID_INVOICE_AMOUNT = 800.0;
	public static final LocalDate PAID_INVOICE_DATE = LocalDate.of(2021, 9, 15);
	public static final String PAID_INVOICE_CUSTOMER_NAME = 
			PAID_INVOICE_CUSTOMER_FIRST_NAME + " " + 
					PAID_INVOICE_CUSTOMER_LAST_NAME;

	/**
	 * Helper method to get the output folder.
	 */
	protected String getOutputFolder() {
		return OUTPUT_FOLDER;
	}
	
	/**
	 * A canonicalization function to allow our expected output to include
	 * a placeholder to represent the date on which the test in being run.
	 */
	public static final String fixTestDate(String text) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
		return text.replace("@TESTDATE@", formatter.format(LocalDate.now()));
	}

	/**
	 * Utility method that compares an actual file -- to be found in our
	 * output folder -- with an expected file -- to be found in our 
	 * expected folder -- first applying a given canonicalization function. 
	 */
	public void assertCorrectOutput(String actualFilename, 
			String expectedFilename, Function<String,String> canonicalizer) {
		Path actualPath = Paths.get(getOutputFolder(), actualFilename);
		Path expectedPath = Paths.get(EXPECTED_FOLDER, expectedFilename);
		TestUtility.assertCorrectOutput(actualPath, expectedPath, canonicalizer);
	}
	

	/**
	 * Utility method that compares an actual file -- to be found in our
	 * output folder -- with an expected file -- to be found in our 
	 * expected folder. 
	 */
	public void assertCorrectOutput
			(String actualFilename, String expectedFilename) {
		assertCorrectOutput(actualFilename, expectedFilename, Function.identity());
	}
	
	/**
	 * Utility method that compares a file as it's found, by the same name,
	 * in our output and expected folders. 
	 */
	public void assertCorrectOutput(String filename) {
		assertCorrectOutput(filename, filename);
	}
	
	/**
	 * Applies "substream" logic using dropWhile()/takeWhile() to derive
	 * a string representing the customer heading and associated invoices
	 * for a single customer.
	 */
	public static String getCustomerGroup
			(Stream<String> lines, String customerName) {
		return lines
		.dropWhile(line -> !line.startsWith(customerName))
		.takeWhile(line -> !line.trim().isEmpty())
		.collect(Collectors.joining("\n"));	
	}
	
	/**
	 * Because the invoices-by-customer report has no predictable order,
	 * we test by seeking the customer heading and group of invoices for
	 * a single customer, wherever that may be found in each of the actual
	 * and expected files, and assert that they are identical. 
	 */
	public static void assertCorrectInvoicesByCustomer(String filename) {
		final String CUSTOMER = "John Hiatt";
		try (
			Stream<String> actualLines = 
					Files.lines(Paths.get(OUTPUT_FOLDER, filename)); 
			Stream<String> expectedLines = Files.lines(Paths.get
					(EXPECTED_FOLDER, Reporter.FILENAME_INVOICES_BY_CUSTOMER));
		) {
			String actualGroup = getCustomerGroup(actualLines, CUSTOMER);
			String expectedGroup = getCustomerGroup(expectedLines, CUSTOMER);
			assertThat("Couldn't find matching customer group", 
					actualGroup, equalTo(expectedGroup));
					
		} catch (IOException ex) {
			fail("Couldn't open actual and expected file content.", ex);
		}
	}
	
	/**
	 * Because the invoices-by-customer report has no predictable order,
	 * we test by seeking the customer heading and group of invoices for
	 * a single customer, wherever that may be found in each of the actual
	 * and expected files, and assert that they are identical. 
	 */
	public static void assertCorrectInvoicesGroupedByCustomer() {
		assertCorrectInvoicesByCustomer(Reporter.FILENAME_INVOICES_BY_CUSTOMER);
	}
	
	@Autowired
	protected Billing billing;
	
	@Autowired
	protected Reporter reporter;
	
	/**
	 * Delegate to {@link BillingIntegrationTest#setUpFiles()} to stage the 
	 * source data. Create necessary folders, and remove any reports from  prior 
	 * tests, to avoid false positives caused by an expected file hanging around.
	 * Assemble the full system of Billing and Reporter objects.
	 */
	@BeforeEach
	public void setUp() throws IOException {
		Files.createDirectories(Paths.get(getOutputFolder()));
		Stream.of(Reporter.FILENAME_INVOICES_BY_NUMBER,
			Reporter.FILENAME_INVOICES_BY_CUSTOMER,
			Reporter.FILENAME_OVERDUE_INVOICES,
			Reporter.FILENAME_CUSTOMERS_AND_VOLUME)
				.forEach(f -> new File(getOutputFolder(), f).delete());

		reporter.setAsOf(LocalDate.of(2022, 1, 8));
	}
	
	@Test
	public void testReportInvoicesOrderedByNumber() {
		reporter.reportInvoicesOrderedByNumber();
		assertCorrectOutput(Reporter.FILENAME_INVOICES_BY_NUMBER);
	}
	
	@Test
	public void testReportInvoicesGroupedByCustomer() {
		reporter.reportInvoicesGroupedByCustomer();
		assertCorrectInvoicesGroupedByCustomer();
	}
	
	@Test
	public void testReportOverdueInvoices() {
		reporter.reportOverdueInvoices();
		assertCorrectOutput(Reporter.FILENAME_OVERDUE_INVOICES);
	}
	
	@Test
	public void testReportCustomersAndVolume() {
		reporter.reportCustomersAndVolume();
		assertCorrectOutput(Reporter.FILENAME_CUSTOMERS_AND_VOLUME);
	}
	
	@Test
	public void testCreateCustomer() {
		billing.createCustomer(NEW_CUSTOMER_FIRST_NAME, 
				NEW_CUSTOMER_LAST_NAME, NEW_CUSTOMER_TERMS);
		assertCorrectOutput(Reporter.FILENAME_CUSTOMERS_AND_VOLUME,
			Reporter.FILENAME_CUSTOMERS_AND_VOLUME
				.replace(".txt", "_new_customer.txt"));
	}
	
	@Test
	public void testCreateInvoice() {
		billing.createInvoice(NEW_INVOICE_CUSTOMER_FIRST_NAME + " " +
				NEW_INVOICE_CUSTOMER_LAST_NAME, NEW_INVOICE_AMOUNT);
		assertCorrectOutput(Reporter.FILENAME_INVOICES_BY_NUMBER,
				Reporter.FILENAME_INVOICES_BY_NUMBER
					.replace(".txt", "_new_invoice.txt"), 
				ReporterIntegrationTestBase::fixTestDate);
	}
	
	@Test
	public void testPayInvoice() {
		billing.payInvoice(PAID_INVOICE_NUMBER);
		assertCorrectOutput(Reporter.FILENAME_OVERDUE_INVOICES,
				Reporter.FILENAME_OVERDUE_INVOICES
					.replace(".txt", "_pay_invoice.txt"), 
				ReporterIntegrationTestBase::fixTestDate);
	}
}
