package br.com.murilo;

import br.com.murilo.model.UrlData;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Main implements RequestHandler<Map<String, Object>, Map<String, String>> {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final S3Client s3Client = S3Client.builder().build();

    @Override
    public Map<String, String> handleRequest(Map<String, Object> input, Context context) {
        String body = input.get("body").toString();

        Map<String, String> bodyMap;

        try {
            bodyMap = objectMapper.readValue(body, Map.class);

        } catch (Exception e) {
            throw new RuntimeException("Error parsing JSON body: " + e.getMessage());
        }

        String originalUrl = bodyMap.get("originalUrl");
        long expirationTime = Long.parseLong(bodyMap.get("expirationTime"));

        UrlData urlData = new UrlData(originalUrl, expirationTime);

        String shortUrl = UUID.randomUUID().toString().substring(0, 8);

        try {
            String urlDataJson = objectMapper.writeValueAsString(urlData);

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket("italiano-url-shortener-storage")
                    .key(shortUrl + ".json")
                    .build();

            s3Client.putObject(request, RequestBody.fromString(urlDataJson));

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error saving URL to Bucket S3: " + e.getMessage());
        }

        Map<String, String> response = new HashMap<>();
        response.put("newUrl", shortUrl);

        return response;
    }
}
