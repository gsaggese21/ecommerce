package interview.ecommerce.service.handler;

import interview.ecommerce.entity.*;
import interview.ecommerce.repository.OrderRepository;
import interview.ecommerce.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CancelOrderHandlerTest {

    @Mock
    private ProductService productService;

    @Mock
    private OrderRepository orderRepository;

    private CancelHandler handler;

    private Customer customer;
    private Product product1;
    private Product product2;

    @BeforeEach
    void setup() {
        customer = new Customer("Mario", "Rossi", LocalDate.of(1979, 4, 21),
                "MRARSS79D26T215V", "mario@gmail.com");
        customer.setId(1L);
        product1 = new Product("PR1", "product name", 10);
        product2 = new Product("PR2", "product name 2", 20);

        handler = new CancelHandler(productService, orderRepository);
    }

    @Test
    public void handlesCanceledStatusTest() {
        assertThat(handler.orderStatus()).isEqualTo(OrderStatus.CANCELED);
    }

    @Test
    public void restoresStockOfEveryItemTest() {
        Order order = orderWith(1L, List.of(item(product1, 3), item(product2, 7)));
        when(orderRepository.findProductCodesByOrderId(1L)).thenReturn(List.of("PR1", "PR2"));

        handler.onTransition(order);

        assertThat(product1.getStock()).isEqualTo(13);
        assertThat(product2.getStock()).isEqualTo(27);
    }


    @Test
    public void locksProductsInProductCodeOrderTest() {
        Order order = orderWith(1L, List.of(item(product2, 1), item(product1, 1)));
        when(orderRepository.findProductCodesByOrderId(1L)).thenReturn(List.of("PR1", "PR2"));

        handler.onTransition(order);

        InOrder inOrder = inOrder(productService);
        inOrder.verify(productService).findProductByCodeForUpdate("PR1");
        inOrder.verify(productService).findProductByCodeForUpdate("PR2");
        verifyNoMoreInteractions(productService);
    }

    @Test
    public void locksEveryProductExactlyOnceTest() {
        Order order = orderWith(1L, List.of(item(product1, 2), item(product2, 4)));
        when(orderRepository.findProductCodesByOrderId(1L)).thenReturn(List.of("PR1", "PR2"));

        handler.onTransition(order);

        verify(productService, times(1)).findProductByCodeForUpdate("PR1");
        verify(productService, times(1)).findProductByCodeForUpdate("PR2");
    }

    @Test
    public void orderWithoutItemsRestoresNothingTest() {
        Order order = orderWith(1L, List.of());
        when(orderRepository.findProductCodesByOrderId(1L)).thenReturn(List.of());

        handler.onTransition(order);

        verifyNoInteractions(productService);
        assertThat(product1.getStock()).isEqualTo(10);
    }



    private OrderItem item(Product product, int quantity) {
        OrderItem orderItem = new OrderItem();
        orderItem.setProduct(product);
        orderItem.setQuantity(quantity);
        return orderItem;
    }

    private Order orderWith(Long id, List<OrderItem> items) {
        Order order = new Order(customer, OrderStatus.CANCELED);
        order.setId(id);

        List<OrderItem> owned = new ArrayList<>(items);
        owned.forEach(i -> i.setOrder(order));
        order.setOrderItems(owned);

        return order;
    }

}