package com.blizzard.demo;

import org.apache.beam.sdk.options.StreamingOptions;
import org.apache.beam.sdk.options.Validation.Required;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.Default;

public interface DataflowPipelineOption extends PipelineOptions, StreamingOptions {
	@Description("Pub/Sub topic to read from. " + "Name format should look like: "
			+ "projects/<project-id>/topics/<input-topic-name>. (project-id is default now)")
//	@Default.String("${pubsub.default.input.topic}")
	@Required
	String getInputTopic();
	void setInputTopic(String inputTopic);

	@Description("Pub/Sub topic to publish to. " + "Name format should look like"
			+ "projects/<project-id>/topics/<output-topic-name>. (project-id is default now)")
//	@Default.String("${pubsub.default.output.topic}")
	@Required
	String getOutputTopic();
	void setOutputTopic(String outputTopic);

	@Description("GCP project name")
//	@Default.String("${pubsub.default.output.topic}")
	@Required
	String getProject();
	void setProject(String outputTopic);

	@Description("Build Number retrieved from git")
	String getBuildNumber();
	void setBuildNumber(String buildNumber);
}
