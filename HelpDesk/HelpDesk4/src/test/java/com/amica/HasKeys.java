package com.amica;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

/**
 * Matcher for a stream that focuses on a key value in the stream's expected
 * elements. Provide a key-extractor function (along the lines of 
 * MyElementType::getKey) and the values expected to be in the stream.
 * We will match if the stream, as mapped using the key extractor,
 * has exactly that sequence of keys -- no more, no fewer, and in the
 * same order as given.
 *
 * @author Will Provost
 */
public class HasKeys<T,U> extends TypeSafeMatcher<Stream<? extends T>>{

	private Function<? super T,U> keyExtractor;
	private Object[] expectedKeys;
	private String expected;
	private String was;
	
	@SafeVarargs // We use U... as an Object[]
	public HasKeys(Function<? super T,U> keyExtractor, U... expectedKeys) {
		this.keyExtractor = keyExtractor;
		this.expectedKeys = expectedKeys;
		expected = Arrays.stream(expectedKeys)
				.map(Object::toString)
				.collect(Collectors.joining(", ", "[ ", " ]"));		
	}
	
	public HasKeys(Function<? super T,U> keyExtractor, List<U> expectedKeys) {
		this.keyExtractor = keyExtractor;
		this.expectedKeys = expectedKeys.toArray();
		expected = expectedKeys.stream()
				.map(Object::toString)
				.collect(Collectors.joining(", ", "[ ", " ]"));		
	}
	
	public void describeTo(Description description) {
		description.appendText("stream with keys ");
		description.appendText(expected);
	}
	
	@Override
	public void describeMismatchSafely
			(Stream<? extends T> stream, Description description) {
		description.appendText("was: stream with keys ");
		description.appendText(was);
	}

	protected boolean matchesSafely(Stream<? extends T> stream) {

		Iterator<U> actual = stream.map(keyExtractor).iterator();
		Iterator<Object> expected = Arrays.stream(expectedKeys).iterator();
		boolean match = true;
		List<U> values = new ArrayList<>();
		while (actual.hasNext() && expected.hasNext()) {
			U value = actual.next();
			values.add(value);
			if (!value.equals(expected.next())) {
				match = false;
			}
		}
		if (actual.hasNext() && expected.hasNext()) {
			match= false;
		}

		while (actual.hasNext()) {
			values.add(actual.next());
		}
		was = values.stream()
				.map(Object::toString)
				.collect(Collectors.joining(", ", "[ ", " ]"));
		
		return match;
		
	}
	
	@SafeVarargs
	public static <T,U> HasKeys<T,U> hasKeys
			(Function<? super T,U> keyExtractor, U... expectedKeys) {
		return new HasKeys<T,U>(keyExtractor, expectedKeys);
	}
	
	public static <T,U> HasKeys<T,U> hasKeys
			(Function<? super T,U> keyExtractor,List<U> expectedKeys) {
		return new HasKeys<T,U>(keyExtractor, expectedKeys);
	}
}
