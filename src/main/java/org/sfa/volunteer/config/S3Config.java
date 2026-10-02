package org.sfa.volunteer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {

    @Value("${saayam.s3.regions.us:us-east-1}")
    private String usRegion;

    @Value("${saayam.s3.regions.eu:eu-west-1}")
    private String euRegion;

    @Bean("s3ClientUs")
    public S3Client s3ClientUs() {
        return S3Client.builder()
                .region(Region.of(usRegion))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(false)
                        .build())
                .build();
    }

    @Bean("s3ClientEu")
    public S3Client s3ClientEu() {
        return S3Client.builder()
                .region(Region.of(euRegion))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(false)
                        .build())
                .build();
    }

    @Bean("s3PresignerUs")
    public S3Presigner s3PresignerUs() {
        return S3Presigner.builder().region(Region.of(usRegion)).build();
    }

    @Bean("s3PresignerEu")
    public S3Presigner s3PresignerEu() {
        return S3Presigner.builder().region(Region.of(euRegion)).build();
    }
}