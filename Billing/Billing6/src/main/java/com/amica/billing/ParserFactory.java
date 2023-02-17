package com.amica.billing;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Level;

import com.amica.billing.parse.CSVParser;
import com.amica.billing.parse.FlatParser;
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

	public static final String CONFIGURAIONT_NAME = "Billing";
	public static final String BASE_PROPERTY_NAME = 
			ParserFactory.class.getSimpleName();

	private static Map<String,Supplier<Parser>> parsers = new HashMap<>();
	static {
		resetParsers();
	}
	
	/**
	 * Helper function to use the Java Reflection API to create a class
	 * of the requested name, and to handle any exceptions.
	 */
	public static Parser createParserWithClassName(String className) {
		try {
			return (Parser) Class.forName(className)
					.getConstructor(new Class<?>[0])
					.newInstance();
		} catch (Exception ex) {
			log.log(Level.WARNING, 
					"Couldn't create parser factory as configured", ex);
		}
		
		return null;
	}
	
	public static void resetParsers() {
		parsers.put("csv", CSVParser::new);
		parsers.put("flat", FlatParser::new);
		parsers.put(null, CSVParser::new);

		if (System.getProperty("env.name") != null) {
			Configuration configuration = ComponentConfigurationManager.getInstance()
					.getConfiguration(CONFIGURAIONT_NAME);

			Iterable<String> keys = () -> configuration.getKeys();
			for (String key : keys) {
				if (key.startsWith(BASE_PROPERTY_NAME)) {
					String extension = key.substring(BASE_PROPERTY_NAME.length());
					if (extension.length() != 0) {
						if (extension.startsWith(".")) {
							parsers.put(extension.substring(1),
								() -> createParserWithClassName
									(configuration.getString(key)));
							log.info(String.format("Configured parser %s=%s",
									extension, configuration.getString(key)));
						} else {
							log.warning("Couldn't parser property: " + key +
									"; correct format is " + BASE_PROPERTY_NAME +
									" or " + BASE_PROPERTY_NAME + ".extension");
						}
					} else {
						parsers.put(null, () -> createParserWithClassName
								(configuration.getString(key)));
						log.info(String.format("Configured default parser %s",
								configuration.getString(key)));
					}
				}

			}
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
