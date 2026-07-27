package interview.ecommerce.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateProductRequest(@JsonProperty("product_code") @NotEmpty String productCode,
                                   @JsonProperty("product_name") String productName,
                                   @PositiveOrZero int stock) {}
