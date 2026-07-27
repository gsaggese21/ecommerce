package interview.ecommerce.exceptions;

import interview.ecommerce.entity.OrderStatus;

public class InvalidOrderStateException extends DomainException{
    public InvalidOrderStateException(Long orderId, OrderStatus oldOrderStatus, OrderStatus newOrderStatus) {
        super("Order with identifier '%s' can't change status from '%s' to '%s'".formatted(orderId,oldOrderStatus.name(), newOrderStatus.name()));
    }

}
