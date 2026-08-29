package com.recoup.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.recoup.backend.service.PaymentGateway;
import com.recoup.backend.service.RazorpayTestModeGateway;
import com.recoup.backend.service.SimulatedPaymentGateway;

@Configuration
public class GatewayConfig {

    private static final Logger log = LoggerFactory.getLogger(GatewayConfig.class);

    @Bean
    public PaymentGateway paymentGateway(@Value("${razorpay.key-id:}") String keyId,
                                          @Value("${razorpay.key-secret:}") String keySecret) {
        if (!keyId.isBlank() && !keySecret.isBlank()) {
            try {
                log.info("Razorpay test-mode credentials found -- using RazorpayTestModeGateway");
                return new RazorpayTestModeGateway(keyId, keySecret);
            } catch (Exception e) {
                log.warn("Failed to initialise Razorpay client, falling back to SimulatedPaymentGateway: {}", e.getMessage());
            }
        }
        log.info("No Razorpay credentials configured -- using SimulatedPaymentGateway");
        return new SimulatedPaymentGateway();
    }
}
