package com.amica.billing.parse;

import static com.amica.billing.TestUtility.*;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.amica.billing.TestUtility;

/**
 * Unit test for the {@link FlatParser}. Relies on data sets in the 
 * {@link TestUtility} and its own CSV representations of those data sets,
 * help in memory as lists of strings, to drive the parsing and producing
 * methods and expect clean translations between string and object forms.
 * 
 * @author Will Provost
 */
public class FlatParserTest {

	public static final List<String> GOOD_CUSTOMER_DATA = Stream.of 
			("Customer    One         CASH      ",
			 "Customer    Two         CREDIT_45 ",
			 "Customer    Three       CREDIT_30 ").toList();
	
	public static final List<String> BAD_CUSTOMER_DATA = Stream.of
			("Customer    One         CASHY     ",
			 "Customer    Two",
			 "Customer    Three       CREDIT_30 ").toList();

	public static final List<String> GOOD_INVOICE_DATA = Stream.of 
			("   1Customer    One           100.00010422      ",
			 "   2Customer    Two           200.00010422010522",
			 "   3Customer    Two           300.00010622      ",
			 "   4Customer    Two           400.00111121      ",
			 "   5Customer    Three         500.00010422010822",
			 "   6Customer    Three         600.00120421      ").toList();
	
	public static final List<String> BAD_INVOICE_DATA = Stream.of
			("   1Customer    One              100010422      ",
			 "   2Customer    Two              200010422010522",
			 "   3Customer    Two",
			 "   4Customer    Two            400.0993021      ",
			 "   5Customer    Four             500010422010822",
			 "   6Customer    Three            600120421      ").toList();
	
	private Parser parser = new FlatParser();

	@Test
	public void testParseCustomers() {
		assertThat(parser.parseCustomers(GOOD_CUSTOMER_DATA.stream()).toList(), 
				sameAsList(GOOD_CUSTOMERS));
	}
	
	@Test
	public void testParseCustomers_Bad() {
		assertThat(parser.parseCustomers(BAD_CUSTOMER_DATA.stream()).toList(), 
				sameAsList(BAD_CUSTOMERS));
	}
	
	@Test
	public void testParseInvoices() {
		assertThat(parser.parseInvoices
			(GOOD_INVOICE_DATA.stream(), GOOD_CUSTOMERS_MAP).toList(), 
			sameAsList(GOOD_INVOICES));
	}
	
	@Test
	public void testParseInvoices_Bad() {
		assertThat(parser.parseInvoices
			(BAD_INVOICE_DATA.stream(), GOOD_CUSTOMERS_MAP).toList(), 
			sameAsList(BAD_INVOICES));
	}
	
	@Test
	public void testProduceCustomers() {
		assertThat(parser.produceCustomers(GOOD_CUSTOMERS.stream()).toList(),
				sameAsList(GOOD_CUSTOMER_DATA));
	}
	
	@Test
	public void testProduceInvoices() {
		assertThat(parser.produceInvoices(GOOD_INVOICES.stream()).toList(),
				sameAsList(GOOD_INVOICE_DATA));
	}
}
