package com.generac.ces.systemgateway.service.kinesis;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;
import com.amazonaws.services.kinesis.model.*;
import com.generac.ces.SystemShadowCommandsProto;
import com.generac.ces.systemgateway.configuration.KinesisConfiguration;
import com.generac.ces.systemgateway.exception.*;
import java.nio.ByteBuffer;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class KinesisService {

    private String streamName;
    private AmazonKinesis kinesisClient;

    @Autowired
    public KinesisService(KinesisConfiguration kinesisConfiguration) {
        this.streamName = kinesisConfiguration.getStreamName();

        this.kinesisClient =
                AmazonKinesisClientBuilder.standard()
                        .withEndpointConfiguration(
                                new AwsClientBuilder.EndpointConfiguration(
                                        kinesisConfiguration.getEndpoint(),
                                        kinesisConfiguration.getRegion()))
                        .build();
    }

    public void upload(SystemShadowCommandsProto.SystemShadowCommandRequest proto) {
        try {
            PutRecordRequest putRecordRequest =
                    (new PutRecordRequest())
                            .withPartitionKey(proto.getSubscription().getSensorId());
            putRecordRequest.setStreamName(this.streamName);
            putRecordRequest.setData(ByteBuffer.wrap(proto.toByteArray()));
            kinesisClient.putRecord(putRecordRequest);
        } catch (Exception e) {
            throw new InternalServerException("Kinesis upload failed.");
        }
    }
}
