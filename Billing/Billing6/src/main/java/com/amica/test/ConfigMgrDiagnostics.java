/*
Copyright 2017-2021 Will Provost.
All rights reserved by Capstone Courseware, LLC.
*/

package com.amica.test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
<p>Class utility that offers methods to diagnose incorrect configurations
when using the Amica configuration manager. Give the appropriate method
the same information you would be giving the config manager -- configuration
name, property name, and possibly the component name if you are providing that
programmatically to the config manager. The tool produces a report based on
a set of diagnostic approaches, at least one of which should catch the
most common errors and omissions in using the config manager.</p>

<p>Primary uses are:</p>
<ul>
  <li>In a Java SE or non-hosted context, call
    {@link #diagnose(String,String)}, passing the configuration/category
    name, and property name; or {@link #diagnose(String,String,String)},
    also passing the component name that you are passing this explicitly
    to the config-manager API.</li>
  <li>In a Servlets context, call {@link #diagnose(Object,String,String)},
    passing the servlets context, configuration/category name,
    and property name; or {@link #diagnose(Object,String,String,String)}
    in order also to provide the component name that you are passing
    to the config manager.</li>
</ul>

<p>This tool is not entirely non-invasive, in that it does attempt to use
the config manager itself; so, once it has encountered a failure, later
uses of the config manager may fail in unexpected ways. You should usually
insert a call to one of the above methods, diagnose the problem, and then
remove the call again, to avoid any surprises.</p>

@author Will Provost
*/
public class ConfigMgrDiagnostics extends ConfigurationDiagnostics {

    /**
    Walk the cause chain of the given error, and return the
    message from the root-cause exception or error.
    */
    public static String getRootMessage(ExceptionInInitializerError err) {
        final String THROWAWAY =
                "Resolver: EnvironmentDescriptorResolver - experienced errors during resolution process! Messages(s): ";

        Throwable throwable = err;
        while (throwable.getCause() != null) {
            throwable = throwable.getCause();
        }

        return throwable.getMessage().replace(THROWAWAY, "");
    }

    /**
    Call this method to diagnose problems in a Java-SE environment
    when your component.name property is set or you are relying on a
    single configured ComponentResources folder.
    It will produce an indented-text report to the console.
    */
    public static void diagnose(String category, String property) {
        System.out.print(format(getDiagnosis(category, property)));
    }

    /**
    Call this method to diagnose problems in a Java-SE environment
    when you are providing the component name to the config manager
    programmatically.
    It will produce an indented-text report to the console.
    */
    public static void diagnose
            (String componentName, String category, String property) {
        System.out.print(format(getDiagnosis
                (componentName, category, property)));
    }

    /**
    Call this method in a Java-SE environment. Note that most callers
    will prefer to call {@link #diagnose(String,String)}, which calls
    this method and then {@link #format(Item)} to format output to
    the console.

    @param category The name of the configuration or category you are
        trying to read (the name of the property file)
    @param property The name of the property you're trying to read
    @return The root node of a hierarchical data structure with
        error information. Call one of the formatting methods to
        turn this into readable text or HTML.
    */
    public static Report getDiagnosis(String category, String property) {
        return new ConfigMgrDiagnostics
                (category, property).getDiagnosis();
    }

    /**
    Call this method in a Java-SE environment. Note that most callers
    will prefer to call {@link #diagnose(String,String,String)}, which calls
    this method and then {@link #format(Item)} to format output to
    the console.

    @param componentName Pass any component name that you are passing to
        the config manager programmatically -- or <strong>null</strong>
        if you are relying on the component.name property or a single
        ComponentResources subfolder.
    @param category The name of the configuration or category you are
        trying to read (the name of the property file)
    @param property The name of the property you're trying to read
    @return The root node of a hierarchical data structure with
        error information. Call one of the formatting methods to
        turn this into readable text or HTML.
    */
    public static Report getDiagnosis
            (String componentName, String category, String property) {
        return new ConfigMgrDiagnostics
                (componentName, category, property).getDiagnosis();
    }

    /**
    Represents a node in a class-path resource tree.
    */
    private abstract class Node {
        protected File file;
        protected String name;

        /**
        Build a node on a file reference and optionally
        a user-friendly label.
        */
        public Node(File file, String name) {
            this.file = file;
            this.name = name;
        }

        /**
        Returns the given label for this node, or the file path if no label
        is given.
        */
        public String getName() {
            return name != null ? name : file.getPath();
        }

        /**
        Returns the absolute path of this node in the system.
        */
        public String getAbsolutePath() {
            return file.getAbsolutePath();
        }

        /**
        Returns a new node representing a subpath of this node.
        */
        public abstract Node resolve(String path);

        /**
        Returns true if this node is a directory or folder.
        */
        public abstract boolean isDirectory();

        /**
        Returns true if this node is a file, and not a folder.
        */
        public abstract boolean isFile();

        @Override
        public String toString() {
        	return file.getPath();
        }
    }

    /**
    Encapsulates a node on the local file system.
    */
    private class FileNode extends Node {

        /**
        Build based on a java.io.File.
        */
        public FileNode(File file) {
            super(file, null);
        }

        /**
        Build based on a java.io.File, and provide a user-friendly label.
        */
        public FileNode(File file, String name) {
            super(file, name);
        }

        /**
        Returns a file representing the given subpath.
        */
        public FileNode resolve(String path) {
            return new FileNode(new File(file, path), name + "/" + path);
        }

        /**
        Returns true if this node is a directory or folder.
        */
        public  boolean isDirectory() {
            return file.isDirectory();
        }

        /**
        Returns true if this node is a file, and not a folder.
        */
        public boolean isFile() {
            return file.isFile();
        }

        public File getFile() {
        	return file;
        }
    }

    private static final String COMPONENT_RESOURCES = "ComponentResources";
    private static final String ENVIRONMENT_RESOURCES = "EnvironmentResources";
    private static final String SHARED_RESOURCES = "SharedResources";

    private String componentName;
    private String category;
    private String property;

    private List<Node> classPath = new ArrayList<>();
    private Item attempt;
    private Item locations = new Item("Resource folders found:");
    private Item errors = new Item("Errors found in configuration:");;
    private List<File> propertyFiles = new ArrayList<>();
    private boolean noCategory = false;
    private boolean foundProperty = true;

    private Report report;

    /**
    Create a diagnosis tool for the given explicit component name,
    category, and property name. Delegates to
    {@link #ConfigDiagnostics(String,String)}.
    */
    public ConfigMgrDiagnostics(String componentName, String category, String property) {
        this.componentName = componentName;
        this.category = category;
        this.property = property;

        classPath.add(new FileNode(new File("src/main/resources")));

        File srcTestResources = new File("src/test/resources");
        if (srcTestResources.exists() && srcTestResources.isDirectory()) {
        	classPath.add(new FileNode(srcTestResources));
        }

        String alternate = System.getProperty("cfgmgr.classpath");
        if (alternate != null) {
        	classPath.add(new FileNode(new File(alternate)));
        }

        attempt = new Item("Attempting to read property as follows:");

        if (componentName != null) {
            attempt.add("configuration: " + componentName);
        } else {
            attempt.add("configuration: default (component.name is " +
                    System.getProperty("component.name", "<undefined>") +
                    ")");
        }

        attempt.add("category: " + category);
        attempt.add("property: " + property);

        Item cpConfirmation = new Item("Scanning the following locations:");
        classPath.stream().map(Node::toString).forEach(cpConfirmation::add);

        confirmConfiguration();

        Summary summary = Summary.GOOD;
        if (noCategory) {
            summary = Summary.NO_CATEGORY;
        } else if (errors.hasChildren()) {
                summary = Summary.BAD_CONFIG;
        } else if (!foundProperty) {
            summary = Summary.NO_PROPERTY;
        }

        report = new Report(summary);
        report.add(attempt);
        report.add(cpConfirmation);
        report.add(locations);

        if (!errors.hasChildren()) {
            errors = new Item("No errors detected in configuration.");
        }
        report.add(errors);

        if (!propertyFiles.isEmpty()) {
        	properties = propertyFiles.stream()
    				.map(File::getPath)
    				.map(Paths::get)
    				.flatMap(path -> {
    						try { return Files.lines(path); }
    						catch (IOException ex) { ex.printStackTrace(); }
    						return Stream.empty();
    					})
    				.filter(line -> !line.isEmpty())
    				.filter(line -> !line.startsWith("#"))
    				.map(line -> line.split(" *= *"))
    				.filter(nv -> nv.length == 2)
    				.collect(Collectors.toMap(nv -> nv[0], nv -> nv[1]));
        	if (property != null) {
        		if (properties.containsKey(property)) {
            		report.add(new Item(String.format
                			("Property \"%s\" found with value \"%s\"",
                				property, properties.get(property))));
                		foundProperty = true;
                	} else {
                		report.add(new Item(String.format
                				("Property \"%s\" not found.", property)));
                		foundProperty = false;
        		}
        	}
        }

    }

    /**
    Create a diagnosis tool for the given category and property name.
    We scan the class path and build a list of nodes for later analysis.
    We replace target folders with source folders, mostly to provide more
    intuitive output for the developer.
    */
    public ConfigMgrDiagnostics(String category, String property) {
        this(null, category, property);
    }

    /**
    Return the requested component name for this configuration.
    */
    public String getComponentName() {
        return componentName;
    }

    /**
    Return the requested category for this configuration.
    */
    public String getCategory() {
        return category;
    }

    /**
    Return the requested property name.
    */
    public String getPropertyName() {
        return property;
    }

    /**
    Find a given folder that is expected to hold property files.
    Report errors if missing or found in multiple locations.
    */
    private void findFolder(String path) {

        List<Node> found = new ArrayList<>();
        for (Node node : classPath) {
            if (node.resolve(path).isDirectory()) {
                found.add(node);
            }
        }

        if (found.isEmpty()) {
            errors.add("Missing required folder " + path);
        } else if (found.size() > 1) {
            Item error = errors.add("Found required folder " + path +
                   "in multiple locations:");
            for (Node node : found) {
                error.add(node.getName());
            }
        } else {
            locations.add(path + " found in " + found.get(0).getName());
            Node folder = found.get(0).resolve(path);
            Node propertyFile = folder.resolve(category + ".properties");
            if (propertyFile.isFile()) {
            	propertyFiles.add(((FileNode) propertyFile).getFile());
            }

            if (folder instanceof FileNode) {
                File folderAsFile = new File(folder.getAbsolutePath());
                if (folderAsFile.getAbsolutePath().startsWith
                        (new File("src").getAbsolutePath()) &&
                        folderAsFile.list().length == 0) {
                    errors.add(path + " was found, but it is empty, so it may not be found in the runtime class path.")
                        .add("Consider adding a file to this folder, even if it has no content.");
                }
            }
        }
    }

    /**
    Walk the source and target trees as given. Identify any files
    found in the target that are not in the source, which can be left behind
    in some cases when folders are renamed and which can trigger especially
    mysterious issues with the config manager.
    */
    private void compareSourceAndBuild
            (Item cruft, String source, String build) {
        SortedSet<String> sourceFiles = new TreeSet<>();
        SortedSet<String> cruftFiles = new TreeSet<>();

        try {
            File sourceFolder = new File(source);
            if (sourceFolder.isDirectory()) {
                Files.walk(sourceFolder.toPath())
                     .map(p -> p.toString().replace("\\", "/").replace(source, ""))
                     .forEach(p -> sourceFiles.add(p));
            }

            File buildFolder = new File(build);
            if (buildFolder.isDirectory()) {
                Files.walk(buildFolder.toPath())
                     .map(p -> p.toString().replace("\\", "/").replace(build, ""))
                     .filter(p -> !sourceFiles.contains(p))
                     .forEach(p -> cruftFiles.add(p));
            }
        } catch (IOException ex) {
            errors.add("IOException while walking source and build trees:")
                  .add(ex.getMessage());
            ex.printStackTrace();
        }

        for (String path : cruftFiles) {
            cruft.add(build + path);
        }
    }

    /**
    Master process of confirming the configuration. Calls
    {@link #findFolder findFolder} on each of
    <ul>
      <li>ComponentResources/<em>component.name</em></li>
      <li>EnvironmentResources/<em>server.env</em></li>
      <li>SharedResources</li>
    </ul>
    For the first of these we must account for programmatic component name,
    component.name property, or implicit component based on a single
    subfolder. Also calls {@link #compareSourceAndBuild compareSourceAndBuild}
    for each of the main and test resource trees; and assures that the
    desired property file is found in at least one resource folder.
    */
    private void confirmConfiguration() {

        if (componentName != null) {
            findFolder(COMPONENT_RESOURCES + "/" + componentName);
        } else {
            String cnProperty = System.getProperty("component.name");
            if (cnProperty != null) {
                findFolder(COMPONENT_RESOURCES + "/" + cnProperty);
            } else {
                List<Node> options = new ArrayList<>();
                int crCount = 0;
                for (Node candidate : classPath) {
                    Node crFolder = candidate.resolve(COMPONENT_RESOURCES);
                    if (crFolder.isDirectory() && crFolder instanceof FileNode) {
                        ++crCount;
                        for (File option : new File(crFolder.getAbsolutePath()).listFiles()) {
                            if (option.isDirectory()) {
                                options.add(crFolder.resolve(option.getName()));
                            }
                        }
                    }
                }

                if (crCount == 0) {
                    errors.add("Missing required folder " +
                            COMPONENT_RESOURCES);
                } else if (options.isEmpty()) {
                    errors.add("No subfolders of " +
                            COMPONENT_RESOURCES + " were found.");
                } else if (options.size() == 1) {
                    findFolder(COMPONENT_RESOURCES + "/" +
                            new File(options.get(0).getAbsolutePath()).getName());
                } else {
                    Item error = errors.add("Attempting to use default component configuration, but multiple folders were found under " +
                            COMPONENT_RESOURCES + ".");
                    Item multiple = error.add("Here are the locations:");
                    for (Node option : options) {
                        multiple.add(option.getName());
                    }
                    Item fixes = error.add("Possible fixes:");
                    fixes.add("Remove extraneous folders from your path");
                    fixes.add("Set the component.name environment variable");
                    fixes.add("Request a specific component configuration via the API");
                }
            }
        }

        String seProperty = System.getProperty("server.env");
        String enProperty = System.getProperty("env.name");
        if (seProperty != null) {
            findFolder(ENVIRONMENT_RESOURCES + "/" + seProperty);
        } else if (enProperty != null) {
                findFolder(ENVIRONMENT_RESOURCES + "/" + enProperty);
        } else {
            errors.add("Neither env.name nor server.env is defined.");
        }

        findFolder(SHARED_RESOURCES);

        if (propertyFiles.isEmpty()) {
        	if (!errors.hasChildren()) {
                noCategory = true;
            }
            errors.add("Missing required file " + category + ".properties")
                    .add("This file must be found in at least one of the resource folders.");
        }

        Item cruft = new Item("Paths found:");
        for (String resFolder : new String[]{ COMPONENT_RESOURCES,
                ENVIRONMENT_RESOURCES, SHARED_RESOURCES}) {
            compareSourceAndBuild(cruft, "src/main/resources/" + resFolder,
                    "target/classes/" + resFolder);
            compareSourceAndBuild(cruft, "src/test/resources/" + resFolder,
                    "target/test-classes/" + resFolder);
        }

        if (cruft.hasChildren()) {
            Item error = errors.add
                    ("Some files were found in your Maven build that are not in your source tree");
            error.add(cruft);
            error.add("Such files are often left behind when you change folder or file names.");
            error.add("Consider running a Maven clean, which will clear out any cruft in the target folders.");
        }


    }

    /**
    Compiles a complete diagnosis, including echoes of what is being
    requested, all errors from the
    {@link #confirmConfiguration confirmConfiguration} process,
    and finally invokes the config manager directly and attempts
    to translate any error results.
    */
    public Report getDiagnosis() {

        return report;
    }

    /**
    Returns true if at least one property file of the expected name
    was found in the configuration.
    */
    public boolean isFoundPropertyFile() {
        return !propertyFiles.isEmpty();
    }

    /**
    Returns true if a specific property was requested and found.
    */
    public boolean isFoundProperty() {
        return foundProperty & (property != null);
    }

    /**
     * Exposes a command-line interface to the diagnostics tool.
     * This is primarily used in automated testing of the course software.
     */
    public static void main(String[] args) {
    	if (args.length > 2) {
    		String envName = args[0];
    		String componentName = args[1];
    		String category = args[2];
    		String property = args.length > 3 ? args[3] : null;

    		System.setProperty("server.env", envName);
    		diagnose(componentName, category, property);
    	} else {
    		System.out.println("Usage: ConfgMgrDiagnostics <server.env> <component.name> <category> <property>");
    	}
    }
}
