package com.recoup.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Locks in the request-boundary behaviour that isn't covered by service-level
 *  unit tests: a bogus batch size shouldn't 500 the server, and the API key
 *  gate on the one money-relevant endpoint actually gates. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiValidationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private HttpEntity<Void> withApiKey(String key) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", key);
        return new HttpEntity<>(headers);
    }

    @Test
    void rejectsABatchSizeOfZero() {
        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/batches/run?size=0&seed=1", withApiKey("test-key"), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsABatchSizeAboveTheCap() {
        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/batches/run?size=999999&seed=1", withApiKey("test-key"), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void acceptsAnOrdinaryBatchSize() {
        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/batches/run?size=5&seed=1", withApiKey("test-key"), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void rejectsARunWithoutTheApiKey() {
        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/batches/run?size=5&seed=1", null, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsARunWithTheWrongApiKey() {
        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/batches/run?size=5&seed=1", withApiKey("wrong-key"), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsAnUnknownCaseStatusFilterInsteadOf500() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/cases?batchId=nonexistent&status=NOT_A_REAL_STATUS", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsANonNumericCaseIdInsteadOf500() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/cases/not-a-number", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
