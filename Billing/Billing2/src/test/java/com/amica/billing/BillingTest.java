package com.amica.billing;

import static com.amica.billing.TestUtility.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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
	public static final String CUSTOMER_FILE_LOC = TEMP_FOLDER + "/" + CUSTOMERS_FILENAME;
	public static final String INVOICE_FILE_LOC = TEMP_FOLDER + "/" + INVOICES_FILENAME;

	public static final Map<String, Double> expectedCustomersAndVolume = Map.of("Customer Three", 1100.00, "Customer Two", 900.00, "Customer One", 100.00);

	Billing billing;
	Consumer<Customer> mockCustomerConsumer;
	Consumer<Invoice> mockInvoiceConsumer;

	/**
	 * Assure that the necessary folders are in place, and make a copy
	 * of the source data files. Install mock objects as listeners for changes.
	 */
	@BeforeEach
	public void setUp() throws IOException {
		Files.createDirectories(Paths.get(TEMP_FOLDER));
		Files.createDirectories(Paths.get(OUTPUT_FOLDER));
		Files.copy(Paths.get(SOURCE_FOLDER, CUSTOMERS_FILENAME), 
				Paths.get(TEMP_FOLDER, CUSTOMERS_FILENAME),
				StandardCopyOption.REPLACE_EXISTING);
		Files.copy(Paths.get(SOURCE_FOLDER, INVOICES_FILENAME), 
				Paths.get(TEMP_FOLDER, INVOICES_FILENAME),
				StandardCopyOption.REPLACE_EXISTING);

		billing = new Billing(CUSTOMER_FILE_LOC, INVOICE_FILE_LOC);

		mockCustomerConsumer = Mockito.mock(Consumer.class);
		mockInvoiceConsumer = Mockito.mock(Consumer.class);
		billing.addCustomerListener(mockCustomerConsumer);
		billing.addInvoiceListener(mockInvoiceConsumer);
	}

	/////////////////////////////////////////////////////////////////////////////////////////
	// Invoice Query Methods
	//
	@Test
	public void testGetInvoicesOrderedByNumber(){
		assertThat( billing.getInvoicesOrderedByNumber(), hasNumbers(1,2,3,4,5,6) );
	}

	@Test
	public void testGetInvoicesOrderedByDate(){
		assertThat( billing.getInvoicesOrderedByDate(), hasNumbers(4,6,1,2,5,3) );
	}

	@Test
	public void testGetInvoicesGroupedByCustomer(){
		Map<Customer, List<Invoice>> invoicesByCustomer = billing.getInvoicesGroupedByCustomer();
		assertThat( invoicesByCustomer.get(GOOD_CUSTOMERS.get(0)).stream(), hasNumbers(1) );
		assertThat( invoicesByCustomer.get(GOOD_CUSTOMERS.get(1)).stream(), hasNumbers(2,3,4) );
		assertThat( invoicesByCustomer.get(GOOD_CUSTOMERS.get(2)).stream(), hasNumbers(5,6) );
	}

	@Test
	public void testGetOverdueInvoices(){
		assertThat( billing.getOverdueInvoices(AS_OF_DATE), hasNumbers(4,6,1) );
	}

	@Test
	public void testGetCustomersAndVolume(){
		Map<Customer, Double> customersAndVolume = billing.getCustomersAndVolume();

		for( Map.Entry<Customer, Double> e : customersAndVolume.entrySet() ){
			assertThat( e.getValue(), closeTo(expectedCustomersAndVolume.get(e.getKey().getName()), .0001) );
		}
	}

	@Test
	public void testGetCustomersAndVolumeStream(){
		Set<Billing.CustomerAndVolume> customersAndVolumeStream = billing.getCustomersAndVolumeStream().collect(Collectors.toSet());

		for( Billing.CustomerAndVolume e : customersAndVolumeStream ){
			assertThat( e.getVolume(), closeTo(expectedCustomersAndVolume.get(e.getCustomer().getName()), .0001) );
		}
	}

	/////////////////////////////////////////////////////////////////////////////////////////
	// Updating Methods
	//
	@Test
	@SneakyThrows
	public void testCreateCustomer(){
		billing.createCustomer("Test", "Person", Terms.CASH);
		try( Stream<String> fileStream = Files.lines(Paths.get(CUSTOMER_FILE_LOC)) ){
			assertThat( fileStream.filter(s -> s.contains("Test")).findFirst().orElse(null), equalTo("Test,Person,CASH") );
		}
		Mockito.verify( mockCustomerConsumer, Mockito.atLeastOnce()).accept(billing.getCustomers().get("Test Person") );
	}

	@Test
	@SneakyThrows
	public void testCreateInvoice(){
		billing.createInvoice(GOOD_CUSTOMERS.get(0).getName(), 999.99);
		try( Stream<String> fileStream = Files.lines(Paths.get(INVOICE_FILE_LOC)) ){
			assertThat( fileStream.filter(s -> s.contains("999.99")).findFirst().orElse(null), equalTo("7,Customer,One,999.99," + LocalDate.now()) );
		}
		Mockito.verify( mockInvoiceConsumer, Mockito.atLeastOnce()).accept(billing.getInvoices().get(6) );
	}

	@Test
	@SneakyThrows
	public void testPayInvoice(){
		billing.payInvoice(3);
		try( Stream<String> fileStream = Files.lines(Paths.get(INVOICE_FILE_LOC)) ){
			assertThat( fileStream.filter(s -> s.contains("2022-01-06")).findFirst().orElse(null), equalTo("3,Customer,Two,300.00,2022-01-06," + LocalDate.now()) );
		}
		Mockito.verify( mockInvoiceConsumer, Mockito.atLeastOnce()).accept(billing.getInvoices().get(2) );
	}
	
}
