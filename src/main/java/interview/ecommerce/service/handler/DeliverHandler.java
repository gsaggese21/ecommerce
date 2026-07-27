package interview.ecommerce.service.handler;

import interview.ecommerce.entity.Order;
import interview.ecommerce.entity.OrderStatus;
import org.springframework.stereotype.Component;

@Component
class DeliverHandler implements OrderStatusTransitionHandler {
    @Override
    public OrderStatus orderStatus() {
        return OrderStatus.DELIVERED;
    }

    @Override
    public void onTransition(Order order) {
    }

}


