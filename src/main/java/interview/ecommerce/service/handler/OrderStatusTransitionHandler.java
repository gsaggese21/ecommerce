package interview.ecommerce.service.handler;

import interview.ecommerce.entity.Order;
import interview.ecommerce.entity.OrderStatus;

public interface OrderStatusTransitionHandler {
    OrderStatus orderStatus();
    void onTransition(Order order);
}
