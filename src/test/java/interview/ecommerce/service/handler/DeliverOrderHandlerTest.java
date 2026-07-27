package interview.ecommerce.service.handler;

import interview.ecommerce.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

public class DeliverOrderHandlerTest {

    private DeliverHandler handler;
    private Customer customer;
    private Product product;

    @BeforeEach
    void setup() {
        handler = new DeliverHandler();
        customer = new Customer("Mario", "Rossi", LocalDate.of(1979, 4, 21),
                "MRARSS79D26T215V", "mario@gmail.com");
        customer.setId(1L);
        product = new Product("PR1", "product name", 10);
    }

    @Test
    public void handlesDeliveredStatusTest() {
        assertThat(handler.orderStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    public void doesNotTouchStockTest() {
        Order order = new Order(customer, OrderStatus.DELIVERED);
        order.setId(1L);
        order.setOrderItems(List.of(new OrderItem(order, product, 4)));

        assertThatNoException().isThrownBy(() -> handler.onTransition(order));

        assertThat(product.getStock()).isEqualTo(10);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

}