package dev.mars.generic.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.inject.Inject;
import java.io.InputStream;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import dev.mars.config.GenericApiConfig;
import dev.mars.common.exception.ConfigurationException;



/**
 * Loads YAML configuration files for queries and API endpoints
 */
@Singleton
public class ConfigurationLoader implements ConfigurationLoaderInterface {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationLoader.class);

    private final ObjectMapper yamlMapper;
    private final GenericApiConfig genericApiConfig;

    @Inject
    public ConfigurationLoader(GenericApiConfig genericApiConfig) {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.genericApiConfig = genericApiConfig;

        // Log the configuration directories and patterns that will be used
        logger.info("ConfigurationLoader initialized with directory scanning:");
        logger.info("  - Configuration directories: {}", genericApiConfig.getConfigDirectories());
        logger.info("  - Database patterns: {}", genericApiConfig.getDatabasePatterns());
        logger.info("  - Query patterns: {}", genericApiConfig.getQueryPatterns());
        logger.info("  - Endpoint patterns: {}", genericApiConfig.getEndpointPatterns());
    }

    /**
     * Resolve a configuration resource with fallback paths
     */
    private InputStream resolveConfigResource(String configPath) {
        logger.debug("Attempting to load resource: {}", configPath);

        // First, try to load as external file if path contains relative or absolute indicators
        if (configPath.startsWith("./") || configPath.startsWith("../") || configPath.startsWith("/") ||
            (configPath.length() > 1 && configPath.charAt(1) == ':')) {
            try {
                // Resolve the path relative to the current working directory
                Path filePath = Paths.get(configPath).toAbsolutePath().normalize();
                logger.debug("Trying to load external file: {}", filePath);

                if (Files.exists(filePath)) {
                    logger.info("Successfully loading external file: {}", filePath);
                    return new FileInputStream(filePath.toFile());
                } else {
                    logger.debug("External file not found: {}", filePath);
                }
            } catch (Exception e) {
                logger.debug("Failed to load external file: {}", configPath, e);
            }
        }

        // Try direct classpath path
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(configPath);
        if (inputStream != null) {
            logger.debug("Loaded from classpath: {}", configPath);
            return inputStream;
        }

        // Try with config/ prefix if not found
        if (!configPath.startsWith("config/")) {
            String altPath = "config/" + configPath;
            logger.debug("Resource not found, trying alternative classpath path: {}", altPath);
            inputStream = getClass().getClassLoader().getResourceAsStream(altPath);
            if (inputStream != null) {
                logger.debug("Loaded from classpath with config/ prefix: {}", altPath);
                return inputStream;
            }
        }

        // Try without config/ prefix if not found
        if (configPath.startsWith("config/")) {
            String altPath = configPath.substring(7);
            logger.debug("Resource not found, trying alternative classpath path: {}", altPath);
            inputStream = getClass().getClassLoader().getResourceAsStream(altPath);
            if (inputStream != null) {
                logger.debug("Loaded from classpath without config/ prefix: {}", altPath);
                return inputStream;
            }
        }

        return inputStream;
    }

    /**
     * Scan directories for configuration files matching the specified patterns
     */
    private java.util.List<Path> scanForConfigurationFiles(java.util.List<String> patterns) {
        java.util.List<Path> matchingFiles = new java.util.ArrayList<>();

        for (String directory : genericApiConfig.getConfigDirectories()) {
            logger.info("Scanning directory '{}' for configuration files with patterns: {}", directory, patterns);

            try {
                Path dirPath = Paths.get(directory).toAbsolutePath().normalize();

                if (!Files.exists(dirPath)) {
                    logger.warn("Configuration directory does not exist: {}", dirPath);
                    continue;
                }

                if (!Files.isDirectory(dirPath)) {
                    logger.warn("Configuration path is not a directory: {}", dirPath);
                    continue;
                }

                // Scan directory for files matching patterns
                try (java.util.stream.Stream<Path> files = Files.list(dirPath)) {
                    java.util.List<Path> directoryMatches = files
                        .filter(Files::isRegularFile)
                        .filter(file -> matchesAnyPattern(file.getFileName().toString(), patterns))
                        .sorted()
                        .collect(java.util.stream.Collectors.toList());

                    logger.info("Found {} matching files in directory '{}': {}",
                               directoryMatches.size(), directory,
                               directoryMatches.stream().map(p -> p.getFileName().toString()).collect(java.util.stream.Collectors.toList()));

                    matchingFiles.addAll(directoryMatches);
                }

            } catch (Exception e) {
                logger.error("Failed to scan directory '{}' for configuration files", directory, e);
            }
        }

        logger.info("Total configuration files found: {} files", matchingFiles.size());
        return matchingFiles;
    }

    /**
     * Check if a filename matches any of the specified patterns
     */
    private boolean matchesAnyPattern(String filename, java.util.List<String> patterns) {
        for (String pattern : patterns) {
            if (matchesPattern(filename, pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if a filename matches a specific pattern (supports * wildcard)
     */
    private boolean matchesPattern(String filename, String pattern) {
        // Convert glob pattern to regex
        String regex = pattern
            .replace(".", "\\.")
            .replace("*", ".*");

        return filename.matches(regex);
    }

    /**
     * Load query configurations from YAML files
     */
    public Map<String, QueryConfig> loadQueryConfigurations() {
        logger.info("Loading query configurations using directory scanning");

        java.util.List<Path> queryFiles = scanForConfigurationFiles(genericApiConfig.getQueryPatterns());

        if (queryFiles.isEmpty()) {
            logger.error("FATAL CONFIGURATION ERROR: No query configuration files found");
            logger.error("  Directories scanned: {}", genericApiConfig.getConfigDirectories());
            logger.error("  Patterns searched: {}", genericApiConfig.getQueryPatterns());
            logger.error("  Type: Query configurations");
            logger.error("  Impact: Application cannot start without query configurations");
            logger.error("  Action: Ensure query configuration files exist in the specified directories");
            logger.error("Application startup aborted due to missing configuration files");
            throw new dev.mars.common.exception.ConfigurationException(
                "No query configuration files found in directories: " +
                genericApiConfig.getConfigDirectories() + " with patterns: " + genericApiConfig.getQueryPatterns());
        }

        Map<String, QueryConfig> allQueries = new java.util.LinkedHashMap<>();

        for (Path queryFile : queryFiles) {
            logger.info("Loading query configurations from file: {}", queryFile);

            try (InputStream inputStream = new FileInputStream(queryFile.toFile())) {
                QueriesWrapper wrapper = yamlMapper.readValue(inputStream, QueriesWrapper.class);
                Map<String, QueryConfig> queries = wrapper.getQueries();

                if (queries == null || queries.isEmpty()) {
                    logger.warn("No query configurations found in file: {}", queryFile);
                    continue;
                }

                // Check for duplicate keys across files
                for (String queryName : queries.keySet()) {
                    if (allQueries.containsKey(queryName)) {
                        logger.error("FATAL CONFIGURATION ERROR: Duplicate query configuration found");
                        logger.error("  Query name: {}", queryName);
                        logger.error("  First defined in: (previous file)");
                        logger.error("  Duplicate found in: {}", queryFile);
                        logger.error("  Impact: Configuration conflicts prevent application startup");
                        logger.error("  Action: Ensure query names are unique across all configuration files");
                        logger.error("Application startup aborted due to configuration conflict");
                        throw new dev.mars.common.exception.ConfigurationException(
                            "Duplicate query configuration found: " + queryName + " in file: " + queryFile);
                    }
                }

                allQueries.putAll(queries);
                logger.info("Loaded {} query configurations from: {}", queries.size(), queryFile.getFileName());

                // Log each query for debugging
                queries.forEach((key, config) -> {
                    logger.info("  - Query '{}': {} (database: {})", key, config.getName(), config.getDatabase());
                });

            } catch (Exception e) {
                logger.error("FATAL CONFIGURATION ERROR: Failed to load query configurations");
                logger.error("  File: {}", queryFile);
                logger.error("  Type: Query configurations");
                logger.error("  Error: {}", e.getMessage());
                logger.error("  Impact: Application cannot start without valid query configurations");
                logger.error("  Action: Check file format, syntax, and accessibility");
                logger.error("Application startup aborted due to configuration loading failure", e);
                throw new dev.mars.common.exception.ConfigurationException(
                    "Failed to load query configurations from file: " + queryFile, e);
            }
        }

        if (allQueries.isEmpty()) {
            logger.error("FATAL CONFIGURATION ERROR: No query configurations found in any file");
            logger.error("  Files processed: {}", queryFiles.size());
            logger.error("  Type: Query configurations");
            logger.error("  Impact: Application cannot start without query configurations");
            logger.error("  Action: Verify files contain valid YAML with query definitions");
            logger.error("Application startup aborted due to empty configuration files");
            throw new dev.mars.common.exception.ConfigurationException(
                "No query configurations found in " + queryFiles.size() + " processed files");
        }

        logger.info("Successfully loaded {} total query configurations from {} files",
                   allQueries.size(), queryFiles.size());
        return allQueries;
    }
    
    /**
     * Load database configurations from YAML file
     */
    public Map<String, DatabaseConfig> loadDatabaseConfigurations() {
        logger.info("Loading database configurations using directory scanning");

        java.util.List<Path> databaseFiles = scanForConfigurationFiles(genericApiConfig.getDatabasePatterns());

        if (databaseFiles.isEmpty()) {
            logger.error("FATAL CONFIGURATION ERROR: No database configuration files found");
            logger.error("  Directories scanned: {}", genericApiConfig.getConfigDirectories());
            logger.error("  Patterns searched: {}", genericApiConfig.getDatabasePatterns());
            logger.error("  Type: Database configurations");
            logger.error("  Impact: Application cannot start without database configurations");
            logger.error("  Action: Ensure database configuration files exist in the specified directories");
            logger.error("Application startup aborted due to missing configuration files");
            throw new ConfigurationException("No database configuration files found in directories: " +
                genericApiConfig.getConfigDirectories() + " with patterns: " + genericApiConfig.getDatabasePatterns());
        }

        Map<String, DatabaseConfig> allDatabases = new java.util.LinkedHashMap<>();

        for (Path databaseFile : databaseFiles) {
            logger.info("Loading database configurations from file: {}", databaseFile);

            try (InputStream inputStream = new FileInputStream(databaseFile.toFile())) {
                DatabasesWrapper wrapper = yamlMapper.readValue(inputStream, DatabasesWrapper.class);
                Map<String, DatabaseConfig> databases = wrapper.getDatabases();

                if (databases == null || databases.isEmpty()) {
                    logger.warn("No database configurations found in file: {}", databaseFile);
                    continue;
                }

                // Check for duplicate keys across files
                for (String databaseName : databases.keySet()) {
                    if (allDatabases.containsKey(databaseName)) {
                        logger.error("FATAL CONFIGURATION ERROR: Duplicate database configuration found");
                        logger.error("  Database name: {}", databaseName);
                        logger.error("  First defined in: (previous file)");
                        logger.error("  Duplicate found in: {}", databaseFile);
                        logger.error("  Impact: Configuration conflicts prevent application startup");
                        logger.error("  Action: Ensure database names are unique across all configuration files");
                        logger.error("Application startup aborted due to configuration conflict");
                        throw new dev.mars.common.exception.ConfigurationException(
                            "Duplicate database configuration found: " + databaseName + " in file: " + databaseFile);
                    }
                }

                allDatabases.putAll(databases);
                logger.info("Loaded {} database configurations from: {}", databases.size(), databaseFile.getFileName());

                // Log each database for debugging
                databases.forEach((key, config) -> {
                    logger.info("  - Database '{}': {} (url: {})", key, config.getName(), config.getUrl());
                });

            } catch (Exception e) {
                logger.error("FATAL CONFIGURATION ERROR: Failed to load database configurations");
                logger.error("  File: {}", databaseFile);
                logger.error("  Type: Database configurations");
                logger.error("  Error: {}", e.getMessage());
                logger.error("  Impact: Application cannot start without valid database configurations");
                logger.error("  Action: Check file format, syntax, and accessibility");
                logger.error("Application startup aborted due to configuration loading failure", e);
                throw new ConfigurationException("Failed to load database configurations from file: " + databaseFile, e);
            }
        }

        if (allDatabases.isEmpty()) {
            logger.error("FATAL CONFIGURATION ERROR: No database configurations found in any file");
            logger.error("  Files processed: {}", databaseFiles.size());
            logger.error("  Type: Database configurations");
            logger.error("  Impact: Application cannot start without database configurations");
            logger.error("  Action: Verify files contain valid YAML with database definitions");
            logger.error("Application startup aborted due to empty configuration files");
            throw new ConfigurationException("No database configurations found in " + databaseFiles.size() + " processed files");
        }

        logger.info("Successfully loaded {} total database configurations from {} files",
                   allDatabases.size(), databaseFiles.size());
        return allDatabases;
    }

    /**
     * Load API endpoint configurations from YAML file
     */
    public Map<String, ApiEndpointConfig> loadEndpointConfigurations() {
        logger.info("Loading endpoint configurations using directory scanning");

        java.util.List<Path> endpointFiles = scanForConfigurationFiles(genericApiConfig.getEndpointPatterns());

        if (endpointFiles.isEmpty()) {
            logger.error("FATAL CONFIGURATION ERROR: No endpoint configuration files found");
            logger.error("  Directories scanned: {}", genericApiConfig.getConfigDirectories());
            logger.error("  Patterns searched: {}", genericApiConfig.getEndpointPatterns());
            logger.error("  Type: Endpoint configurations");
            logger.error("  Impact: Application cannot start without endpoint configurations");
            logger.error("  Action: Ensure endpoint configuration files exist in the specified directories");
            logger.error("Application startup aborted due to missing configuration files");
            throw new dev.mars.common.exception.ConfigurationException(
                "No endpoint configuration files found in directories: " +
                genericApiConfig.getConfigDirectories() + " with patterns: " + genericApiConfig.getEndpointPatterns());
        }

        Map<String, ApiEndpointConfig> allEndpoints = new java.util.LinkedHashMap<>();

        for (Path endpointFile : endpointFiles) {
            logger.info("Loading endpoint configurations from file: {}", endpointFile);

            try (InputStream inputStream = new FileInputStream(endpointFile.toFile())) {
                EndpointsWrapper wrapper = yamlMapper.readValue(inputStream, EndpointsWrapper.class);
                Map<String, ApiEndpointConfig> endpoints = wrapper.getEndpoints();

                if (endpoints == null || endpoints.isEmpty()) {
                    logger.warn("No endpoint configurations found in file: {}", endpointFile);
                    continue;
                }

                // Check for duplicate keys across files
                for (String endpointName : endpoints.keySet()) {
                    if (allEndpoints.containsKey(endpointName)) {
                        logger.error("FATAL CONFIGURATION ERROR: Duplicate endpoint configuration found");
                        logger.error("  Endpoint name: {}", endpointName);
                        logger.error("  First defined in: (previous file)");
                        logger.error("  Duplicate found in: {}", endpointFile);
                        logger.error("  Impact: Configuration conflicts prevent application startup");
                        logger.error("  Action: Ensure endpoint names are unique across all configuration files");
                        logger.error("Application startup aborted due to configuration conflict");
                        throw new dev.mars.common.exception.ConfigurationException(
                            "Duplicate endpoint configuration found: " + endpointName + " in file: " + endpointFile);
                    }
                }

                allEndpoints.putAll(endpoints);
                logger.info("Loaded {} endpoint configurations from: {}", endpoints.size(), endpointFile.getFileName());

                // Log each endpoint for debugging
                endpoints.forEach((key, config) -> {
                    logger.info("  - Endpoint '{}': {} {} (query: {})", key, config.getMethod(), config.getPath(), config.getQuery());
                });

            } catch (Exception e) {
                logger.error("FATAL CONFIGURATION ERROR: Failed to load endpoint configurations");
                logger.error("  File: {}", endpointFile);
                logger.error("  Type: Endpoint configurations");
                logger.error("  Error: {}", e.getMessage());
                logger.error("  Impact: Application cannot start without valid endpoint configurations");
                logger.error("  Action: Check file format, syntax, and accessibility");
                logger.error("Application startup aborted due to configuration loading failure", e);
                throw new ConfigurationException("Failed to load endpoint configurations from file: " + endpointFile, e);
            }
        }

        if (allEndpoints.isEmpty()) {
            logger.error("FATAL CONFIGURATION ERROR: No endpoint configurations found in any file");
            logger.error("  Files processed: {}", endpointFiles.size());
            logger.error("  Type: Endpoint configurations");
            logger.error("  Impact: Application cannot start without endpoint configurations");
            logger.error("  Action: Verify files contain valid YAML with endpoint definitions");
            logger.error("Application startup aborted due to empty configuration files");
            throw new ConfigurationException("No endpoint configurations found in " + endpointFiles.size() + " processed files");
        }

        logger.info("Successfully loaded {} total endpoint configurations from {} files",
                   allEndpoints.size(), endpointFiles.size());
        return allEndpoints;
    }
    
    /**
     * Wrapper class for queries YAML structure
     */
    public static class QueriesWrapper {
        private Map<String, QueryConfig> queries;
        
        public Map<String, QueryConfig> getQueries() {
            return queries;
        }
        
        public void setQueries(Map<String, QueryConfig> queries) {
            this.queries = queries;
        }
    }
    
    /**
     * Wrapper class for endpoints YAML structure
     */
    public static class EndpointsWrapper {
        private Map<String, ApiEndpointConfig> endpoints;

        public Map<String, ApiEndpointConfig> getEndpoints() {
            return endpoints;
        }

        public void setEndpoints(Map<String, ApiEndpointConfig> endpoints) {
            this.endpoints = endpoints;
        }
    }

    /**
     * Wrapper class for databases YAML structure
     */
    public static class DatabasesWrapper {
        private Map<String, DatabaseConfig> databases;

        public Map<String, DatabaseConfig> getDatabases() {
            return databases;
        }

        public void setDatabases(Map<String, DatabaseConfig> databases) {
            this.databases = databases;
        }
    }
}
