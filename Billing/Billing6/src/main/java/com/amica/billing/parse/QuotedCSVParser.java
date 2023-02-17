package com.amica.billing.parse;

import static com.amica.billing.parse.CSVParser.*;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.amica.billing.Customer;
import com.amica.billing.Invoice;

/**
 * A parser that can read a CSV format with certain expected columns.
 * String values are enclosed in double quotation marks (&quot;).
 *
 * @author Will Provost
 */
public class QuotedCSVParser implements Parser {

	private CSVParser csvParser = new CSVParser();

	private void stripQuotes(String[] fields, int index) {
		if (fields.length > index) {
			fields[index] = fields[index].substring(1, fields[index].length() - 1);
		}
	}

	private void addQuotes(String[] fields, int index) {
		if (fields.length > index) {
			fields[index] = "\"" + fields[index] + "\"";
		}
	}

	private String stripQuotesFromCustomer(String line) {
		String[] fields = line.split(",");
		stripQuotes(fields, CUSTOMER_FIRST_NAME_COLUMN);
		stripQuotes(fields, CUSTOMER_LAST_NAME_COLUMN);
		return Arrays.stream(fields).collect(Collectors.joining(","));
	}

	private String stripQuotesFromInvoice(String line) {
		String[] fields = line.split(",");
		stripQuotes(fields, INVOICE_FIRST_NAME_COLUMN);
		stripQuotes(fields, INVOICE_LAST_NAME_COLUMN);
		return Arrays.stream(fields).collect(Collectors.joining(","));
	}

	private String addQuotesToCustomer(String line) {
		String[] fields = line.split(",");
		addQuotes(fields, CUSTOMER_FIRST_NAME_COLUMN);
		addQuotes(fields, CUSTOMER_LAST_NAME_COLUMN);
		return Arrays.stream(fields).collect(Collectors.joining(","));
	}

	private String addQuotesToInvoice(String line) {
		String[] fields = line.split(",");
		addQuotes(fields, INVOICE_FIRST_NAME_COLUMN);
		addQuotes(fields, INVOICE_LAST_NAME_COLUMN);
		return Arrays.stream(fields).collect(Collectors.joining(","));
	}

	/**
	 * Strip quotes from specific fields, and delegate to thge CSV parser.
	 */
	public Stream<Customer> parseCustomers(Stream<String> customerLines) {

		return csvParser.parseCustomers(customerLines
				.map(this::stripQuotesFromCustomer));

	}

	/**
	 * Strip quotes from specific fields, and delegate to thge CSV parser.
	 */
	public Stream<Invoice> parseInvoices(Stream<String> invoiceLines,
			Map<String, Customer> customers) {

		return csvParser.parseInvoices(invoiceLines
				.map(this::stripQuotesFromInvoice), customers);
	}

	/**
	 * Delegate to the CSV parser, and add quotes to specific fields
	 * in the returned strings.
	 */
	public Stream<String> produceCustomers(Stream<Customer> customers) {
		return csvParser.produceCustomers(customers)
				.map(this::addQuotesToCustomer);
	}

	/**
	 * Delegate to the CSV parser, and add quotes to specific fields
	 * in the returned strings.
	 */
	public Stream<String> produceInvoices(Stream<Invoice> invoices) {
		return csvParser.produceInvoices(invoices)
				.map(this::addQuotesToInvoice);

	}
}
