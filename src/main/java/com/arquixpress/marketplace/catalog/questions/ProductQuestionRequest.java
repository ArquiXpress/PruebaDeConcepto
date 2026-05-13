package com.arquixpress.marketplace.catalog.questions;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductQuestionRequest(
        @NotBlank @Size(max = 700) String question
) {
}
