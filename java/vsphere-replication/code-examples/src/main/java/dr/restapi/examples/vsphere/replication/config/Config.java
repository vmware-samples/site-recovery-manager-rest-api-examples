/* Copyright 2023 VMware, Inc. */
/* SPDX-License-Identifier: BSD-2-Clause */
package dr.restapi.examples.vsphere.replication.config;

import dr.restapi.examples.vsphere.replication.exceptions.ConfigNotInitializedException;
import dr.restapi.examples.vsphere.replication.exceptions.ConfigNotValidException;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Configuration for the code example scenarios.
 * <p>Configuration is stored in the project resource file {@code dr-rest-api-examples.properties}.
 * <p>Configuration can be dynamically extended at runtime by {@linkplain Config#cacheConfig(String, String)}.
 */
public final class Config {
   public static final String CONFIG_FILE = "dr-rest-api-examples.properties";

   private static final Config CONFIG = new Config();
   private static final AtomicBoolean TO_LOAD_CONFIG = new AtomicBoolean(true);

   private final Properties props;

   private Config() {
      props = new Properties();
   }

   /**
    * Get the singleton instance with loaded configuration.
    * @return configuration
    */
   public static Config get() {
      if (TO_LOAD_CONFIG.compareAndSet(true, false)) {
         CONFIG.load();
      }

      return CONFIG;
   }

   /**
    * Load configuration.
    * @throws ConfigNotInitializedException when configuration is not successfully initialized
    */
   private void load() {
      try (InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
         if (inputStream == null) {
            throw new ConfigNotInitializedException("Resource file [{0}] can not be found.", CONFIG_FILE);
         }

         props.load(inputStream);
      } catch (SecurityException ex) {
         throw new ConfigNotInitializedException(
               ex,
               "Configuration initialization failed due to a security issue.");
      } catch (IllegalArgumentException ex) {
         throw new ConfigNotInitializedException(
               ex,
               "Configuration initialization failed since" +
               " the configuration file contains a malformed Unicode escape sequence.");
      } catch (IOException ex) {
         throw new ConfigNotInitializedException(
               ex,
               "Configuration initialization failed due to an error when reading from the input stream.");
      }
   }

   /**
    * Get configuration value which is mapped to the specified property name {@code propName}.
    * @param propName property name to obtain its property value
    * @return configuration string value
    * @throws ConfigNotValidException when such property is not defined or property value is not valid
    */
   public String getPropertyNotEmpty(String propName) {
      String propValue = props.getProperty(propName);

      if (StringUtils.isEmpty(propValue)) {
         throw new ConfigNotValidException(
               "Configuration value with property name [{0}] should not be null or an empty string.",
               propName);
      }

      return propValue;
   }

   /**
    * Get configuration value which is mapped to the specified property name {@code propName}.
    * @param propName property name to obtain its property value
    * @return boolean
    * @throws ConfigNotValidException when such property is not defined or property value is not valid
    */
   public boolean getBoolean(String propName) {
      String propValue = getPropertyNotEmpty(propName);

      if ("true".equalsIgnoreCase(propValue)) {
         return true;
      }

      if ("false".equalsIgnoreCase(propValue)) {
         return false;
      }

      throw new ConfigNotValidException(
            "Configuration value [{0}] with property name [{1}] is not a boolean.",
            propValue,
            propName);
   }

   /**
    * Get configuration value which is mapped to the specified property name {@code propName}.
    * @param propName property name to obtain its property value
    * @return integer
    * @throws ConfigNotValidException when such property is not defined or property value is not valid
    */
   public int getInt(String propName) {
      String propertyValue = null;

      try {
         propertyValue = getPropertyNotEmpty(propName);

         return Integer.parseInt(propertyValue);
      } catch (NumberFormatException ex) {
         throw new ConfigNotValidException(
               "Configuration value [{0}] with property name [{1}] is not an integer.",
               propertyValue,
               propName);
      }
   }

   /**
    * Get configuration value which is mapped to the specified property name {@code propName}.
    * @param propName property name to obtain its property value
    * @return positive integer
    * @throws ConfigNotValidException when such property is not defined or property value is not valid
    */
   public int getPositiveInt(String propName) {
      int integer = getInt(propName);

      if (integer <= 0) {
         throw new ConfigNotValidException(
               "Configuration value [{0}] with property name [{1}] is not a positive integer.",
               integer,
               propName);
      }

      return integer;
   }

   /**
    * Get configuration value which is mapped to the specified property name {@code propName}.
    * @param propName property name to obtain its property value
    * @return positive integer or zero
    * @throws ConfigNotValidException when such property is not defined or property value is not valid
    */
   public int getPositiveOrZeroInt(String propName) {
      int integer = getInt(propName);

      if (integer < 0) {
         throw new ConfigNotValidException(
               "Configuration value [{0}] with property name [{1}] is not a positive integer or a zero.",
               integer,
               propName);
      }

      return integer;
   }

   /**
    * Get configuration value which is mapped to the specified property name {@code propName}.
    * @param propName property name to obtain its property value
    * @return long
    * @throws ConfigNotValidException when such property is not defined or property value is not valid
    */
   public long getLong(String propName) {
      String propertyValue = null;
      try {
         propertyValue = getPropertyNotEmpty(propName);

         return Long.parseLong(propertyValue);
      } catch (NumberFormatException ex) {
         throw new ConfigNotValidException(
               ex,
               "Configuration value [{0}] with property name [{1}] is not a valid long.",
               propertyValue,
               propName);
      }
   }

   /**
    * Add or update a configuration at runtime.
    * @param propName name of the property
    * @param propValue value of the property
    */
   public void cacheConfig(String propName, String propValue) {
      this.props.setProperty(propName, propValue);
   }
}
