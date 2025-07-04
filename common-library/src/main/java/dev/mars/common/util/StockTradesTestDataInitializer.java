package dev.mars.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Utility class for initializing stock trades test data
 * Moved from generic-api-service to common-library for reusability
 */
public class StockTradesTestDataInitializer {
    private static final Logger logger = LoggerFactory.getLogger(StockTradesTestDataInitializer.class);

    /**
     * Initialize stock trades data for testing using SQL directly
     */
    public static void initializeStockTradesForTesting(Connection connection) throws Exception {
        logger.info("Initializing stock trades data for testing");

        try (Statement statement = connection.createStatement()) {

            // Create stock_trades table if it doesn't exist
            String createTableSql = """
                CREATE TABLE IF NOT EXISTS stock_trades (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    symbol VARCHAR(10) NOT NULL,
                    trade_type VARCHAR(4) NOT NULL CHECK (trade_type IN ('BUY', 'SELL')),
                    quantity INTEGER NOT NULL CHECK (quantity > 0),
                    price DECIMAL(10,2) NOT NULL CHECK (price > 0),
                    total_value DECIMAL(15,2) NOT NULL,
                    trade_date_time TIMESTAMP NOT NULL,
                    trader_id VARCHAR(50) NOT NULL,
                    exchange VARCHAR(20) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
                """;

            statement.execute(createTableSql);
            logger.info("Stock trades table created/verified");

            // Check if data already exists
            try (ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM stock_trades")) {
                if (rs.next() && rs.getInt(1) > 0) {
                    logger.info("Stock trades data already exists, skipping sample data loading");
                    return;
                }
            }

            // Insert sample data
            String insertSql = """
                INSERT INTO stock_trades (symbol, trade_type, quantity, price, total_value,
                                        trade_date_time, trader_id, exchange)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

            try (PreparedStatement preparedStatement = connection.prepareStatement(insertSql)) {
                // Sample data
                Object[][] sampleData = {
                    {"AAPL", "BUY", 100, 150.50, 15050.00, "2024-01-15 10:30:00", "trader001", "NASDAQ"},
                    {"GOOGL", "SELL", 50, 2800.75, 140037.50, "2024-01-15 11:15:00", "trader002", "NASDAQ"},
                    {"MSFT", "BUY", 200, 380.25, 76050.00, "2024-01-15 14:20:00", "trader001", "NASDAQ"},
                    {"TSLA", "BUY", 75, 220.80, 16560.00, "2024-01-16 09:45:00", "trader003", "NASDAQ"},
                    {"AMZN", "SELL", 25, 3200.00, 80000.00, "2024-01-16 13:30:00", "trader002", "NASDAQ"}
                };

                for (Object[] row : sampleData) {
                    preparedStatement.setString(1, (String) row[0]);
                    preparedStatement.setString(2, (String) row[1]);
                    preparedStatement.setInt(3, (Integer) row[2]);
                    preparedStatement.setBigDecimal(4, new java.math.BigDecimal(row[3].toString()));
                    preparedStatement.setBigDecimal(5, new java.math.BigDecimal(row[4].toString()));
                    preparedStatement.setTimestamp(6, java.sql.Timestamp.valueOf((String) row[5]));
                    preparedStatement.setString(7, (String) row[6]);
                    preparedStatement.setString(8, (String) row[7]);
                    preparedStatement.executeUpdate();
                }

                logger.info("Loaded {} sample stock trades", sampleData.length);
            }
        }
    }
}
