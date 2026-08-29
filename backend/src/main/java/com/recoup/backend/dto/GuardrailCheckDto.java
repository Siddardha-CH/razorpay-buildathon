package com.recoup.backend.dto;

import com.recoup.backend.model.GuardrailCheck;

public record GuardrailCheckDto(String name, boolean passed, String detail) {
    public static GuardrailCheckDto from(GuardrailCheck g) {
        return new GuardrailCheckDto(g.getName(), g.isPassed(), g.getDetail());
    }
}
