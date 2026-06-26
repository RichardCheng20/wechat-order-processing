package com.vwholesale.supplier.support;

import com.vwholesale.common.exception.BusinessException;
import com.vwholesale.supplier.mapper.SupplierIdSequenceMapper;
import com.vwholesale.supplier.mapper.SupplierMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 供应商业务编号：yyyymmdd + 3 位当日序号，如 20260625001。
 */
@Service
@RequiredArgsConstructor
public class SupplierNoGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.BASIC_ISO_DATE;
    private static final int MAX_DAILY_SEQ = 999;
    private static final int NO_LENGTH = 11;

    private final SupplierIdSequenceMapper sequenceMapper;
    private final SupplierMapper supplierMapper;

    @Transactional
    public String nextNo(Long merchantId) {
        return nextNo(merchantId, LocalDate.now());
    }

    @Transactional
    public String nextNo(Long merchantId, LocalDate date) {
        syncSequence(merchantId, date);
        sequenceMapper.allocate(merchantId, date);
        int seq = sequenceMapper.currentSeq();
        if (seq < 1 || seq > MAX_DAILY_SEQ) {
            throw BusinessException.of(400, "当日供应商编号已用尽（最多 " + MAX_DAILY_SEQ + " 个）");
        }
        return date.format(DATE_FMT) + String.format("%03d", seq);
    }

    private void syncSequence(Long merchantId, LocalDate date) {
        String prefix = date.format(DATE_FMT);
        Integer maxSeq = supplierMapper.selectMaxSeqForDate(merchantId, prefix);
        if (maxSeq != null && maxSeq > 0) {
            sequenceMapper.syncMin(merchantId, date, maxSeq);
        }
    }

    public static boolean matchesFormat(String supplierNo) {
        if (supplierNo == null || supplierNo.length() != NO_LENGTH) {
            return false;
        }
        if (!supplierNo.chars().allMatch(Character::isDigit)) {
            return false;
        }
        int seq = Integer.parseInt(supplierNo.substring(8));
        return seq >= 1 && seq <= MAX_DAILY_SEQ;
    }
}
