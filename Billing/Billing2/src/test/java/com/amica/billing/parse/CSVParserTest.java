package com.amica.billing.parse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import com.amica.billing.Customer;
import com.amica.billing.Invoice;
import com.amica.billing.TestUtility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.amica.billing.TestUtility.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Unit test for the {@link CSVParser}. Relies on data sets in the 
 * {@link TestUtility} and its own CSV representations of those data sets,
 * help in memory as lists of strings, to drive the parsing and producing
 * methods and expect clean translations between string and object forms.
 * 
 * @author Will Provost
 */
public class CSVParserTest {
	CSVParser parser;

	public static final List<String> GOOD_CUSTOMER_DATA = Stream.of
			("Customer,One,CASH",
			 "Customer,Two,45",
			 "Customer,Three,30").toList();
	
	public static final List<String> BAD_CUSTOMER_DATA = Stream.of 
			("Customer,One,CASHY_MONEY", 
			 "Customer,Two",
			 "Customer,Three,30").toList();

	public static final List<String> GOOD_INVOICE_DATA = Stream.of 
			("1,Customer,One,100,2022-01-04",
			 "2,Customer,Two,200,2022-01-04,2022-01-05",
			 "3,Customer,Two,300,2022-01-06",
			 "4,Customer,Two,400,2021-11-11",
			 "5,Customer,Three,500,2022-01-04,2022-01-08",
			 "6,Customer,Three,600,2021-12-04").toList();
	
	public static final List<String> BAD_INVOICE_DATA = Stream.of
			("1,Customer,One,100,2022-01-04",
			 "2,Customer,Two,200,2022-01-04,2022-01-05",
			 "3,Customer,Two,300",
			 "4,Customer,Four,400,2021-11-11",
			 "5,Customer,Three,500,2022-01-04,20220108",
			 "6,Customer,Three,600,2021-12-04").toList();

	@BeforeEach
	public void setUp(){
		parser = new CSVParser();
	}

	@Test
	public void testParseGoodCustomers(){
		List<Customer> customerList = parser.parseCustomers(GOOD_CUSTOMER_DATA.stream()).collect(Collectors.toList());
		assertThat( customerList, sameAsList(GOOD_CUSTOMERS) );
	}

	@Test
	public void testParseBadCustomers(){
		List<Customer> customerList = parser.parseCustomers(BAD_CUSTOMER_DATA.stream()).collect(Collectors.toList());
		assertThat( customerList, sameAsList(BAD_CUSTOMERS) );
	}

	@Test
	public void testParseGoodInvoices(){
		List<Invoice> invoiceList = parser.parseInvoices(GOOD_INVOICE_DATA.stream(), GOOD_CUSTOMERS_MAP).collect(Collectors.toList());
		assertThat( invoiceList, sameAsList(GOOD_INVOICES) );
	}

	@Test
	public void testParseBadInvoices(){
		List<Invoice> invoiceList = parser.parseInvoices(BAD_INVOICE_DATA.stream(), GOOD_CUSTOMERS_MAP).collect(Collectors.toList());
		assertThat( invoiceList, sameAsList(BAD_INVOICES) );
	}

	@Test
	public void testProduceCustomer(){
		Stream<Customer> customerStream = parser.parseCustomers(GOOD_CUSTOMER_DATA.stream());
		List<String> producedList = parser.produceCustomers(customerStream).collect(Collectors.toList());

		assertThat(producedList, containsInAnyOrder("Customer,One,CASH", "Customer,Two,45", "Customer,Three,30"));
	}

	@Test
	public void testProduceInvoice(){
		Stream<Invoice> invoiceStream = parser.parseInvoices(GOOD_INVOICE_DATA.stream(), GOOD_CUSTOMERS_MAP);
		List<String> producedList = parser.produceInvoices(invoiceStream).map(i -> i.replace(".00", "")).collect(Collectors.toList());

		assertThat( producedList, containsInAnyOrder("1,Customer,One,100,2022-01-04", "2,Customer,Two,200,2022-01-04,2022-01-05", "3,Customer,Two,300,2022-01-06", "4,Customer,Two,400,2021-11-11", "5,Customer,Three,500,2022-01-04,2022-01-08", "6,Customer,Three,600,2021-12-04") );
	}

}
