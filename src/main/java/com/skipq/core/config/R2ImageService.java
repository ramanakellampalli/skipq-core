package com.skipq.core.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class R2ImageService {

    private static final List<String> FOLDERS = List.of(
            "breakfast", "beverages", "rice", "veg", "non-veg", "snacks"
    );

    private final S3Client s3;
    private final String bucketName;
    private final String publicUrl;

    private final Map<String, List<String>> cache = new ConcurrentHashMap<>();

    public R2ImageService(
            @Value("${app.r2.account-id}") String accountId,
            @Value("${app.r2.access-key-id}") String accessKeyId,
            @Value("${app.r2.secret-access-key}") String secretAccessKey,
            @Value("${app.r2.bucket-name}") String bucketName,
            @Value("${app.r2.public-url}") String publicUrl) {

        this.bucketName = bucketName;
        this.publicUrl = publicUrl;
        this.s3 = S3Client.builder()
                .endpointOverride(URI.create("https://" + accountId + ".r2.cloudflarestorage.com"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .region(Region.of("auto"))
                .build();
    }

    @PostConstruct
    @Scheduled(fixedRate = 86400000L)
    public void refreshCache() {
        log.info("Refreshing R2 image cache...");
        for (String folder : FOLDERS) {
            try {
                List<String> urls = listFolder(folder);
                cache.put(folder, urls);
                log.info("Cached {} images for folder '{}'", urls.size(), folder);
            } catch (Exception e) {
                log.error("Failed to list R2 folder '{}': {}", folder, e.getMessage());
            }
        }
    }

    public List<String> getImages(String folder) {
        return cache.getOrDefault(folder, Collections.emptyList());
    }

    public List<String> getRandomImages(List<String> folders, int count) {
        List<String> pool = new ArrayList<>();
        for (String folder : folders) {
            pool.addAll(cache.getOrDefault(folder, Collections.emptyList()));
        }
        Collections.shuffle(pool);
        return pool.stream().limit(count).toList();
    }

    private List<String> listFolder(String folder) {
        var request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(folder + "/")
                .build();

        return s3.listObjectsV2(request)
                .contents()
                .stream()
                .filter(o -> !o.key().endsWith("/"))
                .map(S3Object::key)
                .map(key -> publicUrl + "/" + key)
                .toList();
    }
}
