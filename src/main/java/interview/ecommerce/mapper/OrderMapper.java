package interview.ecommerce.mapper;


import interview.ecommerce.dto.OrderDto;
import interview.ecommerce.dto.OrderItemDto;
import interview.ecommerce.entity.Order;
import interview.ecommerce.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class OrderMapper {


    public OrderDto orderToDto(Order order){
        List<OrderItemDto> orderItemsDtos = orderItemsToDtos(order.getOrderItems());

        return new OrderDto(order.getId(), order.getCustomer().getId(), orderItemsDtos,order.getStatus());
    }

    public OrderItemDto orderItemToDto(OrderItem orderItem){
        return new OrderItemDto(orderItem.getProduct().getCode(), orderItem.getQuantity());
    }

    public List<OrderItemDto> orderItemsToDtos(List<OrderItem> orderItems){
        List<OrderItemDto> orderItemDtos = new ArrayList<>();
        for(OrderItem o : orderItems){
            orderItemDtos.add(orderItemToDto(o));
        }
        return orderItemDtos;
    }
}
