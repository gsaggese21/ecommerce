package interview.ecommerce.service;

import interview.ecommerce.dto.*;
import interview.ecommerce.entity.*;
import interview.ecommerce.exceptions.InsufficientStockException;
import interview.ecommerce.exceptions.InvalidOrderStateException;
import interview.ecommerce.exceptions.ResourceNotFoundException;
import interview.ecommerce.mapper.OrderMapper;
import interview.ecommerce.repository.OrderRepository;
import interview.ecommerce.service.handler.OrderStatusTransitionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;


@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CustomerService customerService;

    @Mock
    private ProductService productService;

    @Mock
    private OrderStatusTransitionHandler deliverHandler;

    @Mock
    private OrderStatusTransitionHandler cancelHandler;

    private static final OrderMapper orderMapper = new OrderMapper();

    private OrderService orderService;

    private Customer customer;
    private Product product1;
    private Product product2;

    @BeforeEach
    void setup() {
        customer = new Customer("Mario", "Rossi", LocalDate.of(1979, 4, 21), "MRARSS79D26T215V", "mario@gmail.com");
        customer.setId(1L);
        product1 = new Product("PR1", "product name", 10);
        product2 = new Product("PR2", "product name 2", 20);

        when(deliverHandler.orderStatus()).thenReturn(OrderStatus.DELIVERED);
        when(cancelHandler.orderStatus()).thenReturn(OrderStatus.CANCELED);

        orderService = new OrderService(orderRepository, customerService, productService, orderMapper,
                List.of(deliverHandler, cancelHandler));
    }



    @Test
    public void createOrderOkTest() {
        when(customerService.findCustomerEntityById(1L)).thenReturn(customer);
        when(productService.findProductByCodeForUpdate("PR1")).thenReturn(product1);
        when(productService.findProductByCodeForUpdate("PR2")).thenReturn(product2);
        mockSaveOrder();

        CreateOrderRequest createOrderRequest = new CreateOrderRequest(1L,
                List.of(new OrderItemDto("PR1", 2), new OrderItemDto("PR2", 5)));

        OrderDto orderDto = orderService.createOrder(createOrderRequest);

        ArgumentCaptor<Order> orderArgumentCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderArgumentCaptor.capture());
        Order orderSaved = orderArgumentCaptor.getValue();

        assertThat(orderSaved.getCustomer()).isEqualTo(customer);
        assertThat(orderSaved.getOrderItems()).hasSize(2);
        assertThat(orderSaved.getStatus()).isEqualTo(OrderStatus.ORDERED);
        assertThat(orderDto).isNotNull();
        assertThat(orderDto.getOrderId()).isEqualTo(1L);
        assertThat(orderDto.getOrderStatus()).isEqualTo(OrderStatus.ORDERED);
    }

    @Test
    public void createOrderDecreaseStockTest() {
        when(customerService.findCustomerEntityById(1L)).thenReturn(customer);
        when(productService.findProductByCodeForUpdate("PR1")).thenReturn(product1);
        when(productService.findProductByCodeForUpdate("PR2")).thenReturn(product2);
        mockSaveOrder();

        CreateOrderRequest createOrderRequest = new CreateOrderRequest(1L,
                List.of(new OrderItemDto("PR1", 2), new OrderItemDto("PR2", 5)));

        orderService.createOrder(createOrderRequest);

        assertThat(product1.getStock()).isEqualTo(8);
        assertThat(product2.getStock()).isEqualTo(15);
    }

    @Test
    public void createOrderGroupSameProductCodeTest() {
        when(customerService.findCustomerEntityById(1L)).thenReturn(customer);
        when(productService.findProductByCodeForUpdate("PR1")).thenReturn(product1);
        mockSaveOrder();

        CreateOrderRequest createOrderRequest = new CreateOrderRequest(1L,
                List.of(new OrderItemDto("PR1", 2), new OrderItemDto("PR1", 3)));

        orderService.createOrder(createOrderRequest);

        ArgumentCaptor<Order> orderArgumentCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderArgumentCaptor.capture());

        verify(productService, times(1)).findProductByCodeForUpdate("PR1");
        assertThat(orderArgumentCaptor.getValue().getOrderItems()).hasSize(1);
        assertThat(orderArgumentCaptor.getValue().getOrderItems().get(0).getQuantity()).isEqualTo(5);
        assertThat(product1.getStock()).isEqualTo(5);
    }

    @Test
    public void createOrderLocksProductsInProductCodeOrderTest() {
        when(customerService.findCustomerEntityById(1L)).thenReturn(customer);
        when(productService.findProductByCodeForUpdate("PR1")).thenReturn(product1);
        when(productService.findProductByCodeForUpdate("PR2")).thenReturn(product2);
        mockSaveOrder();

        CreateOrderRequest createOrderRequest = new CreateOrderRequest(1L,
                List.of(new OrderItemDto("PR2", 1), new OrderItemDto("PR1", 1)));

        orderService.createOrder(createOrderRequest);

        InOrder inOrder = inOrder(productService);
        inOrder.verify(productService).findProductByCodeForUpdate("PR1");
        inOrder.verify(productService).findProductByCodeForUpdate("PR2");
    }

    @Test
    public void createOrderEmptyItemsTest() {
        when(customerService.findCustomerEntityById(1L)).thenReturn(customer);
        mockSaveOrder();

        CreateOrderRequest createOrderRequest = new CreateOrderRequest(1L, List.of());

        OrderDto orderDto = orderService.createOrder(createOrderRequest);

        ArgumentCaptor<Order> orderArgumentCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderArgumentCaptor.capture());

        assertThat(orderArgumentCaptor.getValue().getOrderItems()).isEmpty();
        assertThat(orderDto).isNotNull();
        verifyNoInteractions(productService);
    }

    @Test
    public void createOrderCustomerNotFound404Test() {
        when(customerService.findCustomerEntityById(99L))
                .thenThrow(ResourceNotFoundException.customer(99L));

        CreateOrderRequest createOrderRequest = new CreateOrderRequest(99L,
                List.of(new OrderItemDto("PR1", 2)));

        assertThatThrownBy(() -> orderService.createOrder(createOrderRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        verifyNoInteractions(orderRepository, productService);
    }

    @Test
    public void createOrderProductNotFound404Test() {
        when(customerService.findCustomerEntityById(1L)).thenReturn(customer);
        when(productService.findProductByCodeForUpdate("PR3"))
                .thenThrow(ResourceNotFoundException.product("PR3"));
        mockSaveOrder();

        CreateOrderRequest createOrderRequest = new CreateOrderRequest(1L,
                List.of(new OrderItemDto("PR3", 2)));

        assertThatThrownBy(() -> orderService.createOrder(createOrderRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("PR3");
    }

    @Test
    public void createOrderInsufficientStockTest() {
        when(customerService.findCustomerEntityById(1L)).thenReturn(customer);
        when(productService.findProductByCodeForUpdate("PR1")).thenReturn(product1);
        mockSaveOrder();

        CreateOrderRequest createOrderRequest = new CreateOrderRequest(1L,
                List.of(new OrderItemDto("PR1", 100)));

        assertThatThrownBy(() -> orderService.createOrder(createOrderRequest))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("PR1");

        assertThat(product1.getStock()).isEqualTo(10);
    }

    @Test
    public void createOrderInsufficientStockOnSecondLineTest() {
        when(customerService.findCustomerEntityById(1L)).thenReturn(customer);
        when(productService.findProductByCodeForUpdate("PR1")).thenReturn(product1);
        when(productService.findProductByCodeForUpdate("PR2")).thenReturn(product2);
        mockSaveOrder();

        CreateOrderRequest createOrderRequest = new CreateOrderRequest(1L,
                List.of(new OrderItemDto("PR1", 1), new OrderItemDto("PR2", 999)));

        assertThatThrownBy(() -> orderService.createOrder(createOrderRequest))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("PR2");

        assertThat(product2.getStock()).isEqualTo(20);
    }
    

    @Test
    public void changeStatusToDeliveredOkTest() {
        Order order = orderWith(1L, product1, 5, OrderStatus.ORDERED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        OrderDto orderDto = orderService.changeStatus(1L, OrderStatus.DELIVERED);

        assertThat(orderDto.getOrderStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);

        verify(deliverHandler).onTransition(order);
        verify(cancelHandler, never()).onTransition(any());
    }

    @Test
    public void changeStatusToCanceledOkTest() {
        Order order = orderWith(1L, product1, 5, OrderStatus.ORDERED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        OrderDto orderDto = orderService.changeStatus(1L, OrderStatus.CANCELED);

        assertThat(orderDto.getOrderStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);

        verify(cancelHandler).onTransition(order);
        verify(deliverHandler, never()).onTransition(any());
    }

    @Test
    public void changeStatusAppliesTransitionBeforeInvokingHandlerTest() {
        Order order = orderWith(1L, product1, 5, OrderStatus.ORDERED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        doAnswer(invocation -> {
            Order received = invocation.getArgument(0);
            assertThat(received.getStatus()).isEqualTo(OrderStatus.CANCELED);
            return null;
        }).when(cancelHandler).onTransition(any(Order.class));

        orderService.changeStatus(1L, OrderStatus.CANCELED);

        verify(cancelHandler).onTransition(order);
    }

    @Test
    public void changeStatusNotRequestableStatusTest() {
        assertThatThrownBy(() -> orderService.changeStatus(1L, OrderStatus.ORDERED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ORDERED");

        verifyNoInteractions(orderRepository);
    }

    @Test
    public void changeStatusOrderNotFound404Test() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.changeStatus(99L, OrderStatus.DELIVERED))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        verify(deliverHandler, never()).onTransition(any());
    }

    @Test
    public void changeStatusDeliverAlreadyDeliveredExceptionTest() {
        Order order = orderWith(1L, product1, 5, OrderStatus.DELIVERED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.changeStatus(1L, OrderStatus.DELIVERED))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("1");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        verify(deliverHandler, never()).onTransition(any());
    }

    @Test
    public void changeStatusDeliverAlreadyCanceledExceptionTest() {
        Order order = orderWith(1L, product1, 5, OrderStatus.CANCELED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.changeStatus(1L, OrderStatus.DELIVERED))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("1");

        verify(deliverHandler, never()).onTransition(any());
    }

    @Test
    public void changeStatusCancelAlreadyCanceledDoesNotRestoreStockTest() {
        Order order = orderWith(1L, product2, 6, OrderStatus.CANCELED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.changeStatus(1L, OrderStatus.CANCELED))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("1");

        verify(cancelHandler, never()).onTransition(any());
        assertThat(product2.getStock()).isEqualTo(20);
    }

    @Test
    public void changeStatusCancelAlreadyDeliveredExceptionTest() {
        Order order = orderWith(1L, product2, 6, OrderStatus.DELIVERED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.changeStatus(1L, OrderStatus.CANCELED))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("1");

        verify(cancelHandler, never()).onTransition(any());
    }


    @Test
    public void getAllOrdersTest() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("id"));

        Order order1 = orderWith(1L, product1, 2, OrderStatus.ORDERED);
        Order order2 = orderWith(2L, product2, 5, OrderStatus.DELIVERED);

        when(orderRepository.findOrderIds(pageable))
                .thenReturn(new PageImpl<>(List.of(1L, 2L), pageable, 2));
        when(orderRepository.findByIdIn(List.of(1L, 2L)))
                .thenReturn(List.of(order1, order2));

        PageResponse<OrderDto> response = orderService.getAllOrders(pageable);

        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.totalPages()).isEqualTo(1);
        assertThat(response.last()).isTrue();

        assertThat(response.content()).hasSize(2);

        OrderDto first = response.content().get(0);
        assertThat(first.getOrderId()).isEqualTo(1L);
        assertThat(first.getCustomerId()).isEqualTo(1L);
        assertThat(first.getOrderItems()).hasSize(1);
        assertThat(first.getOrderItems().get(0).getProductCode()).isEqualTo("PR1");
        assertThat(first.getOrderItems().get(0).getQuantity()).isEqualTo(2);

        assertThat(response.content().get(1).getOrderItems().get(0).getProductCode()).isEqualTo("PR2");
        assertThat(response.content().get(1).getOrderStatus()).isEqualTo(OrderStatus.DELIVERED);

        verify(orderRepository).findOrderIds(pageable);
        verify(orderRepository).findByIdIn(List.of(1L, 2L));
    }

    @Test
    public void getAllOrdersSecondPageTest() {
        Pageable pageable = PageRequest.of(1, 1, Sort.by("id"));

        Order order2 = orderWith(2L, product2, 5, OrderStatus.ORDERED);

        when(orderRepository.findOrderIds(pageable))
                .thenReturn(new PageImpl<>(List.of(2L), pageable, 2));
        when(orderRepository.findByIdIn(List.of(2L))).thenReturn(List.of(order2));

        PageResponse<OrderDto> response = orderService.getAllOrders(pageable);

        assertThat(response.page()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(2);
        assertThat(response.last()).isTrue();
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).getOrderId()).isEqualTo(2L);
    }

    @Test
    public void getAllOrdersEmptyTest() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("id"));

        when(orderRepository.findOrderIds(pageable)).thenReturn(Page.empty(pageable));

        PageResponse<OrderDto> response = orderService.getAllOrders(pageable);

        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
        assertThat(response.totalPages()).isZero();

        verify(orderRepository).findOrderIds(pageable);
        verify(orderRepository, never()).findByIdIn(anyCollection());
    }


    private void mockSaveOrder() {
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order orderReceived = invocation.getArgument(0);
            assertThat(orderReceived.getId()).isNull();
            orderReceived.setId(1L);
            return orderReceived;
        });
    }

    private Order orderWith(Long id, Product product, int quantity, OrderStatus orderStatus) {
        Order order = new Order(customer, orderStatus);
        order.setId(id);
        order.setOrderItems(List.of(new OrderItem(order, product, quantity)));
        return order;
    }

}