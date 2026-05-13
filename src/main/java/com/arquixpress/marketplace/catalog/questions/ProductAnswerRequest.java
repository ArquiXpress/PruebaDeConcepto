package com.arquixpress.marketplace.catalog.questions;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductAnswerRequest(
        @NotBlank @Size(max = 1200) String answer
) {
}
