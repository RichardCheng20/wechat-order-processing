package com.vwholesale.payment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vwholesale.common.context.MerchantContext;
import com.vwholesale.common.exception.BusinessException;
import com.vwholesale.common.security.RoleChecker;
import com.vwholesale.payment.dto.BossSupplierReportVO;
import com.vwholesale.payment.dto.PurchasePaymentCreateRequest;
import com.vwholesale.payment.dto.PurchasePaymentVO;
import com.vwholesale.payment.dto.SupplierReportRow;
import com.vwholesale.payment.dto.SupplierReportSummary;
import com.vwholesale.payment.entity.PurchasePayment;
import com.vwholesale.payment.mapper.PurchasePaymentMapper;
import com.vwholesale.supplier.entity.Supplier;
import com.vwholesale.supplier.mapper.SupplierMapper;
import com.vwholesale.supplier.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchasePaymentService {

    private final PurchasePaymentMapper purchasePaymentMapper;
    private final SupplierMapper supplierMapper;
    private final SupplierService supplierService;
    private final MerchantContext merchantContext;

    @Transactional
    public PurchasePaymentVO create(PurchasePaymentCreateRequest request) {
        RoleChecker.requireBoss();
        Long merchantId = merchantContext.currentMerchantId();

        Supplier supplier = supplierService.resolveForPayment(request.getSupplierId(), request.getSupplierName());

        BigDecimal amount = request.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw BusinessException.of(400, "请输入有效金额");
        }

        String method = StringUtils.hasText(request.getMethod()) ? request.getMethod().trim().toUpperCase() : "OTHER";
        LocalDateTime paidAt = request.getPaidAt() != null ? request.getPaidAt() : LocalDateTime.now();

        PurchasePayment payment = new PurchasePayment();
        payment.setMerchantId(merchantId);
        payment.setSupplierId(supplier.getId());
        payment.setAmount(amount);
        payment.setMethod(method);
        payment.setPaidAt(paidAt);
        payment.setOperatorUserId(RoleChecker.currentUserId());
        payment.setRemark(trimRemark(request.getRemark()));
        if (request.getVoucherUrls() != null && !request.getVoucherUrls().isEmpty()) {
            payment.setVoucherUrls(String.join(",", request.getVoucherUrls()));
        }
        purchasePaymentMapper.insert(payment);

        supplierService.addPaidAmount(supplier.getId(), amount);

        return toVO(payment, supplier.getName());
    }

    public BossSupplierReportVO supplierReport(LocalDate dateFrom, LocalDate dateTo) {
        RoleChecker.requireOwnerAdmin();
        if (dateFrom == null || dateTo == null) {
            throw BusinessException.of(400, "请选择日期范围");
        }
        if (dateFrom.isAfter(dateTo)) {
            throw BusinessException.of(400, "开始日期不能晚于结束日期");
        }
        if (dateFrom.plusDays(366).isBefore(dateTo)) {
            throw BusinessException.of(400, "日期范围不能超过 366 天");
        }

        Long merchantId = merchantContext.currentMerchantId();
        List<PurchasePayment> payments = purchasePaymentMapper.selectList(new LambdaQueryWrapper<PurchasePayment>()
                .eq(PurchasePayment::getMerchantId, merchantId)
                .ge(PurchasePayment::getPaidAt, dateFrom.atStartOfDay())
                .lt(PurchasePayment::getPaidAt, dateTo.plusDays(1).atStartOfDay()));

        Map<Long, ReportAgg> aggMap = new HashMap<>();
        BigDecimal totalPaid = BigDecimal.ZERO;

        for (PurchasePayment payment : payments) {
            if (payment.getSupplierId() == null || payment.getAmount() == null) {
                continue;
            }
            totalPaid = totalPaid.add(payment.getAmount());
            ReportAgg agg = aggMap.computeIfAbsent(payment.getSupplierId(), k -> new ReportAgg());
            agg.supplierId = payment.getSupplierId();
            agg.paidAmount = agg.paidAmount.add(payment.getAmount());
            agg.paymentCount += 1;
        }

        Set<Long> supplierIds = aggMap.keySet();
        Map<Long, Supplier> supplierMap = supplierIds.isEmpty()
                ? Map.of()
                : supplierMapper.selectBatchIds(supplierIds).stream()
                        .collect(Collectors.toMap(Supplier::getId, s -> s));

        List<SupplierReportRow> rows = aggMap.values().stream()
                .map(agg -> {
                    Supplier supplier = supplierMap.get(agg.supplierId);
                    String name = supplier != null ? supplier.getName() : "未知供应商";
                    return SupplierReportRow.builder()
                            .supplierId(agg.supplierId)
                            .supplierName(name)
                            .paidAmount(agg.paidAmount)
                            .paymentCount(agg.paymentCount)
                            .build();
                })
                .sorted((a, b) -> b.getPaidAmount().compareTo(a.getPaidAmount()))
                .toList();

        SupplierReportSummary summary = SupplierReportSummary.builder()
                .paidAmount(totalPaid)
                .paymentCount(payments.size())
                .supplierCount((int) rows.stream()
                        .map(SupplierReportRow::getSupplierId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .count())
                .build();

        return BossSupplierReportVO.builder()
                .summary(summary)
                .rows(rows)
                .build();
    }

    private static class ReportAgg {
        private Long supplierId;
        private BigDecimal paidAmount = BigDecimal.ZERO;
        private int paymentCount;
    }

    private String trimRemark(String remark) {
        if (!StringUtils.hasText(remark)) {
            return null;
        }
        String trimmed = remark.trim();
        return trimmed.length() > 200 ? trimmed.substring(0, 200) : trimmed;
    }

    private PurchasePaymentVO toVO(PurchasePayment payment, String supplierName) {
        return PurchasePaymentVO.builder()
                .id(payment.getId())
                .supplierId(payment.getSupplierId())
                .supplierName(supplierName)
                .amount(payment.getAmount())
                .method(payment.getMethod())
                .paidAt(payment.getPaidAt())
                .remark(payment.getRemark())
                .voucherUrls(parseVoucherUrls(payment.getVoucherUrls()))
                .createdAt(payment.getCreatedAt())
                .build();
    }

    private List<String> parseVoucherUrls(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Collections.emptyList();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }
}
