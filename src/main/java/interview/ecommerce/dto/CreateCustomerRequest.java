package interview.ecommerce.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record CreateCustomerRequest(
        @JsonProperty("first_name")
        @NotBlank(message = "first_name is required")
        String firstName,

        @JsonProperty("last_name")
        @NotBlank(message = "last_name is required")
        String lastName,

        @JsonProperty("birth_date")
        @JsonFormat(pattern = "yyyy-MM-dd")
        @Past(message = "birth_date must be in the past")
        LocalDate birthDate,

        @NotBlank(message = "taxcode is required")
        @Pattern(regexp = "^[A-Z0-9]{16}$",
                message = "taxcode must be exactly 16 uppercase letters or digits")
        String taxcode,

        @Email(message = "email must be a well-formed address")
        String email) {}


