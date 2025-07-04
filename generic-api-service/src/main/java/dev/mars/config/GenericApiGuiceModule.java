package dev.mars.config;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import dev.mars.generic.GenericApiController;
import dev.mars.generic.GenericApiService;
import dev.mars.generic.GenericRepository;
import dev.mars.generic.config.ConfigurationLoader;
import dev.mars.generic.config.ConfigurationLoaderFactory;
import dev.mars.generic.config.EndpointConfigurationManager;
import dev.mars.generic.database.DatabaseConnectionManager;
import dev.mars.database.loader.DatabaseConfigurationLoader;
import dev.mars.database.repository.DatabaseConfigurationRepository;
import dev.mars.database.repository.QueryConfigurationRepository;
import dev.mars.database.repository.EndpointConfigurationRepository;
import dev.mars.generic.management.ConfigurationMetadataService;
import dev.mars.generic.management.ConfigurationManagementService;
import dev.mars.generic.management.ConfigurationManagementController;
import dev.mars.generic.management.UsageStatisticsService;
import dev.mars.generic.management.HealthMonitoringService;
import dev.mars.generic.management.ManagementController;
import dev.mars.generic.migration.ConfigurationMigrationService;
import dev.mars.generic.migration.ConfigurationMigrationController;
import dev.mars.database.DatabaseManager;
import dev.mars.database.ConfigurationDataLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Guice dependency injection module for Generic API Service
 */
public class GenericApiGuiceModule extends AbstractModule {
    private static final Logger logger = LoggerFactory.getLogger(GenericApiGuiceModule.class);
    
    @Override
    protected void configure() {
        logger.info("Configuring Generic API Guice dependency injection");
        logger.info("Generic API Guice module configured successfully");
    }
    
    @Provides
    @Singleton
    public GenericApiConfig provideGenericApiConfig() {
        logger.info("Creating GenericApiConfig instance");
        return GenericApiConfig.loadFromFile();
    }
    



    @Provides
    @Singleton
    public SwaggerConfig provideSwaggerConfig(GenericApiConfig genericApiConfig) {
        logger.info("Creating SwaggerConfig instance");
        return new SwaggerConfig(genericApiConfig);
    }

    @Provides
    @Singleton
    public ConfigurationLoader provideConfigurationLoader(GenericApiConfig genericApiConfig) {
        logger.info("Creating ConfigurationLoader instance");
        return new ConfigurationLoader(genericApiConfig);
    }

    @Provides
    @Singleton
    public DatabaseConfigurationRepository provideDatabaseConfigurationRepository(DatabaseManager databaseManager) {
        logger.info("Creating DatabaseConfigurationRepository instance");
        return new DatabaseConfigurationRepository(databaseManager);
    }

    @Provides
    @Singleton
    public QueryConfigurationRepository provideQueryConfigurationRepository(DatabaseManager databaseManager) {
        logger.info("Creating QueryConfigurationRepository instance");
        return new QueryConfigurationRepository(databaseManager);
    }

    @Provides
    @Singleton
    public EndpointConfigurationRepository provideEndpointConfigurationRepository(DatabaseManager databaseManager) {
        logger.info("Creating EndpointConfigurationRepository instance");
        return new EndpointConfigurationRepository(databaseManager);
    }

    @Provides
    @Singleton
    public DatabaseConfigurationLoader provideDatabaseConfigurationLoader(DatabaseConfigurationRepository databaseRepository,
                                                                        QueryConfigurationRepository queryRepository,
                                                                        EndpointConfigurationRepository endpointRepository) {
        logger.info("Creating DatabaseConfigurationLoader instance");
        return new DatabaseConfigurationLoader(databaseRepository, queryRepository, endpointRepository);
    }

    @Provides
    @Singleton
    public ConfigurationLoaderFactory provideConfigurationLoaderFactory(GenericApiConfig genericApiConfig,
                                                                       ConfigurationLoader yamlConfigurationLoader,
                                                                       DatabaseConfigurationLoader databaseConfigurationLoader) {
        logger.info("Creating ConfigurationLoaderFactory instance");
        return new ConfigurationLoaderFactory(genericApiConfig, yamlConfigurationLoader, databaseConfigurationLoader);
    }

    @Provides
    @Singleton
    public EndpointConfigurationManager provideEndpointConfigurationManager(ConfigurationLoaderFactory configurationLoaderFactory) {
        logger.info("Creating EndpointConfigurationManager instance");
        EndpointConfigurationManager manager = new EndpointConfigurationManager(configurationLoaderFactory);

        // Validate configurations on startup
        manager.validateConfigurations();

        return manager;
    }

    @Provides
    @Singleton
    public DatabaseConnectionManager provideDatabaseConnectionManager(EndpointConfigurationManager configurationManager) {
        logger.info("Creating DatabaseConnectionManager instance");
        return new DatabaseConnectionManager(configurationManager);
    }



