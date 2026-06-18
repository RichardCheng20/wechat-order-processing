package com.vwholesale.payment.service;

import com.vwholesale.common.context.MerchantContext;
import com.vwholesale.common.exception.BusinessException;
import com.vwholesale.common.security.RoleChecker;
import com.vwholesale.payment.dto.PurchasePaymentCreateRequest;
import com.vwholesale.payment.dto.PurchasePaymentVO;
import com.vwholesale.payment.entity.PurchasePayment;
import com.vwholesale.payment.mapper.PurchasePaymentMapper;
import com.vwholesale.supplier.entity.Supplier;
import com.vwholesale.supplier.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PurchasePaymentService {

    private final PurchasePaymentMapper purchasePaymentMapper;
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
