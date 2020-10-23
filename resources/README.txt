Problem statement
=================

This is a small app to illustrate an apparent issue with subscribing an SQS Queue to an SNS Topic.

The use case is simple; we have an SNS Topic to which we will subscribe an SQS Queue.  
We wish that SQS Queue to receive a subset of messages sent to the SNS topic - namely those which have a String attribute with name of "Trace" and value of "true" present.

The app publishes 2 messages, each of which have the Trace:true attribute.  One of the messages has an additional attribute of type Binary.

Only the message which omits the Binary attribute is sent to SQS from SNS.

According to the AWS documentation  "Amazon SNS ignores message attributes with the Binary data type" - see https://docs.aws.amazon.com/sns/latest/dg/sns-subscription-filter-policies.html

Prerequisites
=============

Create the SNS Topic, SQS Queue and subscription via cloudformation.  

DISCLAIMER: The access policy on the Queue permits sqs:SendMessage and sqs:ReceiveMessage to AWS:*

  aws cloudformation deploy --template-file path\to\sqs-and-sns.cft --stack-name sqs-sns


Sample App
==========
Run the app, passing the AWS account ID in which the SNS/SQS resources were created
The app will publish 2 messages to the topic and query the SQS queue, retrieving a single message