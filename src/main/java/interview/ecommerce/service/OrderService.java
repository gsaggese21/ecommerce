package interview.ecommerce.service;

import interview.ecommerce.dto.*;
import interview.ecommerce.entity.*;
import interview.ecommerce.exceptions.ResourceNotFoundException;
import interview.ecommerce.mapper.OrderMapper;
import interview.ecommerce.repository.OrderRepository;
import interview.ecommerce.service.handler.OrderStatusTransitionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderMapper orderMapper;
    private final OrderRepository orderRepository;
    private final CustomerService customerService;
    private final ProductService productService;
    private final Map<OrderStatus, OrderStatusTransitionHandler> handlers;

    public OrderService(OrderRepository orderRepository,
                        CustomerService customerService,
                        ProductService productService,
                        OrderMapper orderMapper,
                        List<OrderStatusTransitionHandler> handlers) {
        this.customerService = customerService;
        this.orderRepository = orderRepository;
        this.productService = productService;
        this.orderMapper = orderMapper;
        this.handlers = handlers.stream()
                .collect(Collectors.toMap(OrderStatusTransitionHandler::orderStatus, Function.identity()));
    }

    @Transactional
    public OrderDto createOrder(CreateOrderRequest requestDto) {
        log.debug("Creating order for customer {} with {} requested lines",
                requestDto.customerId(), requestDto.orderItemDto().size());

        Customer customer = customerService.findCustomerEntityById(requestDto.customerId());
        Order order = orderRepository.save(new Order(customer, OrderStatus.ORDERED));

        List<OrderItemDto> orderItemDtos = groupOrdersByProductCode(requestDto.orderItemDto());

        for (OrderItemDto o : orderItemDtos) {
            Product product = productService.findProductByCodeForUpdate(o.getProductCode());
            product.decreaseStock(o.getQuantity());
            order.addOrderItem(new OrderItem(order, product, o.getQuantity()));

            log.debug("Order {}: product {} decreased by {}, remaining {}",
                    order.getId(), product.getCode(), o.getQuantity(), product.getStock());
        }

        log.info("Order {} created for customer {} with {} products",
                order.getId(), customer.getId(), order.getOrderItems().size());

        return orderMapper.orderToDto(order);
    }

    public PageResponse<OrderDto> getAllOrders(Pageable pageable) {
        log.debug("Finding orders: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());

        Page<Long> ids = orderRepository.findOrderIds(pageable);

        if (ids.isEmpty()) {
            log.debug("Order not found on page {} (total elements: {})",
                    pageable.getPageNumber(), ids.getTotalElements());
            return PageResponse.of(new PageImpl<Order>(List.of(), pageable, ids.getTotalElements()),
                    orderMapper::orderToDto);
        }

        Map<Long, Order> ordersById = orderRepository.findByIdIn(ids.getContent()).stream()
                .collect(Collectors.toMap(Order::getId, Function.identity(), (a, b) -> a));

        List<Order> orders = ids.getContent().stream()
                .map(ordersById::get)
                .filter(Objects::nonNull)
                .toList();

        log.debug("Retrieved {} orders out of {}", orders.size(), ids.getTotalElements());

        return PageResponse.of(new PageImpl<>(orders, pageable, ids.getTotalElements()),
                orderMapper::orderToDto);
    }


    @Transactional
    public OrderDto changeStatus(Long orderId, OrderStatus orderStatus) {
        OrderStatusTransitionHandler handler = handlers.get(orderStatus);
        if (handler == null)
            throw new IllegalArgumentException("Status %s is not requestable".formatted(orderStatus));

        Order order = findOrderById(orderId);
        order.transitionTo(orderStatus);
        handler.onTransition(order);

        log.info("Order {} moved to status {}", orderId, orderStatus);
        return orderMapper.orderToDto(order);
    }


    private List<OrderItemDto> groupOrdersByProductCode(List<OrderItemDto> orderItemDtos) {
        List<OrderItemDto> grouped = orderItemDtos.stream()
                .collect(Collectors.groupingBy(
                        OrderItemDto::getProductCode,
                        Collectors.summingInt(OrderItemDto::getQuantity)))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new OrderItemDto(e.getKey(), e.getValue()))
                .toList();

        if (grouped.size() < orderItemDtos.size())
            log.debug("{} requested lines grouped into {} distinct products",
                    orderItemDtos.size(), grouped.size());

        return grouped;
    }

    private Order findOrderById(Long id) {
        log.trace("Finding order by id {}", id);

        return orderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.order(id));
    }

}