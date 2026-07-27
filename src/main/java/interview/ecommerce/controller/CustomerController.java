package interview.ecommerce.controller;

import interview.ecommerce.dto.CreateCustomerRequest;
import interview.ecommerce.dto.CustomerDto;
import interview.ecommerce.dto.PageResponse;
import interview.ecommerce.service.CustomerService;
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
@RequestMapping("/customers")
@Tag(name = "Customers", description = "Create and Get All Customers")
public class CustomerController {

    private static final Logger log = LoggerFactory.getLogger(CustomerController.class);

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a customer")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Customer created"),
            @ApiResponse(responseCode = "400", description = "Malformed request or failed validation",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public CustomerDto createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        log.debug("POST /customers");
        return customerService.createCustomer(request);
    }

    @GetMapping
    @Operation(summary = " Returns a paginated list of customers.")
    @ApiResponse(responseCode = "200", description = "Page of customers")
    public PageResponse<CustomerDto> findAll(
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        log.debug("GET /customers {}", pageable);
        return customerService.findAll(pageable);
    }
}
