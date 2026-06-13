package com.vwholesale.dispatch.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vwholesale.common.context.MerchantContext;
import com.vwholesale.common.enums.OrderStatus;
import com.vwholesale.common.exception.BusinessException;
import com.vwholesale.common.security.RoleChecker;
import com.vwholesale.customer.entity.Customer;
import com.vwholesale.customer.mapper.CustomerMapper;
import com.vwholesale.dispatch.dto.WorkerItemUpdateRequest;
import com.vwholesale.dispatch.dto.WorkerTaskItemVO;
import com.vwholesale.dispatch.dto.WorkerTaskVO;
import com.vwholesale.dispatch.entity.DispatchLog;
import com.vwholesale.dispatch.mapper.DispatchLogMapper;
import com.vwholesale.order.entity.Order;
import com.vwholesale.order.entity.OrderItem;
import com.vwholesale.order.mapper.OrderItemMapper;
import com.vwholesale.order.mapper.OrderMapper;
import com.vwholesale.product.entity.Product;
import com.vwholesale.product.mapper.ProductMapper;
import com.vwholesale.worker.entity.Worker;
import com.vwholesale.worker.service.WorkerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DispatchService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final CustomerMapper customerMapper;
    private final ProductMapper productMapper;
    private final DispatchLogMapper dispatchLogMapper;
    private final WorkerService workerService;
    private final MerchantContext merchantContext;

    @Transactional
    public WorkerTaskVO assignOrder(Long orderId, Long workerId) {
        RoleChecker.requireBoss();
        Order order = getOrderOrThrow(orderId);
        Worker worker = workerService.getWorkerOrThrow(workerId);

        if (!OrderStatus.PENDING_PICK.equals(order.getStatus())
                && !OrderStatus.PICKING.equals(order.getStatus())) {
            throw BusinessException.of(400, "只有待分拣或分拣中的订单可以派单");
        }

        Long fromWorkerId = order.getAssignedWorkerId();
        String action = fromWorkerId == null ? "ASSIGN" : "REASSIGN";

        order.setAssignedWorkerId(worker.getId());
        orderMapper.updateById(order);

        DispatchLog log = new DispatchLog();
        log.setOrderId(order.getId());
        log.setFromWorkerId(fromWorkerId);
        log.setToWorkerId(worker.getId());
        log.setAction(action);
        log.setOperatorUserId(RoleChecker.currentUserId());
        dispatchLogMapper.insert(log);

        return toWorkerTaskVO(order, true);
    }

    public List<WorkerTaskVO> listTasksForWorker() {
        Long workerId = requireWorkerId();
        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getMerchantId, merchantContext.currentMerchantId())
                .eq(Order::getAssignedWorkerId, workerId)
                .in(Order::getStatus, OrderStatus.PENDING_PICK, OrderStatus.PICKING, OrderStatus.PICKED)
                .orderByAsc(Order::getDeliveryDate)
                .orderByDesc(Order::getId));
        return orders.stream().map(order -> toWorkerTaskVO(order, false)).toList();
    }

    public WorkerTaskVO getTaskDetail(Long orderId) {
        Order order = getWorkerOrderOrThrow(orderId);
        return toWorkerTaskVO(order, true);
    }

    @Transactional
    public WorkerTaskVO startPicking(Long orderId) {
        Order order = getWorkerOrderOrThrow(orderId);
        if (!OrderStatus.PENDING_PICK.equals(order.getStatus())) {
            throw BusinessException.of(400, "只有待分拣订单可以开始分拣");
        }
        order.setStatus(OrderStatus.PICKING);
        orderMapper.updateById(order);
        return toWorkerTaskVO(order, true);
    }

    @Transactional
    public WorkerTaskVO updateItem(Long orderId, Long itemId, WorkerItemUpdateRequest request) {
        Order order = getWorkerOrderOrThrow(orderId);
        if (!OrderStatus.PICKING.equals(order.getStatus())) {
            throw BusinessException.of(400, "请先开始分拣");
        }
        OrderItem item = orderItemMapper.selectOne(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getId, itemId)
                .eq(OrderItem::getOrderId, orderId));
        if (item == null) {
            throw BusinessException.of(404, "订单明细不存在");
        }
        if (request.getActualQty() != null) {
            item.setActualQty(request.getActualQty());
        }
        if (request.getShortageFlag() != null) {
            item.setShortageFlag(request.getShortageFlag());
        }
        if (request.getPickRemark() != null) {
            item.setPickRemark(request.getPickRemark());
        }
        orderItemMapper.updateById(item);
        return toWorkerTaskVO(order, true);
    }

    @Transactional
    public WorkerTaskVO markPicked(Long orderId) {
        Order order = getWorkerOrderOrThrow(orderId);
        if (!OrderStatus.PICKING.equals(order.getStatus())) {
            throw BusinessException.of(400, "只有分拣中订单可以标记已拣完");
        }
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        for (OrderItem item : items) {
            if (item.getActualQty() == null) {
                item.setActualQty(item.getOrderQty());
                orderItemMapper.updateById(item);
            }
        }
        order.setStatus(OrderStatus.PICKED);
        orderMapper.updateById(order);
        return toWorkerTaskVO(order, true);
    }

    @Transactional
    public WorkerTaskVO markDelivered(Long orderId) {
        Order order = getWorkerOrderOrThrow(orderId);
        if (!OrderStatus.PICKED.equals(order.getStatus())) {
            throw BusinessException.of(400, "只有已拣完订单可以标记送达");
        }
        order.setStatus(OrderStatus.PENDING_PRICE);
        orderMapper.updateById(order);
        return toWorkerTaskVO(order, true);
    }

    private Order getWorkerOrderOrThrow(Long orderId) {
        RoleChecker.requireWorker();
        Long workerId = requireWorkerId();
        Order order = getOrderOrThrow(orderId);
        if (!Objects.equals(order.getAssignedWorkerId(), workerId)) {
            throw BusinessException.of(403, "无权操作该订单");
        }
        return order;
    }

    private Long requireWorkerId() {
        RoleChecker.requireWorker();
        Long workerId = RoleChecker.currentWorkerId();
        if (workerId == null) {
            throw BusinessException.of(403, "工人档案未初始化，请重新登录");
        }
        return workerId;
    }

    private Order getOrderOrThrow(Long id) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getId, id)
                .eq(Order::getMerchantId, merchantContext.currentMerchantId()));
        if (order == null) {
            throw BusinessException.of(404, "订单不存在");
        }
        return order;
    }

    private WorkerTaskVO toWorkerTaskVO(Order order, boolean withItems) {
        Customer customer = customerMapper.selectById(order.getCustomerId());
        WorkerTaskVO.WorkerTaskVOBuilder builder = WorkerTaskVO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .customerName(customer != null ? customer.getName() : "客户")
                .deliveryAddressShort(order.getDeliveryAddressShort())
                .status(order.getStatus())
                .statusLabel(statusLabel(order.getStatus()))
                .deliveryDate(order.getDeliveryDate())
                .remark(order.getRemark())
                .createdAt(order.getCreatedAt());

        if (!withItems) {
            long count = orderItemMapper.selectCount(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
            return builder.itemCount((int) count).build();
        }

        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId())
                .orderByAsc(OrderItem::getId));
        Set<Long> productIds = items.stream().map(OrderItem::getProductId).collect(Collectors.toSet());
        Map<Long, Product> productMap = productIds.isEmpty()
                ? Map.of()
                : productMapper.selectBatchIds(productIds).stream().collect(Collectors.toMap(Product::getId, p -> p));

        List<WorkerTaskItemVO> itemVOs = items.stream().map(item -> WorkerTaskItemVO.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .productName(productMap.get(item.getProductId()) != null ? productMap.get(item.getProductId()).getName() : "未知商品")
                .orderQty(item.getOrderQty())
                .actualQty(item.getActualQty())
                .unit(item.getUnit())
                .shortageFlag(item.getShortageFlag())
                .pickRemark(item.getPickRemark())
                .build()).toList();

        return builder.items(itemVOs).itemCount(itemVOs.size()).build();
    }

    private String statusLabel(String status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case OrderStatus.PENDING_PICK -> "待分拣";
            case OrderStatus.PICKING -> "分拣中";
            case OrderStatus.PICKED -> "已拣完";
            case OrderStatus.PENDING_PRICE -> "待录价";
            default -> status;
        };
    }
}
