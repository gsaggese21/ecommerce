package interview.ecommerce.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public class ProductDto {

    @JsonProperty("product_code")
    @NotNull
    private String code;

    @JsonProperty("product_id")
    private Long id;

    private String name;

    private int stock;

    public ProductDto(Long id, String code, String name, int stock) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.stock = stock;
    }

    public ProductDto() {
    }

    public String getCode() {
        return code;
    }


    public String getName() {
        return name;
    }

    public int getStock() {
        return stock;
    }

    public Long getId() {
        return id;
    }

}
