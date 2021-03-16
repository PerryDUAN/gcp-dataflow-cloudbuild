package com.blizzard.demo;
import org.apache.beam.sdk.transforms.DoFn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage;

public class MessagesTransform extends DoFn<PubsubMessage, PubsubMessage>{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger LOG = LoggerFactory.getLogger(MessagesTransform.class);

    @ProcessElement
    public void process(ProcessContext context) {
        LOG.info(String.format("Received message %s", new String(context.element().getPayload())));
        PubsubMessage msg = context.element();
        context.output(msg);
    }
}
