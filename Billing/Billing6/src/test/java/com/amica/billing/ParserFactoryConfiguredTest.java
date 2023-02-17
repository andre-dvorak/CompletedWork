package com.amica.billing;


import static com.amica.billing.ParserFactory.createParser;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.amica.billing.parse.FlatParser;
import com.amica.billing.parse.QuotedCSVParser;
import com.amica.esa.componentconfiguration.manager.ComponentConfigurationManager;

/**
 * Test of the {@link ParserFactory} component in a 
 * configuration-manager environment.
 *
 * @author Will Provost
 */
public class ParserFactoryConfiguredTest {

	@BeforeAll
	public static void setUpBeforeAll() {
		System.setProperty("env.name", "Quoted");
		ComponentConfigurationManager.getInstance().initialize();
		ParserFactory.resetParsers();
	}
	
	@Test
	public void testDefaultParser() {
		assertThat(createParser("any.xxx"), instanceOf(FlatParser.class));
	}
	
	@Test
	public void testCSVParser() {
		assertThat(createParser("any.csv"), instanceOf(QuotedCSVParser.class));
	}
}

