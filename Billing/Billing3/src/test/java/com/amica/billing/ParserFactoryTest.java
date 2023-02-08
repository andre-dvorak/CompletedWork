package com.amica.billing;

import static com.amica.billing.ParserFactory.createParser;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.amica.billing.parse.CSVParser;
import com.amica.billing.parse.FlatParser;
import com.amica.billing.parse.Parser;

/**
 * Unit test for the {@link ParserFactory}
 * We check the basic functioning of the factory in translating file extensions
 * to instances of appropriate parsers, and adding/overriding standard parsers
 * programmatically. We use a mock of the {@link Parser} interface, and set up
 * a supplier of that object, in order to test that the parser factory can
 * accurately reflect new and replaced parsers. 
 * 
 * @author Will Provost
 */
public class ParserFactoryTest {

	private Parser mockParser = mock(Parser.class);
	private Supplier<Parser> mockParserFactory = () -> mockParser;
	
	@BeforeEach
	public void setUp() {
		ParserFactory.resetParsers();
	}
	
	@Test
	public void testCreateParser_CSVFilename() {
		assertThat(createParser("any.csv"), instanceOf(CSVParser.class));
	}
	
	@Test
	public void testCreateParser_FlatFilename () {
		assertThat(createParser("any.flat"), instanceOf(FlatParser.class));
	}
	
	@Test
	public void testCreateParser_FlatFilename_UpperCase() {
		assertThat(createParser("any.FLAT"), instanceOf(FlatParser.class));
	}
	
	@Test
	public void testCreateParser_TXTFilename_LowerCase() {
		ParserFactory.addParser("TXT", mockParserFactory);
		assertThat(createParser("any.txt"), equalTo(mockParser));
	}
	
	@Test
	public void testCreateParser_UnknownExtension() {
		assertThat(createParser("x.y.z"), instanceOf(CSVParser.class));
	}
	
	@Test
	public void testCreateParser_NoExtension() {
		assertThat(createParser("xyz"), instanceOf(CSVParser.class));
	}
	
	@Test
	public void testAddParser() {
		ParserFactory.addParser("xxx", mockParserFactory);
		assertThat(createParser("any.xxx"), equalTo(mockParser));
	}
	
	@Test
	public void testAddParser_Existing() {
		assertThrows(IllegalArgumentException.class, 
				() -> ParserFactory.addParser("csv", mockParserFactory));
		assertThat(createParser("any.csv"), instanceOf(CSVParser.class));
	}
	
	@Test
	public void testReplaceParser() {
		ParserFactory.replaceParser("csv", mockParserFactory);
		assertThat(createParser("any.csv"), equalTo(mockParser));
	}
	
	@Test
	public void testReplaceParser_Missing() {
		assertThrows(IllegalArgumentException.class, 
				() -> ParserFactory.replaceParser("xxx", mockParserFactory));
		assertThat(createParser("any.xxx"), instanceOf(CSVParser.class));
	}
}
