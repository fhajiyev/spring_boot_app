package com.generac.ces.systemgateway.integrationtest;

import com.amazonaws.services.kinesis.model.CreateStreamRequest;
import com.amazonaws.services.kinesis.model.DeleteStreamRequest;
import com.amazonaws.services.kinesis.model.DescribeStreamRequest;
import com.amazonaws.services.kinesis.model.GetRecordsRequest;
import com.amazonaws.services.kinesis.model.GetRecordsResult;
import com.amazonaws.services.kinesis.model.GetShardIteratorRequest;
import com.amazonaws.services.kinesis.model.GetShardIteratorResult;
import com.amazonaws.services.kinesis.model.Shard;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import okhttp3.mockwebserver.MockWebServer;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

@ContextConfiguration(initializers = IntegrationTestBase.ContainerInitializer.class)
public class IntegrationTestBase {

    static boolean isConfigured;
    static MockWebServer mockBackEnd = new MockWebServer();

    static class ContainerInitializer
            implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        static MySQLContainer mySqlDbContainer =
                new MySQLContainer<>("mysql:8.2")
                        .withDatabaseName("systemgateway")
                        .withUsername("root")
                        .withPassword("password");

        static AwsTestContainer localstackContainer = AwsTestContainer.getInstance();

        static ClickhouseContainer clickhouseContainer = ClickhouseContainer.getInstance();

        private static void startContainers() throws Exception {
            mySqlDbContainer.start();
            localstackContainer.start();
            clickhouseContainer.start();
            mockBackEnd.start();
        }

        private static Map<String, String> createDBConnectionConfigDb() {
            return ImmutableMap.<String, String>builder()
                    .put("spring.datasource.mysql.url", mySqlDbContainer.getJdbcUrl())
                    .put("spring.datasource.mysql.username", mySqlDbContainer.getUsername())
                    .put("spring.datasource.mysql.password", mySqlDbContainer.getPassword())
                    .put(
                            "spring.datasource.clickhouse.driver-class-name",
                            "com.clickhouse.jdbc.ClickHouseDriver")
                    .put(
                            "spring.datasource.clickhouse.url",
                            "jdbc:ch://"
                                    + clickhouseContainer.getHost()
                                    + ":"
                                    + clickhouseContainer.getFirstMappedPort()
                                    + "/default")
                    .put("spring.datasource.clickhouse.username", clickhouseContainer.getUsername())
                    .put("spring.datasource.clickhouse.password", clickhouseContainer.getPassword())
                    .put(
                            "subscription-kinesis-sink.endpoint",
                            localstackContainer
                                    .getEndpointOverride(LocalStackContainer.Service.KINESIS)
                                    .toString())
                    .put("subscription-kinesis-sink.region", "us-east-1")
                    .put("subscription-kinesis-sink.stream-name", "test-sink-stream")
                    .put(
                            "web-client.systemUrl",
                            "http://"
                                    + mockBackEnd.getHostName()
                                    + ":"
                                    + mockBackEnd.getPort()
                                    + "/system/internal/v1")
                    .put(
                            "web-client.systemV2Url",
                            "http://"
                                    + mockBackEnd.getHostName()
                                    + ":"
                                    + mockBackEnd.getPort()
                                    + "/system-v2/internal/v1/systems")
                    .put(
                            "web-client.siteUrl",
                            "http://"
                                    + mockBackEnd.getHostName()
                                    + ":"
                                    + mockBackEnd.getPort()
                                    + "/sites/internal/v1/systems")
                    .put(
                            "web-client.odinUrl",
                            "http://"
                                    + mockBackEnd.getHostName()
                                    + ":"
                                    + mockBackEnd.getPort()
                                    + "/systemControl/v1")
                    .put("spring.flyway.enabled", "true")
                    .put("spring.flyway.url", mySqlDbContainer.getJdbcUrl())
                    .put("spring.flyway.user", mySqlDbContainer.getUsername())
                    .put("spring.flyway.password", mySqlDbContainer.getPassword())
                    .put("spring.flyway.locations", "classpath:db/migration/table")
                    .build();
        }

        @SneakyThrows
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            if (!isConfigured) {
                startContainers();
                isConfigured = true;
            }

            ConfigurableEnvironment environment = applicationContext.getEnvironment();
            MapPropertySource testContainer =
                    new MapPropertySource("testcontainers", (Map) createDBConnectionConfigDb());

            environment.getPropertySources().addFirst(testContainer);
        }

        public static void createStream() throws InterruptedException {
            CreateStreamRequest csr = new CreateStreamRequest();
            csr.setStreamName("test-sink-stream");
            csr.setShardCount(1);
            localstackContainer.getKinesisClient().createStream(csr);
            Thread.sleep(Duration.ofSeconds(2).toMillis());
        }

        public static void deleteStream() throws InterruptedException {
            DeleteStreamRequest dsr = new DeleteStreamRequest();
            dsr.setStreamName("test-sink-stream");
            localstackContainer.getKinesisClient().deleteStream(dsr);
            Thread.sleep(Duration.ofSeconds(2).toMillis());
        }

        public static List<ByteBuffer> readFromStream(String streamName) {
            List<ByteBuffer> results = new ArrayList<>();

            // Retrieve the Shards from a Stream

            DescribeStreamRequest describeStreamRequest = new DescribeStreamRequest();
            describeStreamRequest.setStreamName(streamName);

            String shardIterator;
            List<com.amazonaws.services.kinesis.model.Record> records;

            for (Shard shard :
                    localstackContainer
                            .getKinesisClient()
                            .describeStream(describeStreamRequest)
                            .getStreamDescription()
                            .getShards()) {
                GetShardIteratorRequest itReq = new GetShardIteratorRequest();
                itReq.setStreamName(streamName);
                itReq.setShardIteratorType("TRIM_HORIZON");
                itReq.setShardId(shard.getShardId());

                GetShardIteratorResult shardIteratorResult =
                        localstackContainer.getKinesisClient().getShardIterator(itReq);
                shardIterator = shardIteratorResult.getShardIterator();

                while (true) {
                    GetRecordsRequest recordsRequest = new GetRecordsRequest();
                    recordsRequest.setShardIterator(shardIterator);
                    recordsRequest.setLimit(10);
                    GetRecordsResult result =
                            localstackContainer.getKinesisClient().getRecords(recordsRequest);
                    records = result.getRecords();
                    if (records.size() <= 0) {
                        break;
                    }
                    records.forEach(
                            record -> {
                                results.add(record.getData().asReadOnlyBuffer());
                            });
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException exception) {
                        throw new RuntimeException(exception);
                    }
                    shardIterator = result.getNextShardIterator();
                }
            }
            return results;
        }
    }
}
