-- =========================================================
-- StreamCell Flink SQL
-- orders topic -> 1 minute product summary -> PostgreSQL
-- =========================================================

-- ---------------------------------------------------------
-- 1. Kafka Source Table
-- ---------------------------------------------------------
-- Kafka topic: orders
-- Event format:
-- {
--   "order_id": "ord-001",
--   "user_id": "user-001",
--   "product_id": "product-001",
--   "payment_amount": 12000.0,
--   "event_time": "2026-06-05 22:30:10"
-- }
-- ---------------------------------------------------------

CREATE TABLE orders_source (
                               order_id STRING,
                               user_id STRING,
                               product_id STRING,
                               payment_amount DECIMAL(18, 2),
                               event_time TIMESTAMP(3),
                               WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND
) WITH (
      'connector' = 'kafka',
      'topic' = 'orders',
      'properties.bootstrap.servers' = 'kafka-1:9092,kafka-2:9092,kafka-3:9092',
      'properties.group.id' = 'streamcell-order-summary',
      'scan.startup.mode' = 'latest-offset',
      'format' = 'json',
      'json.ignore-parse-errors' = 'true',
      'json.timestamp-format.standard' = 'SQL'
      );

-- ---------------------------------------------------------
-- 2. PostgreSQL JDBC Sink Table
-- ---------------------------------------------------------
-- Physical table:
-- platform.order_summary_1min
-- ---------------------------------------------------------

CREATE TABLE order_summary_1min_sink (
                                         window_start TIMESTAMP(3),
                                         window_end TIMESTAMP(3),
                                         product_id STRING,
                                         order_count BIGINT,
                                         total_payment_amount DECIMAL(18, 2)
) WITH (
      'connector' = 'jdbc',
      'url' = 'jdbc:postgresql://postgres:5432/streamcell',
      'table-name' = 'platform.order_summary_1min',
      'username' = 'streamcell',
      'password' = 'streamcell'
      );

-- ---------------------------------------------------------
-- 3. Insert Aggregation Query
-- ---------------------------------------------------------
-- 1 minute tumbling window
-- group by product_id
-- ---------------------------------------------------------

INSERT INTO order_summary_1min_sink
SELECT
    window_start,
    window_end,
    product_id,
    COUNT(*) AS order_count,
    SUM(payment_amount) AS total_payment_amount
FROM TABLE(
        TUMBLE(
            TABLE orders_source,
            DESCRIPTOR(event_time),
                INTERVAL '1' MINUTE
        )
     )
GROUP BY
    window_start,
    window_end,
    product_id;