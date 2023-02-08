package com.amica.billing;

import static com.amica.billing.TestUtility.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Unit test for the {@link Billing} class.
 * This test focuses on the test data set defined in {@link TestUtillity}
 * and prepared data files that reflect that data. We make a copy of the
 * data files at the start of each test case, create the Billing object
 * to load them, and check its getters and query methods.
 * A few more test cases drive updates to the object, and assure that
 * they are reflected in updates to the staged data files.
 * 
 * @author Will Provost
 */
public class BillingTest {

	public static final String SOURCE_FOLDER = "src/test/resources/data";

	private Billing billing;
	private Consumer<Customer> customerListener;
	private Consumer<Invoice> invoiceListener;

	/**
	 * Helper method to get the customers filename.
	 */
	protected String getCustomersFilename() {
		return CUSTOMERS_FILENAME;
	}
	
	/**
	 * Helper method to get the invoices filename.
	 */
	protected String getInvoicesFilename() {
		return INVOICES_FILENAME;
	}
	
	/**
	 * Helper method to create the Billing object.
	 */
	protected Billing createTestTarget() {
		return new Billing(TEMP_FOLDER + "/" + getCustomersFilename(),
				TEMP_FOLDER + "/" + getInvoicesFilename());
	}
	
	/**
	 * Assure that the necessary folders are in place, and make a copy
	 * of the source data files. Install mock objects as listeners for changes.
	 */
	@BeforeEach
	@SuppressWarnings("unchecked")
	public void setUp() throws IOException {
		Files.createDirectories(Paths.get(TEMP_FOLDER));
		Files.createDirectories(Paths.get(OUTPUT_FOLDER));
		Files.copy(Paths.get(SOURCE_FOLDER, getCustomersFilename()), 
				Paths.get(TEMP_FOLDER, getCustomersFilename()),
				StandardCopyOption.REPLACE_EXISTING);
		Files.copy(Paths.get(SOURCE_FOLDER, getInvoicesFilename()), 
				Paths.get(TEMP_FOLDER, getInvoicesFilename()),
				StandardCopyOption.REPLACE_EXISTING);
				
		billing = createTestTarget();
		
		billing.addCustomerListener(customerListener = mock(Consumer.class));
		billing.addInvoiceListener(invoiceListener = mock(Consumer.class));
	}
	
	@Test
	public void testGetCustomers() {
		Map<String,Customer> customers = billing.getCustomers(); 
		assertThat(customers.keySet(), hasSize(GOOD_CUSTOMERS.size()));
		for (String name : customers.keySet()) {
			assertThat(customers.get(name), 
					samePropertyValuesAs(GOOD_CUSTOMERS_MAP.get(name)));
		}
	}
	
	@Test
	public void testGetInvoices() {
		assertThat(billing.getInvoices(), sameAsList(GOOD_INVOICES));
	}
	
	@Test
	public void testGetInvoicesOrderedByNumber() {
		assertThat(billing.getInvoicesOrderedByNumber(), 
				hasNumbers(1, 2, 3, 4, 5, 6));
	}
	
	@Test
	public void testGetInvoicesOrderedByDate() {
		assertThat(billing.getInvoicesOrderedByDate(),
				hasNumbers(4, 6, 1, 2, 5, 3));
	}
	
	@Test
	public void testGetInvoicesGroupedByCustomer() {
		Map<Customer,List<Invoice>> map = billing.getInvoicesGroupedByCustomer();
		assertThat(map.get(GOOD_CUSTOMERS.get(0)).stream(),  hasNumbers(1));
		assertThat(map.get(GOOD_CUSTOMERS.get(1)).stream(), hasNumbers(2, 3, 4));
		assertThat(map.get(GOOD_CUSTOMERS.get(2)).stream(), hasNumbers(5, 6));
	}

	@Test
	public void testGetOverdueInvoices() {
		assertThat(billing.getOverdueInvoices(AS_OF_DATE), hasNumbers(4, 6, 1));
	}
	
	@Test
	public void testGetCustomersAndVolume() {
		List<Billing.CustomerAndVolume> list = 
				billing.getCustomersAndVolumeStream().toList();
		assertThat(list.get(0).getCustomer(), equalTo(GOOD_CUSTOMERS.get(2)));
		assertThat(list.get(0).getVolume(), closeTo(1100.0, .0001));
		assertThat(list.get(1).getCustomer(), equalTo(GOOD_CUSTOMERS.get(1)));
		assertThat(list.get(1).getVolume(), closeTo(900.0, .0001));
		assertThat(list.get(2).getCustomer(), equalTo(GOOD_CUSTOMERS.get(0)));
		assertThat(list.get(2).getVolume(), closeTo(100.0, .0001));
	}
	
