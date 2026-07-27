package interview.ecommerce.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import interview.ecommerce.entity.OrderStatus;

import java.util.List;

public class OrderDto {

    @JsonProperty("order_id")
    private Long orderId;

    @JsonProperty("customer_id")
    private Long customerId;

    @JsonProperty("products")
    private List<OrderItemDto> orderItems;

    @JsonProperty("order_status")
    private OrderStatus orderStatus;


    public OrderDto(Long orderId, Long customerId, List<OrderItemDto> orderItems, OrderStatus orderStatus) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.orderItems = orderItems;
        this.orderStatus = orderStatus;
    }

    public OrderDto() {
    }

    public Long getOrderId() {
        return orderId;
    }


    public Long getCustomerId() {
        return customerId;
    }

    public List<OrderItemDto> getOrderItems() {
        return orderItems;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }
}
