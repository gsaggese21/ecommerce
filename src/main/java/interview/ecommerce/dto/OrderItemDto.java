package interview.ecommerce.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class OrderItemDto {

    @JsonProperty("product_code")
    @NotNull
    private String productCode;

    @Min(1)
    private int quantity;


    public OrderItemDto(String productCode, int quantity) {
        this.productCode = productCode;
        this.quantity = quantity;
    }

    public OrderItemDto() {
    }


    public String getProductCode() {
        return productCode;
    }


    public int getQuantity() {
        return quantity;
    }

}
