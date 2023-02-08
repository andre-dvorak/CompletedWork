package com.amica.billing;

import static com.amica.billing.TestUtility.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
/**
 * Integration test for the {@link Billing} class.
 * This test focuses on the "country singers" data. We make a copy of the
 * data files at the start of each test case, create the Billing object
 * to load them, and check its getters and query methods.
 * A few more test cases drive updates to the object, and assure that
 * they are reflected in updates to the staged data files.
 * 
 * @author Will Provost
 */
public class BillingIntegrationTest {

	public static final String SOURCE_FOLDER = "data";

	public static final String NEW_CUSTOMER_FIRST_NAME = "Merle";
	public static final String NEW_CUSTOMER_LAST_NAME = "Haggard";
	public static final Terms NEW_CUSTOMER_TERMS = Terms.CASH;
	
	public static final String NEW_INVOICE_CUSTOMER_FIRST_NAME = "John";
	public static final String NEW_INVOICE_CUSTOMER_LAST_NAME = "Hiatt";
	public static final int NEW_INVOICE_NUMBER = 125;
	public static final double NEW_INVOICE_AMOUNT = 999.0;
	
	public static final int PAY_INVOICE_NUMBER = 107;
	
	/**
	 * Assure that necessary folders are in place, and make a copy of the
	 * "country singers" data files.
	 */
	public static void setUpFiles() throws IOException {
		Files.createDirectories(Paths.get(TEMP_FOLDER));
		Files.createDirectories(Paths.get(OUTPUT_FOLDER));
		Files.copy(Paths.get(SOURCE_FOLDER, CUSTOMERS_FILENAME), 
				Paths.get(TEMP_FOLDER, CUSTOMERS_FILENAME),
				StandardCopyOption.REPLACE_EXISTING);
		Files.copy(Paths.get(SOURCE_FOLDER, INVOICES_FILENAME), 
				Paths.get(TEMP_FOLDER, INVOICES_FILENAME),
				StandardCopyOption.REPLACE_EXISTING);
	}
	
	/**
	 * Factory for a stock billing object.
	 */
	public static Billing createBilling() {
		return new Billing(TEMP_FOLDER + "/" + CUSTOMERS_FILENAME,
				TEMP_FOLDER + "/" + INVOICES_FILENAME);
	}
	
	private Billing billing;
	
	@BeforeEach
	public void setUp() throws IOException {
		setUpFiles();
		billing = createBilling();
	}
	
	@Test
	public void testGetInvoicesOrderedByNumber() {
		assertThat(billing.getInvoicesOrderedByNumber(),
				hasNumbers(101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 121, 122, 123, 124));
	}
	
	@Test
	public void testGetInvoicesOrderedByDate() {
		assertThat(billing.getInvoicesOrderedByDate(),
				hasNumbers(101, 102, 103, 104, 110, 105, 106, 107, 109, 111, 112, 113, 108, 114, 115, 116, 117, 118, 119, 121, 123, 122, 124));
	}
	
	@Test
	public void testGetInvoicesGroupedByCustomer() {
		Map<Customer,List<Invoice>> map = billing.getInvoicesGroupedByCustomer();
		Customer jerryReed = billing.getCustomers().get("Jerry Reed");
		assertThat(map.get(jerryReed).stream(), hasNumbers(109, 122));
	}

	@Test
	public void testGetOverdueInvoices() {
		assertThat(billing.getOverdueInvoices(LocalDate.of(2022, 1, 8)),
				hasNumbers(102, 105, 106, 107, 113, 116, 118, 122, 124));
	}
	
	@Test
	public void testGetCustomersAndVolume() {
		List<Billing.CustomerAndVolume> list = 
				billing.getCustomersAndVolumeStream().toList();
		
		assertThat(list.get(0).getCustomer(), hasProperty("name", equalTo("Jerry Reed")));
		assertThat(list.get(0).getVolume(), closeTo(2640.0, .0001));
		
		Billing.CustomerAndVolume last = list.get(list.size() - 1);
		assertThat(last.getCustomer(), hasProperty("name", equalTo("Janis Joplin")));
		assertThat(last.getVolume(), closeTo(510.0, .0001));
	}
	
	/**
	 * After adding a customer, assure that there is one new line in the
	 * customers data file.
	 */
	@Test
	public void testCreateCustomer() throws IOException {
		billing.createCustomer(NEW_CUSTOMER_FIRST_NAME, 
				NEW_CUSTOMER_LAST_NAME, NEW_CUSTOMER_TERMS);
		try ( Stream<String> lines = 
				Files.lines(Paths.get(TEMP_FOLDER, CUSTOMERS_FILENAME)); ) {
			assertThat(lines.anyMatch(s -> s.equals(String.format("%s,%s,%s",
				NEW_CUSTOMER_FIRST_NAME, NEW_CUSTOMER_LAST_NAME, NEW_CUSTOMER_TERMS))),
					equalTo(true));
		}
	}
	
	/**
	 * After adding an invoice, assure that there is one new line in the
	 * invoices data file.
	 */
	@Test
	public void testCreateInvoice() throws IOException {
		billing.createInvoice(NEW_INVOICE_CUSTOMER_FIRST_NAME + " " +
				NEW_INVOICE_CUSTOMER_LAST_NAME, NEW_INVOICE_AMOUNT);
		try ( Stream<String> lines = 
				Files.lines(Paths.get(TEMP_FOLDER, INVOICES_FILENAME)); ) {
			assertThat(lines.anyMatch(s -> s.startsWith(String.format("%d,%s,%s,%1.2f,",
				NEW_INVOICE_NUMBER, NEW_INVOICE_CUSTOMER_FIRST_NAME, 
				NEW_INVOICE_CUSTOMER_LAST_NAME, NEW_INVOICE_AMOUNT))),
					equalTo(true));
		}
	}

	/**
	 * After paying an invoice, assure that the line for that invoice in the
	 * data file now bears the correct paid date.
	 */
	@Test
	public void testPayInvoice() throws IOException {
		billing.payInvoice(PAY_INVOICE_NUMBER);
		try ( Stream<String> lines = 
				Files.lines(Paths.get(TEMP_FOLDER, INVOICES_FILENAME)); ) {
			assertThat(lines.anyMatch(s -> s.startsWith
				(String.format("%d,", PAY_INVOICE_NUMBER)) &&
					s.endsWith(LocalDate.now().toString())),
					equalTo(true));
		}
	}
}
