package interview.ecommerce.repository;

import interview.ecommerce.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order,Long> {

    @Query("select o.id from Order o")
    Page<Long> findOrderIds(Pageable pageable);

    @EntityGraph(attributePaths = {"orderItems", "orderItems.product"})
    List<Order> findByIdIn(Collection<Long> ids);

    @Query("""
           select distinct oi.product.code
           from Order o
           join o.orderItems oi
           where o.id = :orderId
           order by oi.product.code
           """)
    List<String> findProductCodesByOrderId(@Param("orderId") Long orderId);
}
