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
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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

		//Http query on other endpoints
		try {
			HttpQueryImp httpQueryImp = new HttpQueryImp();
			httpQueryImp.simpleRequest();
		} catch (Exception e) {
			log.info("error!");
			e.printStackTrace();
		}


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
	public void process(ProcessContext context) {
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

class HttpQueryImp {
	//targeted url jwt token
	private static String token;

	//targeted url parameters
	@Value("${target.region}")
	private static final String REGION = "us-west2";
	@Value("${target.project.id}")
	private static final String PROJECT_ID = "blz-d-gdp-telemetry";
	@Value("${target.received.function.name}")
	private static final String RECEIVING_FUNCTION_NAME = "TicSubscriptionTest";

	//create the target url (direct target url w/o passing through gateway)
	private static final String receivingFunctionUrl = String.format(
			"https://%s-%s.cloudfunctions.net/%s", REGION, PROJECT_ID, RECEIVING_FUNCTION_NAME);
	//gcp metatdata service endpoint
	@Value("${gcp.meta.endpoint}")
	private static final String metadataTokenEndpoint =
			"http://metadata/computeMetadata/v1/instance/service-accounts/default/identity?audience=";
	private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(HttpQueryImp.class.getName());
	private static final HttpClient client = HttpClient.newBuilder().build();

	//sending a simple http request to endpoint url
	//previous attribute solution method:
	//public static void simpleRequest(String token) throws IOException, InterruptedException {

	public static void simpleRequest() throws IOException, InterruptedException {
		logger.info("Getting target url token at first!");

		getToken();

		logger.info("Token is: ");
		logger.info(token);
		logger.info("Started sending http request!");

		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(receivingFunctionUrl))
				.setHeader("Authorization", "Bearer " + token)
				.build();

		HttpResponse<String> response =
				client.send(request, HttpResponse.BodyHandlers.ofString());

		logger.info(response.body());
	}

	//retrieve jwt token from target url
	private static void getToken() {
		// set up metadata server request, reference: https://cloud.google.com/compute/docs/instances/verifying-instance-identity#request_signature
		java.net.http.HttpRequest tokenRequest = java.net.http.HttpRequest.newBuilder()
				.uri(URI.create(metadataTokenEndpoint + receivingFunctionUrl))
				.GET()
				.header("Metadata-Flavor", "Google")
				.build();

		//retrieving the jwt token from metadata service
		try {
			java.net.http.HttpResponse<String> tokenResponse =
					client.send(tokenRequest, java.net.http.HttpResponse.BodyHandlers.ofString());
			token = tokenResponse.body();
		} catch (IOException | InterruptedException e) {
			logger.info("Error: Token not received!");
		}
	}
}
