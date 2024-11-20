package br.com.murilo;

import br.com.murilo.model.OriginalUrlData;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class Main implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final S3Client s3Client = S3Client.builder().build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        String shortUrlCode = input.get("rawPath")
                .toString()
                .replace("/", "");

        if (shortUrlCode.isBlank()) {
            throw new IllegalArgumentException("Invalid input: 'shortUlCode' is required.");
        }

        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .bucket("italiano-url-shortener-storage")
                .key(shortUrlCode + ".json")
                .build();

        InputStream s3objectStream;

        try {
            s3objectStream = s3Client.getObject(objectRequest);
        } catch(Exception ex) {
            throw new RuntimeException("Error fetching data from S3: " + ex.getMessage());
        }

        OriginalUrlData originalUrlData;

        try {
            originalUrlData = objectMapper.readValue(s3objectStream, OriginalUrlData.class);
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing JSON body: " + ex.getMessage());
        }

        long currentTimeInSeconds = System.currentTimeMillis() / 1000;
        Map<String, Object> response = new HashMap<>();

        if (originalUrlData.getExpirationTime() <= currentTimeInSeconds) {
            response.put("statusCode", 410);
            response.put("body", "The URL has expired.");
            return response;
        }

        Map<String, String> header = new HashMap<>();
        header.put("Location", originalUrlData.getOriginalUrl());
        response.put("statusCode", 302);
        response.put("headers", header);
        return response;
    }
}
