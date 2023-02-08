package com.amica.billing;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.amica.billing.parse.CSVParser;
import com.amica.billing.parse.FlatParser;
import com.amica.billing.parse.QuotedCSVParser;
import com.amica.billing.parse.Parser;

import com.amica.esa.componentconfiguration.manager.ComponentConfigurationManager;
import com.amica.escm.configuration.api.Configuration;
import lombok.extern.java.Log;

/**
 * A factory for parsers that determines which type of parser to create
 * based on the extension of given filenames.
 * 
 * @author Will Provost
 */
 @Log
public class ParserFactory {

 	private static final String PARSER_FACTORY = "ParserFactory";
 	private static final String PARSER_FACTORY_WITH_EXT = "ParserFactory\\.[A-Za-z]+";
	private static Map<String,Supplier<Parser>> parsers = new HashMap<>();
	static {
		resetParsers();
	}
	
	/**
	 * Helper function to use the Java Reflection API to create a class
	 * of the requested name, and to handle any exceptions.
	 */
	public static Parser createParserWithClassName(String className) {
		if (className.equals(CSVParser.class.getName())) {
			return new CSVParser();
		} else if (className.equals(FlatParser.class.getName())) {
			return new FlatParser();
		} else if (className.equals(QuotedCSVParser.class.getName())) {
			return new QuotedCSVParser();
		}
		
		return null;
	}
	
	public static void resetParsers() {
		parsers.put("csv", CSVParser::new);
		parsers.put("flat", FlatParser::new);
		parsers.put(null, CSVParser::new);

		String enProperty = System.getProperty("env.name");
		if(enProperty != null){
			ComponentConfigurationManager manager = ComponentConfigurationManager.getInstance();
			manager.initialize();
			Configuration configuration = manager.getConfiguration(Billing.CONFIG_NAME);

			Stream.generate(configuration.getKeys()::next).limit(configuration.size()).forEach(k -> {
				if( k.equals(PARSER_FACTORY) ){
					parsers.put( null, () -> createParserWithClassName(configuration.getString(k)) );
				}else if( k.matches(PARSER_FACTORY_WITH_EXT) ){
					String ext = k.substring(k.indexOf(".") + 1);
					parsers.put( ext, () -> createParserWithClassName(configuration.getString(k)) );
				}
			});
		}
	}

	public static void addParser(String extension, Supplier<Parser> factory) {
		if (!parsers.containsKey(extension)) {
			parsers.put(extension.toLowerCase(), factory);
		} else {
			throw new IllegalArgumentException
				("There is already a parser for extension " + extension + 
					"; use replaceParser() to replace it.");
		}
	}
	
	public static void replaceParser(String extension, Supplier<Parser> factory) {
		if (parsers.containsKey(extension)) {
			parsers.put(extension.toLowerCase(), factory);
		} else {
			throw new IllegalArgumentException
				("There is no parser for extension " + extension + 
					"; use addParser() to add one.");
		}
	}
	
	public static void replaceDefaultParser(Supplier<Parser> factory) {
		parsers.put(null, factory);
	}
	
	/**
	 * Looks up the file extension to find a 
	 * <code>Supplier&lt;Parser&gt;</code>, invokes it, and returns the result. 
	 */
	public static Parser createParser(String filename) {
		if (filename != null) {
			int index = filename.indexOf(".");
			if (index  != -1 && index != filename.length() - 1) {
				String extension = filename.substring(index + 1).toLowerCase();
				if (parsers.containsKey(extension)) {
					return parsers.get(extension).get();
				}
			}
		}
		return parsers.get(null).get();
	}
}
