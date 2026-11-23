/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */
package com.jstevenperry.sportsnet.framework.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Properties;

/**
 * This class is an abstraction over a single properties file.
 * You pass the name of the file (CLASSPATH-relative) and then you can
 * use this class to deal with the Properties file.
 * <p>
 * The Properties class is pretty nice, so this class doesn't
 * represent any real upgrade, and is purely for pedagogical purposes.
 *
 * @author sperry
 */
@SuppressWarnings("unused")  // Framework classes do not need this inspection
public class PropertiesFile {

    private static final Logger log = LoggerFactory.getLogger(PropertiesFile.class);

    private String propertiesFileName;

    private final Properties properties;

    /**
     * Constructor - call this constructor to "wrap" a Properties file with this class.
     * Yes, it's just that easy.
     *
     * @param propertiesFileName The name of the Properties file - relative to the
     *                           CLASSPATH - that you want to process using this class.
     */
    public PropertiesFile(String propertiesFileName) {

        // Store the properties file name
        this.propertiesFileName = propertiesFileName;

        // Create the Properties object
        properties = new Properties();

        // Load the properties file into the Properties object
        try {
            properties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream(propertiesFileName));
            log.info("Properties file '" + propertiesFileName + "' loaded.");
        } catch (IOException e) {
            String message =
                    "Exception while reading properties file '" + propertiesFileName + "':" + e.getLocalizedMessage();
            log.error(message, e);
            // Let's nip this in the bud
            throw new RuntimeException(message, e);
        }
    }

    public PropertiesFile(Properties properties) {
        this.properties = properties;
    }

    /**
     * Fetches a single property whose value will be returned as a String.
     *
     * @param propertyName The name of the property (e.g., the.property.name)
     * @return String - the value of the property as a String (e.g., "propertyValue")
     */
    public String getProperty(String propertyName) {
        String ret;
        log.trace("Looking for property '" + propertyName + "'...");

        // Read the property
        ret = properties.getProperty(propertyName);

        log.trace("Property value is '" + ret + "'");

        return ret;
    }

    /**
     * Adds a property to this Properties file.
     */
    public void putProperty(String propertyName, String propertyValue) {
        properties.put(propertyName, propertyValue);
    }

    /**
     * Saves the properties object to a new file, along with
     * the COPYRIGHT statement.
     * <p>
     * All properties that have been added will be stored as well.
     */
    public void save(boolean makeBackup) {
        String outputFileName = this.propertiesFileName + ((makeBackup) ? ".bak" : "");

        try {
            // Get the CLASSPATH root directory
            String classpathRoot = Objects.requireNonNull(getClass().getResource("/")).getPath();

            // Open an OutputStream to write the properties backup file to
            FileOutputStream fileOutputStream = new FileOutputStream(classpathRoot + File.separator + outputFileName);

            // Write the properties. Use the COPYRIGHT as the comments.
            properties.store(fileOutputStream, fetchCopyright());
        } catch (IOException e) {
            String message = "Exception occurred: " + e.getLocalizedMessage();
            log.error(message, e);
        }
    }

    /**
     * Reads the COPYRIGHT statement from the project and returns it
     * to the caller.
     *
     * @return String - the COPYRIGHT statement
     * @throws IOException - if something goes horribly wrong.
     */
    private String fetchCopyright() throws IOException {
        String ret;

        byte[] bytes = Files.readAllBytes(Paths.get("./COPYRIGHT"));

        ret = new String(bytes);

        return ret;
    }

}