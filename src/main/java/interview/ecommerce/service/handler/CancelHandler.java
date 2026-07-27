package interview.ecommerce.service.handler;

import interview.ecommerce.entity.Order;
import interview.ecommerce.entity.OrderItem;
import interview.ecommerce.entity.OrderStatus;
import interview.ecommerce.repository.OrderRepository;
import interview.ecommerce.service.ProductService;
import org.springframework.stereotype.Component;

@Component
class CancelHandler implements OrderStatusTransitionHandler {

    private final ProductService productService;
    private final OrderRepository orderRepository;

    public CancelHandler(ProductService productService, OrderRepository orderRepository) {
        this.productService = productService;
        this.orderRepository = orderRepository;
    }

    @Override
    public OrderStatus orderStatus() {
        return OrderStatus.CANCELED;
    }

    @Override
    public void onTransition(Order order) {
        orderRepository.findProductCodesByOrderId(order.getId()).stream()
                .sorted()
                .forEach(productService::findProductByCodeForUpdate);

        for (OrderItem item : order.getOrderItems())
            item.getProduct().increaseStock(item.getQuantity());
    }
}