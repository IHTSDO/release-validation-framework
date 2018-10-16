package org.ihtsdo.rvf.execution.service;

import java.io.IOException;

import org.ihtsdo.otf.dao.s3.OfflineS3ClientImpl;
import org.ihtsdo.otf.dao.s3.S3Client;
import org.ihtsdo.otf.dao.s3.S3ClientImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

import com.amazonaws.auth.BasicAWSCredentials;

@Configuration
@PropertySources({
	@PropertySource(value = "classpath:execution-service-defaults.properties"),
	@PropertySource(value = "file:${rvfConfigLocation}/execution-service.properties", ignoreResourceNotFound=true)})
public class ExecutionServiceConfig {
	
	@Bean("s3Client")
	public S3Client createS3Client(@Value("${offlineMode}") boolean isOffline,
								   @Value("${aws.key}") String awsKey,
								   @Value("${aws.privateKey}") String privateKey) throws IOException {
		if (isOffline) {
			return new OfflineS3ClientImpl();
		}
		return new S3ClientImpl(new BasicAWSCredentials(awsKey, privateKey));
	}
}
