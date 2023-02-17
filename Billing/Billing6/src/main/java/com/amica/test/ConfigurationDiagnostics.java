/*
Copyright 2017-2021 Will Provost.
All rights reserved by Capstone Courseware, LLC.
*/

package com.amica.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
<p>Class utility that offers methods to diagnose incorrect configurations.
Since we only inspect the initialized configuration -- and given that
it could be created in any number of ways, including using in-memory
data structures -- we are limited to helper methods that can check for
required properties, chase down a configured Java type to prove that it
exists, etc. For deeper diagnostics into e.g. configuration manager
setup, see the {@link ConfigMgrDiagnostics} subclass.</p>

@author Will Provost
*/
public class ConfigurationDiagnostics {

    /**
    Summary classification of issues (if any) with the configuration.
    */
    public enum Summary { GOOD, BAD_CONFIG, NO_CATEGORY, NO_PROPERTY };

    /**
    Hierarchical node in a structured report.
    */
    public static class Item {
        private String text;
        private List<Item> children = new ArrayList<>();

        /**
        Build based on a report string.
        */
        public Item(String text) {
            this.text = text;
        }

        /**
        Get the report string.
        */
        public String getText() {
            return text;
        }

        /**
        Add a child item.
        */
        public void add(Item child) {
            children.add(child);
        }

        /**
        Create an item based on the given report string,
        and add it as a child.
        */
        public Item add(String text) {
            Item child = new Item(text);
            children.add(child);
            return child;
        }

        /**
        Returns true if this node has any children.
        */
        public boolean hasChildren() {
            return !children.isEmpty();
        }

        /**
        Get this node's child nodes.
        */
        public Iterable<Item> getChildren() {
            return children;
        }
    }

    /**
    Top-level item that includes a summary of the issue(s) found.
    */
    public static class Report extends Item {
        private Summary summary;

        /**
        Build as an empty list of items with a summary classification.
        */
        public Report(Summary summary) {
            super("");
            this.summary = summary;
        }

        /**
        Returns the summary classification.
        */
        public Summary getSummary() {
            return summary;
        }
    }

    private static final String END_LINE = System.getProperty("line.separator");

    /**
    Recursive helper method to build up the indented-text report.
    */
    private static void format(Item item, String indent,
                StringBuilder builder) {
        final String INCREMENT = "  ";

        builder.append(indent).append(item.getText()).append(END_LINE);
        indent += INCREMENT;
        for (Item child : item.getChildren()) {
            format(child, indent, builder);
        }
    }

    /**
    Formatting utility that produces an indented-text report.
    */
    public static String format(Item item) {
        StringBuilder builder = new StringBuilder();
        for (Item child : item.getChildren()) {
            format(child, "", builder);
            builder.append(END_LINE);
        }
        return builder.toString();
    }

    /**
    Recursive helper method to build up the HTML report.
    */
    private static void formatHTML(Item item, String tag, StringBuilder builder) {

        builder.append("<").append(tag).append(">").append(item.getText())
               .append("</").append(tag).append(">");
        for (Item child : item.getChildren()) {
            builder.append("<ul>");
            formatHTML(child, "li", builder);
            builder.append("</ul>");
        }
    }

    /**
    Formatting utility that produces an HTML report.
    */
    public static String formatHTML(Item item) {
        StringBuilder builder = new StringBuilder();

        for (Item child : item.getChildren()) {
            formatHTML(child, "p", builder);
        }
        return builder.toString();
    }

    protected Map<String,String> properties;

    /**
     * Use this constructor in order to defer creation of the configuration
     * object. Used by {@link ConfigMgrDiagnostics} so that it can perform
     * as much of its work as possible before invoking the configuration
     * manager itself.
     */
    protected ConfigurationDiagnostics() {}
    
    /**
     * Pass a prepared Map to this constructor, such as you would create
     * in order to use a PropertiesConfiguration instead of the full
     * configuration manager.
     */
    public ConfigurationDiagnostics(Properties props) {
    	properties = new HashMap<>();
    	for (Object key : props.keySet()) {
    		properties.put((String) key, (String) props.get(key));
    	}
    }

    /**
     * Accessor for the configuration, if any.
     */
    public Map<String,String> getProperties() {
    	return Collections.unmodifiableMap(properties);
    }

    /**
    Helper method to check for a single property in the config.
    Adds an error report to the given parent if not found.
    */
    public String checkRequiredProperty(String name, Item notFound) {
        if (!properties.containsKey(name)) {
            notFound.add(name);
        }
        return properties.get(name);
    }
    
    public String getString(String name) {
    	return properties.get(name);
    }
    
    public boolean getBoolean(String name, boolean defaultValue) {
    	return properties.containsKey(name)
    		? properties.get(name).equals("true")
    		: defaultValue;
    }

    /**
    Helper method to check that a single property is found in the config,
    and that it names a class that can be loaded from the class path.
    Adds an error to the given parent if the property is not found, and
    returns a new item if found but not a loadable class.
    */
    public Item checkRequiredClassnameProperty(String name, Item notFound) {
        String className = properties.get(name);
        if (className == null) {
            notFound.add(name);
        } else {
            try {
                Class.forName(className);
            } catch (ClassNotFoundException ex) {
                Item noDriver = new Item("The " + name + " property was found, " +
                        "but the value " + className + " doesn't identify a " +
                        "Java class that can be found on the class path.");
                noDriver.add("Check your POM to be sure that the library is there;");
                noDriver.add("or, perhaps it needs to be installed in your server class path.");
                return noDriver;
            }
        }
        return null;
    }

}
