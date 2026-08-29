package com.recoup.backend.service;

import com.recoup.backend.model.ActionResult;
import com.recoup.backend.model.CauseCategory;
import com.recoup.backend.model.RevenueEvent;

public interface PaymentGateway {

    String name();

    ActionResult retryPayment(RevenueEvent event, CauseCategory category);

    ActionResult createPaymentLink(RevenueEvent event, CauseCategory category);
}
