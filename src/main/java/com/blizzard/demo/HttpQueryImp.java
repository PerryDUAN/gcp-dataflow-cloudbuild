package com.blizzard.demo;

import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage;

import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.springframework.beans.factory.annotation.Value;


import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HttpQueryImp extends PTransform<PCollection<PubsubMessage>, PCollection<PubsubMessage>> {
    @Override
    public PCollection<PubsubMessage> expand(PCollection<PubsubMessage> input) {
        input.apply(ParDo.of(new RequestFn()));
        return null;
    }
    //sending a simple http request to endpoint url
    //previous attribute solution method:
    //public static void simpleRequest(String token) throws IOException, InterruptedException {}

    private static class RequestFn<T> extends DoFn<PubsubMessage, PubsubMessage> {
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

        @ProcessElement
        public void process(final ProcessContext context) {
            logger.info("Getting target url token at first!");

            getToken();

            logger.info("Token is: ");
            logger.info(token);
            logger.info("Started sending http request!");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(receivingFunctionUrl))
                    .setHeader("Authorization", "Bearer " + token)
                    .build();

            final PubsubMessage message = context.element();

            try {
                HttpResponse<String> response =
                        client.send(request, HttpResponse.BodyHandlers.ofString());
                logger.info(response.body());
            } catch (Exception e) {
                logger.info("Error: Not request properly.");
            }
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
}
