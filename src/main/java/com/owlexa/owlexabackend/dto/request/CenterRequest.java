package com.owlexa.owlexabackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CenterRequest {
    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "subdomain is required")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "subdomain only allows lowercase letters, numbers and hyphen")
    private String subdomain;
}