    @Provides
    @Singleton
    public GenericRepository provideGenericRepository(DatabaseConnectionManager databaseConnectionManager) {
        logger.info("Creating GenericRepository instance");
        return new GenericRepository(databaseConnectionManager);
    }

    @Provides
    @Singleton
    public GenericApiService provideGenericApiService(GenericRepository genericRepository,
                                                     EndpointConfigurationManager configurationManager) {
        logger.info("Creating GenericApiService instance");
        return new GenericApiService(genericRepository, configurationManager);
    }

    @Provides
    @Singleton
    public GenericApiController provideGenericApiController(GenericApiService genericApiService,
                                                           UsageStatisticsService statisticsService) {
        logger.info("Creating GenericApiController instance");
        return new GenericApiController(genericApiService, statisticsService);
    }

    @Provides
    @Singleton
    public ConfigurationMetadataService provideConfigurationMetadataService(GenericApiConfig genericApiConfig) {
        logger.info("Creating ConfigurationMetadataService instance");
        return new ConfigurationMetadataService(genericApiConfig);
    }

    @Provides
    @Singleton
    public UsageStatisticsService provideUsageStatisticsService() {
        logger.info("Creating UsageStatisticsService instance");
        return new UsageStatisticsService();
    }

    @Provides
    @Singleton
    public HealthMonitoringService provideHealthMonitoringService(DatabaseConnectionManager databaseConnectionManager,
                                                                EndpointConfigurationManager configurationManager) {
        logger.info("Creating HealthMonitoringService instance");
        return new HealthMonitoringService(databaseConnectionManager, configurationManager);
    }

    @Provides
    @Singleton
    public ConfigurationManagementService provideConfigurationManagementService(DatabaseConfigurationRepository databaseRepository,
                                                                               QueryConfigurationRepository queryRepository,
                                                                               EndpointConfigurationRepository endpointRepository,
                                                                               ConfigurationLoaderFactory configurationLoaderFactory,
                                                                               EndpointConfigurationManager configurationManager) {
        logger.info("Creating ConfigurationManagementService instance");
        return new ConfigurationManagementService(databaseRepository, queryRepository, endpointRepository,
                                                 configurationLoaderFactory, configurationManager);
    }

    @Provides
    @Singleton
    public ConfigurationManagementController provideConfigurationManagementController(ConfigurationManagementService configurationManagementService) {
        logger.info("Creating ConfigurationManagementController instance");
        return new ConfigurationManagementController(configurationManagementService);
    }

    @Provides
    @Singleton
    public ConfigurationMigrationService provideConfigurationMigrationService(DatabaseConfigurationRepository databaseRepository,
                                                                             QueryConfigurationRepository queryRepository,
                                                                             EndpointConfigurationRepository endpointRepository,
                                                                             ConfigurationLoader yamlLoader,
                                                                             DatabaseConfigurationLoader databaseLoader,
                                                                             ConfigurationLoaderFactory configurationLoaderFactory) {
        logger.info("Creating ConfigurationMigrationService instance");
        return new ConfigurationMigrationService(databaseRepository, queryRepository, endpointRepository,
                                                yamlLoader, databaseLoader, configurationLoaderFactory);
    }

    @Provides
    @Singleton
    public ConfigurationMigrationController provideConfigurationMigrationController(ConfigurationMigrationService configurationMigrationService) {
        logger.info("Creating ConfigurationMigrationController instance");
        return new ConfigurationMigrationController(configurationMigrationService);
    }

    @Provides
    @Singleton
    public ManagementController provideManagementController(ConfigurationMetadataService metadataService,
                                                          UsageStatisticsService statisticsService,
                                                          HealthMonitoringService healthService,
                                                          GenericApiService genericApiService,
                                                          EndpointConfigurationManager configurationManager) {
        logger.info("Creating ManagementController instance");
        return new ManagementController(metadataService, statisticsService, healthService,
                                      genericApiService, configurationManager);
    }

    @Provides
    @Singleton
    public DatabaseManager provideDatabaseManager(GenericApiConfig genericApiConfig) {
        logger.info("Creating DatabaseManager instance");
        DatabaseManager databaseManager = new DatabaseManager(genericApiConfig);

        // Initialize schema on startup
        logger.info("Initializing database schema");
        databaseManager.initializeSchema();

        return databaseManager;
    }

    @Provides
    @Singleton
    public ConfigurationDataLoader provideConfigurationDataLoader(DatabaseManager databaseManager,
                                                                GenericApiConfig genericApiConfig,
                                                                dev.mars.generic.config.ConfigurationLoader configurationLoader) {
        logger.info("Creating ConfigurationDataLoader instance");
        ConfigurationDataLoader dataLoader = new ConfigurationDataLoader(databaseManager, genericApiConfig, configurationLoader);

        // Load configuration data from YAML if needed
        logger.info("Loading configuration data from YAML if needed");
        dataLoader.loadConfigurationDataIfNeeded();

        return dataLoader;
    }
}
