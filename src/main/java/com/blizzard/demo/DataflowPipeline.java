package com.blizzard.demo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;

import java.util.HashMap;
import java.util.Map;

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

		//Do transform on pubsub messages
		pubsubMessagePCollection.apply("Transforms/Filters/Other operations", ParDo.of(new MessagesTransform()));

		//Publishing transformed message into next topic
		pubsubMessagePCollection.apply("Publish Pubsub Messages to output topic", PubsubIO.writeMessages().to(PUBLISH_TOPIC));

//		//Http query on other endpoints
//		try {
//			HttpQueryImp httpQueryImp = new HttpQueryImp();
//			httpQueryImp.simpleRequest();
//		} catch (Exception e) {
//			log.info("error!");
//			e.printStackTrace();
//		}


		//Execute the pipeline now
		pipeline.run();
	}
}

class MessagesTransform extends DoFn<PubsubMessage, PubsubMessage>{
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger LOG = LoggerFactory.getLogger(MessagesTransform.class);

	@ProcessElement
	public void processElement(ProcessContext context) {
		LOG.info(String.format("Received message %s", new String(context.element().getPayload())));
		PubsubMessage msg = context.element();
		Map<String, String> attributes = new HashMap<>();
		attributes.put("routeID", "dummy");
		attributes.put("traceRoute", "fake");
		attributes.put("messageName", "test1");
		PubsubMessage newMsg = new PubsubMessage(msg.getPayload(), attributes);
		LOG.info(String.format("New message attribute routeID %s", newMsg.getAttribute("routeID")));
		context.output(newMsg);
	}
}
