package com.vwholesale.supplier.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vwholesale.common.context.MerchantContext;
import com.vwholesale.common.exception.BusinessException;
import com.vwholesale.common.security.RoleChecker;
import com.vwholesale.payment.entity.PurchasePayment;
import com.vwholesale.payment.mapper.PurchasePaymentMapper;
import com.vwholesale.supplier.dto.SupplierCreateRequest;
import com.vwholesale.supplier.dto.SupplierUpdateRequest;
import com.vwholesale.supplier.dto.SupplierVO;
import com.vwholesale.supplier.entity.Supplier;
import com.vwholesale.supplier.mapper.SupplierMapper;
import com.vwholesale.supplier.support.SupplierNoGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierMapper supplierMapper;
    private final PurchasePaymentMapper purchasePaymentMapper;
    private final SupplierNoGenerator supplierNoGenerator;
    private final MerchantContext merchantContext;

    public List<SupplierVO> listForBoss(String keyword) {
        RoleChecker.requireBoss();
        Long merchantId = merchantContext.currentMerchantId();
        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<Supplier>()
                .eq(Supplier::getMerchantId, merchantId)
                .orderByDesc(Supplier::getId);
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Supplier::getName, kw)
                    .or().like(Supplier::getSupplierNo, kw)
                    .or().like(Supplier::getContactName, kw)
                    .or().like(Supplier::getPhone, kw));
        }
        return supplierMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    public SupplierVO getById(Long id) {
        RoleChecker.requireBoss();
        return toVO(getSupplierOrThrow(id));
    }

    @Transactional
    public SupplierVO create(SupplierCreateRequest request) {
        RoleChecker.requireBoss();
        String name = request.getName().trim();
        Supplier existing = findByName(name);
        if (existing != null) {
            return toVO(existing);
        }
        Supplier supplier = new Supplier();
        supplier.setMerchantId(merchantContext.currentMerchantId());
        supplier.setSupplierNo(supplierNoGenerator.nextNo(supplier.getMerchantId()));
        supplier.setName(name);
        supplier.setContactName(trimToNull(request.getContactName()));
        supplier.setPhone(trimToNull(request.getPhone()));
        supplier.setRemark(trimToNull(request.getRemark()));
        supplier.setPayableAmount(BigDecimal.ZERO);
        supplier.setPaidAmount(BigDecimal.ZERO);
        supplier.setStatus(1);
        supplierMapper.insert(supplier);
        return toVO(supplier);
    }

    @Transactional
    public SupplierVO update(Long id, SupplierUpdateRequest request) {
        RoleChecker.requireBoss();
        Supplier supplier = getSupplierOrThrow(id);
        if (StringUtils.hasText(request.getName())) {
            String name = request.getName().trim();
            Supplier duplicate = findByName(name);
            if (duplicate != null && !duplicate.getId().equals(id)) {
                throw BusinessException.of(400, "供应商名称已存在");
            }
            supplier.setName(name);
        }
        if (request.getContactName() != null) {
            supplier.setContactName(trimToNull(request.getContactName()));
        }
        if (request.getPhone() != null) {
            supplier.setPhone(trimToNull(request.getPhone()));
        }
        if (request.getRemark() != null) {
            supplier.setRemark(trimToNull(request.getRemark()));
        }
        if (request.getStatus() != null) {
            supplier.setStatus(request.getStatus());
        }
        supplierMapper.updateById(supplier);
        return toVO(supplier);
    }

    @Transactional
    public void delete(Long id) {
        RoleChecker.requireBoss();
        Supplier supplier = getSupplierOrThrow(id);
        Long paymentCount = purchasePaymentMapper.selectCount(new LambdaQueryWrapper<PurchasePayment>()
                .eq(PurchasePayment::getSupplierId, id)
                .eq(PurchasePayment::getMerchantId, merchantContext.currentMerchantId()));
        if (paymentCount != null && paymentCount > 0) {
            throw BusinessException.of(400, "该供应商已有付款记录，无法删除");
        }
        BigDecimal payable = supplier.getPayableAmount() != null ? supplier.getPayableAmount() : BigDecimal.ZERO;
        BigDecimal paid = supplier.getPaidAmount() != null ? supplier.getPaidAmount() : BigDecimal.ZERO;
        if (payable.subtract(paid).compareTo(BigDecimal.ZERO) > 0) {
            throw BusinessException.of(400, "该供应商仍有未结清应付，无法删除");
        }
        supplierMapper.deleteById(id);
    }

    @Transactional
    public Supplier resolveForPayment(Long supplierId, String supplierName) {
        RoleChecker.requireBoss();
        Long merchantId = merchantContext.currentMerchantId();
        if (supplierId != null) {
            Supplier supplier = supplierMapper.selectById(supplierId);
            if (supplier == null || !merchantId.equals(supplier.getMerchantId())) {
                throw BusinessException.of(400, "供应商不存在");
            }
            if (supplier.getStatus() != null && supplier.getStatus() == 0) {
                throw BusinessException.of(400, "供应商已停用");
            }
            return supplier;
        }
        if (!StringUtils.hasText(supplierName)) {
            throw BusinessException.of(400, "请选择或输入供应商名称");
        }
        String name = supplierName.trim();
        Supplier existing = findByName(name);
        if (existing != null) {
            if (existing.getStatus() != null && existing.getStatus() == 0) {
                throw BusinessException.of(400, "供应商已停用");
            }
            return existing;
        }
        Supplier supplier = new Supplier();
        supplier.setMerchantId(merchantId);
        supplier.setSupplierNo(supplierNoGenerator.nextNo(merchantId));
        supplier.setName(name);
        supplier.setPayableAmount(BigDecimal.ZERO);
        supplier.setPaidAmount(BigDecimal.ZERO);
        supplier.setStatus(1);
        supplierMapper.insert(supplier);
        return supplier;
    }

    @Transactional
    public void addPaidAmount(Long supplierId, BigDecimal amount) {
        Supplier supplier = supplierMapper.selectById(supplierId);
        if (supplier == null) {
            throw BusinessException.of(400, "供应商不存在");
        }
        BigDecimal paid = supplier.getPaidAmount() != null ? supplier.getPaidAmount() : BigDecimal.ZERO;
        supplier.setPaidAmount(paid.add(amount));
        supplierMapper.updateById(supplier);
    }

    private Supplier getSupplierOrThrow(Long id) {
        Supplier supplier = supplierMapper.selectOne(new LambdaQueryWrapper<Supplier>()
                .eq(Supplier::getId, id)
                .eq(Supplier::getMerchantId, merchantContext.currentMerchantId()));
        if (supplier == null) {
            throw BusinessException.of(400, "供应商不存在");
        }
        return supplier;
    }

    private Supplier findByName(String name) {
        return supplierMapper.selectOne(new LambdaQueryWrapper<Supplier>()
                .eq(Supplier::getMerchantId, merchantContext.currentMerchantId())
                .eq(Supplier::getName, name)
                .last("LIMIT 1"));
    }

    private SupplierVO toVO(Supplier supplier) {
        BigDecimal payable = supplier.getPayableAmount() != null ? supplier.getPayableAmount() : BigDecimal.ZERO;
        BigDecimal paid = supplier.getPaidAmount() != null ? supplier.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal outstanding = payable.subtract(paid);
        if (outstanding.compareTo(BigDecimal.ZERO) < 0) {
            outstanding = BigDecimal.ZERO;
        }
        return SupplierVO.builder()
                .id(supplier.getId())
                .supplierNo(supplier.getSupplierNo())
                .name(supplier.getName())
                .contactName(supplier.getContactName())
                .phone(supplier.getPhone())
                .remark(supplier.getRemark())
                .status(supplier.getStatus())
                .createdAt(supplier.getCreatedAt())
                .payableAmount(payable)
                .paidAmount(paid)
                .outstandingPayable(outstanding)
                .build();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
