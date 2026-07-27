package interview.ecommerce.entity;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum OrderStatus {
    ORDERED, DELIVERED, CANCELED;


    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
            ORDERED,   EnumSet.of(DELIVERED, CANCELED),
            DELIVERED, EnumSet.noneOf(OrderStatus.class),
            CANCELED,  EnumSet.noneOf(OrderStatus.class));

    public boolean canTransitionTo(OrderStatus status) {
        return TRANSITIONS.get(this).contains(status);
    }
}
