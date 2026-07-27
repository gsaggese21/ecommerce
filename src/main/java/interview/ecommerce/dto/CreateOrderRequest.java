package interview.ecommerce.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateOrderRequest (@JsonProperty("customer_id") @NotNull Long customerId,
                                  @JsonProperty("items") @NotEmpty @NotNull @Valid List<OrderItemDto> orderItemDto){}
