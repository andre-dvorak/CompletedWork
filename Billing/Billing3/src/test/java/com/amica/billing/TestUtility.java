package com.amica.billing;

import static com.amica.HasKeys.hasKeys;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

/**
 * This class gathers various constants, data sets, and utility methods 
 * common to multiple tests.
 * 
 * @author Will Provost
 */
public class TestUtility {

	public static final String TEMP_FOLDER = "temp";
	public static final String OUTPUT_FOLDER = "reports";
	public static final String CUSTOMERS_FILENAME = "customers.csv";
	public static final String INVOICES_FILENAME = "invoices.csv";

	public static final LocalDate AS_OF_DATE = LocalDate.of(2022, 1, 8);
	
	public static final List<Customer> GOOD_CUSTOMERS = Stream.of
		(new Customer("Customer", "One", Terms.CASH),
		 new Customer("Customer", "Two", Terms.CREDIT_45),
		 new Customer("Customer", "Three", Terms.CREDIT_30)).toList();
	
	public static final Map<String,Customer> GOOD_CUSTOMERS_MAP =
		GOOD_CUSTOMERS.stream().collect(Collectors.toConcurrentMap
			(Customer::getName, Function.identity()));

	public static final List<Customer> BAD_CUSTOMERS = GOOD_CUSTOMERS.subList(2, 3);

	public static final List<Invoice> GOOD_INVOICES = Stream.of
		(new Invoice(1, GOOD_CUSTOMERS.get(0), 100, LocalDate.of(2022,  1,  4)),
		 new Invoice(2, GOOD_CUSTOMERS.get(1), 200, LocalDate.of(2022,  1,  4), LocalDate.of(2022, 1, 5)),
		 new Invoice(3, GOOD_CUSTOMERS.get(1), 300, LocalDate.of(2022,  1,  6)),
		 new Invoice(4, GOOD_CUSTOMERS.get(1), 400, LocalDate.of(2021, 11, 11)),
		 new Invoice(5, GOOD_CUSTOMERS.get(2), 500, LocalDate.of(2022,  1,  4), LocalDate.of(2022, 1, 8)),
		 new Invoice(6, GOOD_CUSTOMERS.get(2), 600, LocalDate.of(2021, 12,  4))).toList();
	
	public static final Map<Integer,Invoice> GOOD_INVOICES_MAP =
			GOOD_INVOICES.stream().collect(Collectors.toMap
					(Invoice::getNumber, Function.identity()));

	public static final List<Invoice> BAD_INVOICES = IntStream.of(0, 1, 5)
			.mapToObj(GOOD_INVOICES::get).toList();
	
	/**
	 * Helper to produce a list of matchers that assert each candidate object
	 * has the same properties as the corresponding list element.
	 */
	public static <T> List<Matcher<? super T>> samePropertiesAsList(List<T> source) {
		Stream<Matcher<? super T>> matchers = 
				source.stream().map(Matchers::samePropertyValuesAs);
		return matchers.toList();
	}

	/**
	 * Custom matcher that facilitates comparing two lists when we expect
	 * all properties of the compared elements to match.
	 */
	public static <T> Matcher<Iterable<? extends T>> sameAsList(List<T> source) {
		return contains(samePropertiesAsList(source));
	}
	
	/**
	 * Loads the contents of files at the two given paths,
	 * and asserts that they are the same, first subjecting the content of
	 * each file to the given canonicalization function.  
	 */
	public static void assertCorrectOutput(Path actualPath, 
			Path expectedPath, Function<String,String> canonicalizer) {
		try ( 
			Stream<String> actualLines = Files.lines(actualPath);
			Stream<String> expectedLines = Files.lines(expectedPath);
		) {
			String actual = actualLines.collect(Collectors.joining("\n"));
			String expected = expectedLines.collect(Collectors.joining("\n"));
			assertThat(canonicalizer.apply(actual), 
					equalTo(canonicalizer.apply(expected)));
		} catch (IOException ex) {
			fail("Couldn't open actual and expected file content.", ex);
		}
	}
	
	/**
	 * Loads the tonents of files at the two given paths,
	 * and asserts that they are exactly the same.  
	 */
	public static void assertCorrectOutput(Path actualPath, Path expectedPath) {
		assertCorrectOutput(actualPath, expectedPath, Function.identity());
	}

	/**
	 * Custom matcher that assures a stream of invoices has the expected
	 * invoice numbers, in order.
	 */
	public static Matcher<Stream<? extends Invoice>> hasNumbers(Integer... numbers) {
		return hasKeys(Invoice::getNumber, numbers);
	}

}
