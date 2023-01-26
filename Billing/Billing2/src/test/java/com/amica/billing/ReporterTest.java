package com.amica.billing;

import static com.amica.billing.TestUtility.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;

import com.amica.billing.Billing.CustomerAndVolume;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Unit test for the {@link Reporter}.
 * We set up a mock {@link Billing} object as a source for data,
 * and then compare the reporter's produced files to prepared ones with
 * the expected content for each report. The mock Billing object also uses
 * argument captors to get references to the reporter's listener methods,
 * and calls them to prove that the reporter generates certain reports 
 * in respnose to those simulated events (even though, in this case,
 * no data actually changes). 
 * 
 * @author Will Provost
 */
public class ReporterTest {
	
	public static final String EXPECTED_FOLDER = 
			"src/test/resources/expected/unit_test";

	Billing mockBilling;
	Reporter reporter;
	Consumer<Customer> customerListener;
	Consumer<Invoice> invoiceListener;

	/**
	 * Utility method that compares an actual file -- to be found in our
	 * output folder -- with an expected file -- to be found in our 
	 * expected folder. 
	 */
	public static void assertCorrectOutput(String filename) {
		Path actualPath = Paths.get(OUTPUT_FOLDER, filename);
		Path expectedPath = Paths.get(EXPECTED_FOLDER, filename);
		TestUtility.assertCorrectOutput(actualPath, expectedPath);
	}
	
	/**
	 * Create necessary folders, and remove any reports from prior tests,
	 * to avoid false positives caused by an expected file hanging around.
	 * Build (relatively) simple data sets and mock a Billing object that 
	 * will return them from its query methods. Create the reporter based on
	 * this mock Billing object, and capture the listeners that it passes
	 * in calls to addXXXListener() methods. 
	 */
	@BeforeEach
	public void setUp() throws IOException {
		Files.createDirectories(Paths.get(OUTPUT_FOLDER));
		Stream.of(Reporter.FILENAME_INVOICES_BY_NUMBER,
			Reporter.FILENAME_INVOICES_BY_CUSTOMER,
			Reporter.FILENAME_OVERDUE_INVOICES,
			Reporter.FILENAME_CUSTOMERS_AND_VOLUME)
				.forEach(f -> new File(OUTPUT_FOLDER, f).delete());
		
		Map<Customer,List<Invoice>> invoicesByCustomer = new HashMap<>();
		invoicesByCustomer.put(GOOD_CUSTOMERS.get(0), GOOD_INVOICES.subList(0, 1));
		invoicesByCustomer.put(GOOD_CUSTOMERS.get(1), GOOD_INVOICES.subList(1, 4));
		invoicesByCustomer.put(GOOD_CUSTOMERS.get(2), GOOD_INVOICES.subList(4, 6));
				
		Stream<Invoice> overdueInvoices = 
				IntStream.of(3, 5, 0).mapToObj(GOOD_INVOICES::get);
		
		List<CustomerAndVolume> customersAndVolume = new ArrayList<>();
		
		CustomerAndVolume cv1 = mock(CustomerAndVolume.class);
		when(cv1.getCustomer()).thenReturn(GOOD_CUSTOMERS.get(2));
		when(cv1.getVolume()).thenReturn(1100.0);
		customersAndVolume.add(cv1);
		
		CustomerAndVolume cv2 = mock(CustomerAndVolume.class);
		when(cv2.getCustomer()).thenReturn(GOOD_CUSTOMERS.get(1));
		when(cv2.getVolume()).thenReturn(900.0);
		customersAndVolume.add(cv2);
		
		CustomerAndVolume cv3 = mock(CustomerAndVolume.class);
		when(cv3.getCustomer()).thenReturn(GOOD_CUSTOMERS.get(0));
		when(cv3.getVolume()).thenReturn(100.0);
		customersAndVolume.add(cv3);

		Map<Customer, Double> customersAndVolumeMap = customersAndVolume.stream().collect(Collectors.toMap(c -> c.getCustomer(), v -> v.getVolume()));

		mockBilling = mock(Billing.class);
		when(mockBilling.getCustomers()).thenReturn(GOOD_CUSTOMERS_MAP);
		when(mockBilling.getInvoices()).thenReturn(GOOD_INVOICES);
		when(mockBilling.getInvoicesOrderedByNumber()).thenReturn(GOOD_INVOICES.stream().sorted(Comparator.comparing(Invoice::getNumber)));
		when(mockBilling.getInvoicesOrderedByDate()).thenReturn(GOOD_INVOICES.stream().sorted(Comparator.comparing(Invoice::getIssueDate)));
		when(mockBilling.getInvoicesForCustomer(GOOD_CUSTOMERS.get(0))).thenReturn(invoicesByCustomer.get(GOOD_CUSTOMERS.get(0)).stream());
		when(mockBilling.getInvoicesForCustomer(GOOD_CUSTOMERS.get(1))).thenReturn(invoicesByCustomer.get(GOOD_CUSTOMERS.get(1)).stream());
		when(mockBilling.getInvoicesForCustomer(GOOD_CUSTOMERS.get(2))).thenReturn(invoicesByCustomer.get(GOOD_CUSTOMERS.get(2)).stream());
		when(mockBilling.getInvoicesGroupedByCustomer()).thenReturn(invoicesByCustomer);
		when(mockBilling.getOverdueInvoices(AS_OF_DATE)).thenReturn(overdueInvoices);
		when(mockBilling.getVolumeForCustomer(GOOD_CUSTOMERS.get(0))).thenReturn(1100.0);
		when(mockBilling.getVolumeForCustomer(GOOD_CUSTOMERS.get(1))).thenReturn(900.0);
		when(mockBilling.getVolumeForCustomer(GOOD_CUSTOMERS.get(2))).thenReturn(100.0);
		when(mockBilling.getCustomersAndVolume()).thenReturn(customersAndVolumeMap);
		when(mockBilling.getCustomersAndVolumeStream()).thenReturn(customersAndVolume.stream());

		reporter = new Reporter(mockBilling, OUTPUT_FOLDER, AS_OF_DATE);

		ArgumentCaptor<Consumer> customerCaptor = ArgumentCaptor.forClass(Consumer.class);
		verify(mockBilling).addCustomerListener(customerCaptor.capture());
		customerListener = customerCaptor.getValue();

		ArgumentCaptor<Consumer> invoiceCaptor = ArgumentCaptor.forClass(Consumer.class);
		verify(mockBilling).addInvoiceListener(invoiceCaptor.capture());
		invoiceListener = invoiceCaptor.getValue();
	}

