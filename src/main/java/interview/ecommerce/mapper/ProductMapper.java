package interview.ecommerce.mapper;

import interview.ecommerce.dto.CreateProductRequest;
import interview.ecommerce.dto.ProductDto;
import interview.ecommerce.entity.Product;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ProductMapper {

    public Product createProductRequesttoEntity(CreateProductRequest request){
        return new Product(request.productCode(),
                request.productName(),
                request.stock());
    }

    public ProductDto productEntityToDto(Product product){
        return new ProductDto(product.getId(),product.getCode(),product.getName(), product.getStock());
    }

}
