package interview.ecommerce.controller;

import interview.ecommerce.dto.CreateProductRequest;
import interview.ecommerce.dto.PageResponse;
import interview.ecommerce.dto.ProductDto;
import interview.ecommerce.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/products")
@Tag( name = "Products", description = "Create and Get all products")
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a product",
            description = """
                    Creates a product with its initial stock.
                    The product code is the business identifier and must be unique. Creating a product with 
                    an existing code is rejected with a 409.
                    Stock must be zero or positive. 
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product created"),
            @ApiResponse(responseCode = "400", description = "Malformed request or failed validation",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "A product with the same code already exists",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ProductDto createProduct(@Valid @RequestBody CreateProductRequest productRequest) {
        log.debug("POST /products");
        return productService.create(productRequest);
    }

    @GetMapping
    @Operation(summary = "List products",
            description = """
                    Returns a paginated list of products.
                    Pagination is controlled by the page, size and sort parameters;
                    defaults are page 0, size 20, sorted by id ascending.
                    """)
    @ApiResponse(responseCode = "200", description = "Page of products, possibly empty")
    public PageResponse<ProductDto> getAllProducts(
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        log.debug("GET /products - {}", pageable);
        return productService.findAll(pageable);
    }
}
