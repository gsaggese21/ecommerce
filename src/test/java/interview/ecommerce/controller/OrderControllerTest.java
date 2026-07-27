package interview.ecommerce.controller;

import interview.ecommerce.dto.*;
import interview.ecommerce.entity.OrderStatus;
import interview.ecommerce.exceptions.GlobalExceptionHandler;
import interview.ecommerce.exceptions.InsufficientStockException;
import interview.ecommerce.exceptions.ResourceNotFoundException;
import interview.ecommerce.service.OrderService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(OrderController.class)
@Import(GlobalExceptionHandler.class)
public class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    private static final String CREATE_ORDER_BODY = """
            {
              "customer_id": 1,
              "items": [
                { "product_code": "PR1", "quantity": 2 }
              ]
            }
            """;

    private static final String ORDER_ID_BODY = """
            {
                "order_id" : 1
            }
            """;

    @Test
    public void createOrder201Test() throws Exception {
        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(newOrderDto());

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_ORDER_BODY))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.order_id").value(1))
                .andExpect(jsonPath("$.customer_id").value(1))
                .andExpect(jsonPath("$.products.length()").value(1))
                .andExpect(jsonPath("$.products[0].product_code").value("PR1"))
                .andExpect(jsonPath("$.products[0].quantity").value(2));
    }

    @Test
    public void updateDeliveredOrder200Test() throws Exception {
        when(orderService.changeStatus(1L, OrderStatus.DELIVERED)).thenReturn(new OrderDto(1L, 1L, List.of(new OrderItemDto("PR1", 2)), OrderStatus.DELIVERED));

        mockMvc.perform(patch("/orders/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ORDER_ID_BODY))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.order_id").value(1))
                .andExpect(jsonPath("$.order_status").value(String.valueOf(OrderStatus.DELIVERED)));
    }

    @Test
    public void cancelOrder200Test() throws Exception {
        when(orderService.changeStatus(1L, OrderStatus.CANCELED)).thenReturn(new OrderDto(1L, 1L, List.of(new OrderItemDto("PR1", 2)), OrderStatus.CANCELED));

        mockMvc.perform(delete("/orders/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ORDER_ID_BODY))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.order_id").value(1))
                .andExpect(jsonPath("$.order_status").value(String.valueOf(OrderStatus.CANCELED)));
    }

    @Test
    public void createOrderDeserializesRequestTest() throws Exception {
        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(newOrderDto());

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_ORDER_BODY))
                .andExpect(status().isCreated());

        ArgumentCaptor<CreateOrderRequest> requestCaptor = ArgumentCaptor.forClass(CreateOrderRequest.class);
        verify(orderService).createOrder(requestCaptor.capture());
        CreateOrderRequest received = requestCaptor.getValue();

        assertThat(received.customerId()).isEqualTo(1L);
        assertThat(received.orderItemDto().size()).isEqualTo(1);
        assertThat(received.orderItemDto().get(0).getProductCode()).isEqualTo("PR1");
        assertThat(received.orderItemDto().get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    public void createOrderInsufficientStock409Test() throws Exception {
        when(orderService.createOrder(any(CreateOrderRequest.class)))
                .thenThrow(new InsufficientStockException("PR1", 5, 10));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_ORDER_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Insufficient stock"))
                .andExpect(jsonPath("$.detail").value(containsString("PR1")));
    }

    @Test
    public void createOrderCustomerNotFound404Test() throws Exception {
        when(orderService.createOrder(any(CreateOrderRequest.class)))
                .thenThrow(ResourceNotFoundException.order(1L));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_ORDER_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Entity not found"))
                .andExpect(jsonPath("$.detail").value(containsString("Order with identifier '1' doesn't exists")));
    }

    @Test
    public void createOrderMissingCustomerId400Test() throws Exception {
        String body = """
                { "orders": [ { "product_code": "PR1", "quantity": 2 } ] }
                """;

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }

    @Test
    public void createOrderEmptyOrders400Test() throws Exception {
        String body = """
                { "customer_id": 1, "orders": [] }
                """;

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }

    @Test
    public void createOrderQuantityBelowMinimum400Test() throws Exception {
        String body = """
                { "customer_id": 1, "orders": [ { "product_code": "PR1", "quantity": 0 } ] }
                """;

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }

    @Test
    public void createOrderMissingProductCode400Test() throws Exception {
        String body = """
                { "customer_id": 1, "orders": [ { "quantity": 2 } ] }
                """;

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }

    @Test
    public void createOrderMalformedJson400Test() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ not json"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }

    @Test
    public void getAllOrders200Test() throws Exception {
        when(orderService.getAllOrders(any(Pageable.class))).thenReturn(new PageResponse<>(List.of(newOrderDto()),1,20, 1,1,true));

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].order_id").value(1))
                .andExpect(jsonPath("$.content[0].products[0].product_code").value("PR1"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    public void getAllOrdersEmpty200Test() throws Exception {
        when(orderService.getAllOrders(any(Pageable.class))).thenReturn(new PageResponse<>(List.of(),1,20,0,1,true));

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    private OrderDto newOrderDto() {
        return new OrderDto(1L, 1L, List.of(new OrderItemDto("PR1", 2)), OrderStatus.ORDERED);
    }
}
