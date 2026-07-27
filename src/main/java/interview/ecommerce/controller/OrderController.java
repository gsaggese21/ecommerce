package interview.ecommerce.controller;

import interview.ecommerce.dto.*;
import interview.ecommerce.entity.OrderStatus;
import interview.ecommerce.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@RequestMapping(path = "/orders")
@Tag(name = "Orders", description = "Create, list and manage the lifecycle of orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);


    private final OrderService orderService;

    public OrderController(OrderService orderService){
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an order",
            description = """
                    Creates and persists an order. Items with the same product code are merged into a single
                    line.
                    Stock is decremented under a pessimistic write lock.
                    If one of the requested quantity exceeds the stock the whole order is rejected with 409.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order created"),
            @ApiResponse(responseCode = "400", description = "Malformed request or failed validation",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Customer or product not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Insufficient stock for one of the requested products",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public OrderDto createOrder(@Valid @RequestBody CreateOrderRequest requestDto) {
        log.debug("POST /orders");
        return orderService.createOrder(requestDto);
    }

    @GetMapping
    @Operation(summary = "List orders",
            description = """
                    Returns a paginated list of orders, each including its items and current status.
                    Pagination is controlled by the page, size and sort parameters.
                    Defaults are page 0, size 20, sorted by id ascending.
                    """)
    @ApiResponse(responseCode = "200", description = "Page of orders, possibly empty")
    public PageResponse<OrderDto> getAllOrders(
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        log.debug("GET /orders - {}", pageable);
        return orderService.getAllOrders(pageable);
    }

    @PatchMapping("/{orderId}")
    @Operation(summary = "Mark an order as DELIVERED",
            description = """
                    Moves an order to status DELIVERED.
                    The transition is allowed only from status ORDERED. From status CANCELED or DELIVERED
                    it returns a 409.
                    The transition is guarded by optimistic locking.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order marked as DELIVERED"),
            @ApiResponse(responseCode = "404", description = "Order not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Order not in status ORDERED, or concurrent modification",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public OrderDto deliverOrder(
            @Parameter(description = "Identifier of the order, as returned by POST /orders", example = "1")
            @PathVariable Long orderId) {
        log.debug("PATCH /orders/{}", orderId);
        return orderService.changeStatus(orderId, OrderStatus.DELIVERED);
    }

    @DeleteMapping("/{orderId}")
    @Operation(summary = "Cancel an order",
            description = """
                    Soft delete of an order, restoring the stock of every product it contains.
                    The order is not removed from the system, its status is set to CANCELED and it
                    keeps appearing in the list.
                    The cancellation is allowed only from status ORDERED. From status CANCELED or DELIVERED
                    it returns a 409.
                    Stock is restored under a pessimistic write lock and the transition is guarded
                    by optimistic locking.
                    The updated order is returned in the response body.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order canceled and stock restored"),
            @ApiResponse(responseCode = "404", description = "Order not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Order not in status ORDERED, or concurrent modification",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public OrderDto cancelOrder(
            @Parameter(description = "Identifier of the order, as returned by POST /orders", example = "1")
            @PathVariable Long orderId) {
        log.debug("DELETE /orders/{}", orderId);
        return orderService.changeStatus(orderId, OrderStatus.CANCELED);
    }
}