package com.vwholesale.customer.support;

import com.vwholesale.common.exception.BusinessException;
import com.vwholesale.customer.mapper.CustomerIdSequenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 客户业务编号：yyyymmdd + 3 位当日序号，如 20260620001。
 * 写入 customers.customer_no；customers.id 仍为数据库自增主键。
 */
@Service
@RequiredArgsConstructor
public class CustomerNoGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.BASIC_ISO_DATE;
    private static final int MAX_DAILY_SEQ = 999;
    private static final int NO_LENGTH = 11;

    private final CustomerIdSequenceMapper sequenceMapper;

    @Transactional
    public String nextNo(Long merchantId) {
        return nextNo(merchantId, LocalDate.now());
    }

    @Transactional
    public String nextNo(Long merchantId, LocalDate date) {
        sequenceMapper.allocate(merchantId, date);
        int seq = sequenceMapper.currentSeq();
        if (seq < 1 || seq > MAX_DAILY_SEQ) {
            throw BusinessException.of(400, "当日客户编号已用尽（最多 " + MAX_DAILY_SEQ + " 个）");
        }
        return date.format(DATE_FMT) + String.format("%03d", seq);
    }

    public static boolean matchesFormat(String customerNo) {
        if (customerNo == null || customerNo.length() != NO_LENGTH) {
            return false;
        }
        if (!customerNo.chars().allMatch(Character::isDigit)) {
            return false;
        }
        int seq = Integer.parseInt(customerNo.substring(8));
        return seq >= 1 && seq <= MAX_DAILY_SEQ;
    }
}