	/**
	 * After adding a customer, assure that there is one new line in the
	 * customers data file. We also verify that the object makes the required 
	 * outbound call to the registered customer listener.
	 */
	@Test
	public void testCreateCustomer() throws IOException {
		final String FIRST_NAME = "Customer";
		final String LAST_NAME = "Four";
		final Terms TERMS = Terms.CASH;
		
		billing.createCustomer(FIRST_NAME, LAST_NAME, TERMS);
		
		try ( Stream<String> lines = 
				Files.lines(Paths.get(TEMP_FOLDER, getCustomersFilename())); ) {
			assertThat(lines.anyMatch(s -> s.equals("Customer,Four,CASH")),
					equalTo(true));
		}

		verify(customerListener).accept(any(Customer.class));
	}
	
	@Test
	public void testCreateCustomer_Existing() {
		assertThrows(IllegalArgumentException.class,
				() -> billing.createCustomer("Customer", "One", Terms.CASH));
	}
	
	/**
	 * After adding an invoice, assure that there is one new line in the
	 * invoices data file. We also verify that the object makes the required 
	 * outbound call to the registered invoice listener.
	 */
	@Test
	public void testCreateInvoice() throws IOException {
		final Customer CUSTOMER = GOOD_CUSTOMERS.get(0);
		final double AMOUNT = 999.0;
		billing.createInvoice(CUSTOMER.getName(), AMOUNT);

		try ( Stream<String> lines = 
				Files.lines(Paths.get(TEMP_FOLDER, getInvoicesFilename())); ) {
			assertThat(lines.anyMatch(s -> s.startsWith("7,Customer,One,999.00,")),
					equalTo(true));
		}
		
		verify(invoiceListener).accept(any(Invoice.class));
	}
	
	@Test
	public void testCreateInvoice_NoSuchCustomer() {
		assertThrows(IllegalArgumentException.class, 
				() -> billing.createInvoice("Customer Five", 999));
	}
	
	/**
	 * After paying an invoice, assure that the line for that invoice in the
	 * data file now bears the correct paid date. We also verify that the
	 * object makes the required outbound call to the registered invoice listener.
	 */
	@Test
	public void testPayInvoice() throws IOException {
	
		billing.payInvoice(1);
		
		try ( Stream<String> lines = 
				Files.lines(Paths.get(TEMP_FOLDER, getInvoicesFilename())); ) {
			assertThat(lines.anyMatch(s -> s.startsWith("1,Customer,One") &&
					s.endsWith(LocalDate.now().toString())),
					equalTo(true));
		}
		
		// A weaker assertion that doesn't check what invoice is passed:
		verify(invoiceListener).accept(any(Invoice.class));
		
		// Get the affected invoice, make most of your asserts against taht,
		// and then verify that it was passed in the outbound call.
		// This works since an object is always equal to itself (or that's
		// how equals() is supposed to work, and Lombok does it correctly).
		Invoice paid = billing.getInvoices().get(0);
		assertThat(paid.getPaidDate().isPresent(), equalTo(true));
		verify(invoiceListener).accept(paid);
		
		// Use a Mockito argument captor, and make specific assertions
		// against the captured object.
		ArgumentCaptor<Invoice> captor = ArgumentCaptor.forClass(Invoice.class);
		verify(invoiceListener).accept(captor.capture());
		Invoice captured = captor.getValue();
		assertThat(captured.getPaidDate().isPresent(), equalTo(true));
		
		// Use the "bridge" between Mockito and Hamcrest: the argThat()
		// method lets Mockito match an obejct -- for purposes of stubbing
		// or verifying -- based on a Hamcrest matcher, which then can
		// be as simple or complex as you like.
		verify(invoiceListener).accept(argThat(hasProperty("paidDate",
				hasProperty("present", equalTo(true)))));
	}
	
	@Test
	public void testPayInvoice_NoSuchInvoice() {
		assertThrows(IllegalArgumentException.class, () -> billing.payInvoice(11));
	}
	
	
	@Test
	public void testPayInvoice_AlreadyPaid() {
		assertThrows(IllegalStateException.class, () -> billing.payInvoice(2));
	}
}
