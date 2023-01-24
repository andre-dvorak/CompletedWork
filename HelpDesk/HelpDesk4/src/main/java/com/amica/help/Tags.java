package com.amica.help;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * This class manages keyword tags, assuring that only one instance of the
 * {@link Tag} class is held for a given string, comparing the strings wihtout
 * sensitivity to case. It also maps synonyms to tags, and assures that any
 * attempt to resolve a string to a tag takes those synonyms into account.
 * 
 * @author Will Provost
 */
public class Tags {

	private SortedSet<Tag> tags = new TreeSet<>();
	private Map<String, Tag> synonyms = new HashMap<>();

	/**
	 * Returns a view of all tags.
	 */
	public SortedSet<Tag> getTags() {
		return Collections.unmodifiableSortedSet(tags);
	}

	/**
	 * Returns a view of known synonyms.
	 */
	public Map<String, Tag> getSynonyms() {
		return Collections.unmodifiableMap(synonyms);
	}

	/**
	 * Adds a synonym to our dictionary. This can have the effect of adding
	 * the "term" (the thing for which the first parameter is a synonym)
	 * as a tag for the first time; or it will find an existing tag that matches. 
	 */
	public void addSynonym(String synonym, String term) {
		synonyms.put(synonym.toLowerCase(), getTag(term));
	}

	/**
	 * Gets an existing tag with a matching value, or adds a new tag with
	 * the given value and returns that. If the given value is a synonym,
	 * returns the tag for the translated term. 
	 */
	public Tag getTag(String value) {
		
		if (synonyms.containsKey(value.toLowerCase())) {
			return synonyms.get(value.toLowerCase());
		}

		Tag candidate = new Tag(value);
		if (!tags.stream().anyMatch(candidate::equals)) {
			tags.add(candidate);
		}
		
		return tags.stream().filter(candidate::equals).findAny().get();
	}

}
