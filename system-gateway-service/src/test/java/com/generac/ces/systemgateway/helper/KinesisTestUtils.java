package com.generac.ces.systemgateway.helper;

import com.amazonaws.SDKGlobalConfiguration;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;
import com.amazonaws.services.kinesis.model.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class KinesisTestUtils {
    public static AmazonKinesis getClient(String endpoint, String region) {
        System.setProperty(SDKGlobalConfiguration.AWS_CBOR_DISABLE_SYSTEM_PROPERTY, "true");
        System.setProperty(SDKGlobalConfiguration.DISABLE_CERT_CHECKING_SYSTEM_PROPERTY, "true");

        AmazonKinesisClientBuilder clientBuilder = AmazonKinesisClientBuilder.standard();
        clientBuilder.setEndpointConfiguration(
                new AwsClientBuilder.EndpointConfiguration(endpoint, region));

        return clientBuilder.build();
    }

    public static List<ByteBuffer> readFromStream(AmazonKinesis kinesisClient, String streamName) {
        List<ByteBuffer> results = new ArrayList<>();

        for (Shard shard :
                kinesisClient.describeStream(streamName).getStreamDescription().getShards()) {
            String shardIterator =
                    kinesisClient
                            .getShardIterator(
                                    new GetShardIteratorRequest()
                                            .withStreamName(streamName)
                                            .withShardId(shard.getShardId())
                                            .withShardIteratorType(ShardIteratorType.TRIM_HORIZON))
                            .getShardIterator();

            GetRecordsRequest getRecordsRequest = new GetRecordsRequest();
            getRecordsRequest.setShardIterator(shardIterator);

            GetRecordsResult recordResult = kinesisClient.getRecords(getRecordsRequest);

            recordResult
                    .getRecords()
                    .forEach(
                            record -> {
                                results.add(record.getData());
                            });
        }

        return results;
    }
}
