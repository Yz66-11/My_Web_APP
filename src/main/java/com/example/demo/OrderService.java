package com.example.demo;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ShopService shopService;
    private final DishRepository dishRepository;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        ShopService shopService,
                        DishRepository dishRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.shopService = shopService;
        this.dishRepository = dishRepository;
    }

    @Transactional
    public Order createOrder(User user, Long shopId, List<Map<String, Object>> items, String note) {
        Shop shop = shopService.findById(shopId)
                .orElseThrow(() -> new RuntimeException("店铺不存在"));

        if (items == null || items.isEmpty()) {
            throw new RuntimeException("订单不能为空");
        }

        Order order = new Order();
        order.setUser(user);
        order.setShop(shop);
        order.setNote(note);
        order = orderRepository.save(order);

        BigDecimal total = BigDecimal.ZERO;

        for (Map<String, Object> item : items) {
            Long dishId = ((Number) item.get("dishId")).longValue();
            int quantity = ((Number) item.get("quantity")).intValue();

            Dish dish = dishRepository.findById(dishId)
                    .orElseThrow(() -> new RuntimeException("菜品不存在: " + dishId));

            if (dish.getStatus() != Dish.DishStatus.AVAILABLE) {
                throw new RuntimeException("菜品「" + dish.getDishName() + "」已下架");
            }

            OrderItem oi = new OrderItem(order, dishId, dish.getDishName(),
                    dish.getPrice(), quantity);
            orderItemRepository.save(oi);

            total = total.add(dish.getPrice().multiply(BigDecimal.valueOf(quantity)));
        }

        order.setTotalAmount(total);
        return orderRepository.save(order);
    }

    public List<Order> getMyOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Order getOrderDetail(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));
    }

    public List<OrderItem> getOrderItems(Long orderId) {
        return orderItemRepository.findByOrderId(orderId);
    }
}
