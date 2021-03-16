package com.blizzard.demo;

import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.Validation.Required;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.Default;

public interface DataflowPipelineOption extends DataflowPipelineOptions {
	@Description("Pub/Sub topic to read from. " + "Name format should look like: "
			+ "projects/<project-id>/topics/<input-topic-name>. (project- id is default now)")
	@Default.String("${pubsub.default.input.topic}")
	@Required
	String getInputTopic();

	void setInputTopic(String inputTopic);

	@Description("Pub/Sub topic to publish to. " + "Name format should look like"
			+ "projects/<project-id>/topics/<output-topic-name>. (project-id is default now)")
	@Default.String("${pubsub.default.output.topic}")
	@Required
	String getOutputTopic();

	void setOutputTopic(String outputTopic);

	@Description("Build Number for cloud run")
	String getBuildNumber();

	void setBuildNumber(String buildNumber);
}
