package org.sfa.volunteer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class S3Config {

    @Value("${aws.s3.access-key:}")
    private String accessKey;

    @Value("${aws.s3.secret-key:}")
    private String secretKey;

    @Value("${aws.s3.region}")
    private String region;
    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Bean
    public S3Client s3Client() {
        // Use IAM Role (if on AWS) OR Static Keys (if provided)
        if (accessKey.isEmpty() || secretKey.isEmpty()) {
            return S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(InstanceProfileCredentialsProvider.create()) // IAM Role
                    .build();
        } else {
            return S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                    .build();
        }
    }

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder().region(Region.US_EAST_1).build();
    }
    public List<String> listBuckets() {
        ListBucketsResponse response = s3Client().listBuckets();
        return response.buckets().stream().map(Bucket::name).toList();
    }
    public  List<String> listFoldersInBucket() {
        ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .delimiter("/") // This tells S3 to simulate a folder structure
                .build();

        ListObjectsV2Response response = s3Client().listObjectsV2(listObjectsV2Request);
        return response.commonPrefixes().stream().map(CommonPrefix::prefix).collect(Collectors.toList());
    }


}
