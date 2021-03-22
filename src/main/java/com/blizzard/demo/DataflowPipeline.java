package com.blizzard.demo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;


public class DataflowPipeline {
	private static final Logger log = LoggerFactory.getLogger(DataflowPipeline.class);

	public static void main(String[] args) {
		//Create our custom pipeline options
		DataflowPipelineOption options = PipelineOptionsFactory.fromArgs(args)
				.withValidation()
				.as(DataflowPipelineOption.class);

		final String READ_TOPIC = "projects/blz-d-gdp-telemetry/topics/"
				+ options.getInputTopic();
		final String PUBLISH_TOPIC = "projects/blz-d-gdp-telemetry/topics/"
				+ options.getOutputTopic();

		final String BUILD_NUMBER = options.getBuildNumber();

		log.info(String.format("Creating the pipeline. The build number is %s", BUILD_NUMBER));

		Pipeline pipeline = Pipeline.create(options);

		//Receiving the pub/sub message from READ_TOPIC
		PCollection<PubsubMessage> pubsubMessagePCollection = pipeline.apply("Read Pubsub Messages", PubsubIO.
				readMessagesWithAttributes().fromTopic(READ_TOPIC));

		//Do transform on pubsub messages and then publish into next topic
		pubsubMessagePCollection.apply("Transforms/Filters/Other operations", ParDo.of(new MessagesTransform()))
				.apply("Publish Pubsub Messages to output topic", PubsubIO.writeMessages().to(PUBLISH_TOPIC))
				.apply("HTTP query to other endpoints", new HttpQueryImp());

		//Execute the pipeline now
		pipeline.run();
	}
}