	@Test
	public void testReportInvoicesOrderedByNumber(){
		reporter.reportInvoicesOrderedByNumber();
		assertCorrectOutput(Reporter.FILENAME_INVOICES_BY_NUMBER);
	}

	@Test
	public void testReportInvoicesGroupedByCustomer(){
		reporter.reportInvoicesGroupedByCustomer();
		assertCorrectOutput(Reporter.FILENAME_INVOICES_BY_CUSTOMER);
	}

	@Test
	public void testReportOverdueInvoices(){
		reporter.reportOverdueInvoices();
		assertCorrectOutput(Reporter.FILENAME_OVERDUE_INVOICES);
	}

	@Test
	public void testReportCustomersAndVolume(){
		reporter.reportCustomersAndVolume();
		assertCorrectOutput(Reporter.FILENAME_CUSTOMERS_AND_VOLUME);
	}

	@Test
	public void testCustomerListener(){
		customerListener.accept(null);
		assertCorrectOutput(Reporter.FILENAME_CUSTOMERS_AND_VOLUME);
	}

	@Test
	public void testInvoiceListener(){
		invoiceListener.accept(null);
		assertCorrectOutput(Reporter.FILENAME_INVOICES_BY_NUMBER);
		assertCorrectOutput(Reporter.FILENAME_INVOICES_BY_CUSTOMER);
		assertCorrectOutput(Reporter.FILENAME_OVERDUE_INVOICES);
		assertCorrectOutput(Reporter.FILENAME_CUSTOMERS_AND_VOLUME);
	}
	
}
