/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */
package jdk.xml.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 * This class contains utility methods for reading resources in the JAXP packages
 */
public class SecuritySupport {
    /**
     * Cache for properties in java.home/conf/jaxp.properties
     */
    static final Properties cacheProps = new Properties();

    /**
     * Flag indicating whether java.home/conf/jaxp.properties has been read
     */
    static volatile boolean firstTime = true;

    private SecuritySupport() {}

    public static String getErrorMessage(Locale locale, String bundle, String key,
            Object[] arguments) {
        ResourceBundle rb;
        if (locale != null) {
            rb = ResourceBundle.getBundle(bundle,locale);
        } else {
            rb = ResourceBundle.getBundle(bundle);
        }

        String msg = rb.getString(key);
        if (arguments != null) {
            msg = MessageFormat.format(msg, arguments);
        }
        return msg;
    }

    /**
     * Reads JAXP system property with privilege
     *
     * @param propName the name of the property
     * @return the value of the property
     */
    public static String getSystemProperty(final String propName) {
        return
        AccessController.doPrivileged(
                (PrivilegedAction<String>) () -> (String)System.getProperty(propName));
    }

    /**
     * Reads a system property.
     *
     * @param <T> the type of the property value
     * @param type the type of the property value
     * @param propName the name of the property
     * @param defValue the default value
     * @return the value of the property, or the default value of no system
     * property is found
     */
    public static <T> T getJAXPSystemProperty(Class<T> type, String propName, String defValue) {
        String value = getJAXPSystemProperty(propName);
        if (value == null) {
            value = defValue;
        }
        if (Integer.class.isAssignableFrom(type)) {
            return type.cast(Integer.parseInt(value));
        } else if (Boolean.class.isAssignableFrom(type)) {
            return type.cast(Boolean.parseBoolean(value));
        }
        return type.cast(value);
    }

    /**
     * Reads JAXP system property in this order: system property,
     * $java.home/conf/jaxp.properties if the system property is not specified
     *
     * @param propName the name of the property
     * @return the value of the property
     */
    public static String getJAXPSystemProperty(String propName) {
        String value = getSystemProperty(propName);
        if (value == null) {
            value = readJAXPProperty(propName);
        }
        return value;
    }

    /**
     * Reads the specified property from $java.home/conf/jaxp.properties
     *
     * @param propName the name of the property
     * @return the value of the property
     */
    public static String readJAXPProperty(String propName) {
        String value = null;
        InputStream is = null;
        try {
            if (firstTime) {
                synchronized (cacheProps) {
                    if (firstTime) {
                        String configFile = getSystemProperty("java.home") + File.separator
                                + "conf" + File.separator + "jaxp.properties";
                        File f = new File(configFile);
                        if (getFileExists(f)) {
                            is = getFileInputStream(f);
                            cacheProps.load(is);
                        }
                        firstTime = false;
                    }
                }
            }
            value = cacheProps.getProperty(propName);

        } catch (IOException ex) {
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {}
            }
        }

        return value;
    }

//------------------- private methods ---------------------------
    static boolean getFileExists(final File f) {
        return (AccessController.doPrivileged((PrivilegedAction<Boolean>) ()
                -> f.exists() ? Boolean.TRUE : Boolean.FALSE));
    }

    static FileInputStream getFileInputStream(final File file)
            throws FileNotFoundException {
        try {
            return AccessController.doPrivileged((PrivilegedExceptionAction<FileInputStream>) ()
                    -> new FileInputStream(file));
        } catch (PrivilegedActionException e) {
            throw (FileNotFoundException) e.getException();
        }
    }
}
