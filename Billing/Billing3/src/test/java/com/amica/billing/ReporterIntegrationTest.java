package com.amica.billing;


import static com.amica.billing.BillingIntegrationTest.*;
import static com.amica.billing.TestUtility.AS_OF_DATE;
import static com.amica.billing.TestUtility.OUTPUT_FOLDER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
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

/**
 * Integration test for the {@link Reporter}.
 * We tap into a lot of the setup and test behavior of the 
 * {@link BillingIntegrationTest}, using a {@link Billing} object configured
 * as it is in that test, and using some of the same data and expected-output files.
 * We check the initial reports, and also trigger data changes and compare
 * newly-generated reports to expected content for each of those change scenarios. 
 * 
 * @author Will Provost
 */
public class ReporterIntegrationTest {
	
	public static final String EXPECTED_FOLDER = 
			"src/test/resources/expected/integration_test";
	
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
	public void assertCorrectInvoicesByCustomer(String filename) {
		final String CUSTOMER = "John Hiatt";
		try (
			Stream<String> actualLines = 
					Files.lines(Paths.get(getOutputFolder(), filename)); 
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
	public void assertCorrectInvoicesByCustomer() {
		assertCorrectInvoicesByCustomer(Reporter.FILENAME_INVOICES_BY_CUSTOMER);
	}
	
	protected Billing billing;
	protected Reporter reporter;

	/**
	 * Delegate to {@link BillingIntegrationTest#setUpFiles()} to stage the 
	 * source data. Create necessary folders, and remove any reports from  prior 
	 * tests, to avoid false positives caused by an expected file hanging around.
	 * Assemble the full system of Billing and Reporter objects.
	 */
	@BeforeEach
	public void setUp() throws IOException {
		BillingIntegrationTest.setUpFiles();
		Files.createDirectories(Paths.get(getOutputFolder()));
		Stream.of(Reporter.FILENAME_INVOICES_BY_NUMBER,
			Reporter.FILENAME_INVOICES_BY_CUSTOMER,
			Reporter.FILENAME_OVERDUE_INVOICES,
			Reporter.FILENAME_CUSTOMERS_AND_VOLUME)
				.forEach(f -> new File(getOutputFolder(), f).delete());

		billing = BillingIntegrationTest.createBilling();
		reporter = new Reporter(billing, getOutputFolder(), AS_OF_DATE);;
	}
	
	@Test
	public void testReportInvoicesOrderedByNumber() {
		reporter.reportInvoicesOrderedByNumber();
		assertCorrectOutput(Reporter.FILENAME_INVOICES_BY_NUMBER);
	}
	
	@Test
	public void testReportInvoicesGroupedByCustomer() {
		reporter.reportInvoicesGroupedByCustomer();
		assertCorrectInvoicesByCustomer();
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
				ReporterIntegrationTest::fixTestDate);
	}
	
	@Test
	public void testPayInvoice() {
		billing.payInvoice(PAY_INVOICE_NUMBER);
		assertCorrectOutput(Reporter.FILENAME_OVERDUE_INVOICES,
				Reporter.FILENAME_OVERDUE_INVOICES
					.replace(".txt", "_pay_invoice.txt"), 
				ReporterIntegrationTest::fixTestDate);
	}
}
