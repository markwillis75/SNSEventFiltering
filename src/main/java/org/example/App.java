package org.example;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.HashMap;

/**
 * Publish 2 SNS Messages, both of which have the the String attribute Trace=true
 * On the 2nd message, include an additional binary attribute.
 * Both messages should be published to the subscribed queue, which has an Event Filter of
 * <code>
 *     {
 *       "Trace": [ "true" ]
 *     }
 * </code>
 *
 * The message containing the additional binary attribute is not published to the queue
 *
 * This appears at odds with documentation which states "Amazon SNS ignores message attributes
 * with the Binary data type"
 *
 * See https://docs.aws.amazon.com/sns/latest/dg/sns-subscription-filter-policies.html
 */
public class App
{
    private String TOPIC_ARN = "arn:aws:sns:us-east-1:%s:Topic-1";
    private String QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/%s/Queue-1";

    SnsClient snsClient;
    SqsClient sqsClient;

    private App(String awsAccountId){
        TOPIC_ARN = String.format(TOPIC_ARN, awsAccountId);
        QUEUE_URL = String.format(QUEUE_URL, awsAccountId);

        snsClient =
                SnsClient.builder().credentialsProvider(DefaultCredentialsProvider.builder().build()).build();

        sqsClient =
                SqsClient.builder().credentialsProvider((DefaultCredentialsProvider.builder().build())).build();
    }

    private void execute(){
        clean();

        publish ("msg1", true);  // include a binary attribute
        publish ("msg2", false); // omit the binary attribute

        try {
            Thread.sleep(5000);  // let's give it 5 seconds
        }
        catch(InterruptedException e){
            // nothing to see here
        }

        read();
    }

    private void clean(){
        PurgeQueueRequest purgeQueueRequest =
                PurgeQueueRequest
                .builder()
                .queueUrl(QUEUE_URL)
                .build();

        sqsClient.purgeQueue(purgeQueueRequest);
    }

    private void publish(String msg, boolean withBinaryAttribute){
        PublishRequest publishRequest
                = PublishRequest
                .builder()
                .message(msg)
                .messageAttributes(getAttributes(withBinaryAttribute))
                .topicArn(TOPIC_ARN)
                .build();

        PublishResponse response = snsClient.publish(publishRequest);
        System.out.printf("%s published successfully %b%n",
                msg,
                response.sdkHttpResponse().isSuccessful());
    }

    private HashMap<String, MessageAttributeValue> getAttributes(boolean withBinaryAttribute){
        HashMap<String, MessageAttributeValue> attributes = new HashMap<>();

        attributes.put(
                "Trace",
                MessageAttributeValue
                        .builder()
                        .dataType("String")
                        .stringValue(String.valueOf(true))
                        .build());

        if (withBinaryAttribute){
            attributes.put(
                    "FlyInTheOintment",
                    MessageAttributeValue
                            .builder()
                            .dataType("Binary")
                            .binaryValue(SdkBytes.fromByteArray("Oh Dear".getBytes()))
                            .build());
        }

        return attributes;
    }

    private void read(){
        ReceiveMessageRequest request
                = ReceiveMessageRequest
                .builder()
                .queueUrl(QUEUE_URL)
                .maxNumberOfMessages(10)
                .build();

        ReceiveMessageResponse response
                = sqsClient.receiveMessage(request);

        System.out.printf("Read %d message%n", response.messages().size());
        System.out.println(response.messages());
    }

    public static void main( String[] args )
    {
        if (args.length < 1){
            System.out.println("Usage\n java App aws-account-id");
            System.exit(0);
        }

        new App(args[0]).execute();
    }
}
