package com.vwholesale.merchant.service;

import com.vwholesale.common.context.MerchantContext;
import com.vwholesale.common.exception.BusinessException;
import com.vwholesale.common.security.RoleChecker;
import com.vwholesale.merchant.dto.BossProfileUpdateRequest;
import com.vwholesale.merchant.dto.BossProfileVO;
import com.vwholesale.merchant.entity.Merchant;
import com.vwholesale.merchant.mapper.MerchantMapper;
import com.vwholesale.user.entity.User;
import com.vwholesale.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class BossProfileService {

    private final MerchantMapper merchantMapper;
    private final UserMapper userMapper;
    private final MerchantContext merchantContext;

    public BossProfileVO getProfile() {
        RoleChecker.requireBoss();
        Merchant merchant = getMerchantOrThrow();
        User user = getCurrentUserOrThrow();
        return BossProfileVO.builder()
                .merchantName(merchant.getName())
                .region(merchant.getRegion())
                .contactName(StringUtils.hasText(user.getNickname()) ? user.getNickname() : merchant.getContactName())
                .phone(StringUtils.hasText(user.getPhone()) ? user.getPhone() : merchant.getPhone())
                .build();
    }

    @Transactional
    public BossProfileVO updateProfile(BossProfileUpdateRequest request) {
        RoleChecker.requireBoss();
        Merchant merchant = getMerchantOrThrow();
        User user = getCurrentUserOrThrow();

        merchant.setName(request.getMerchantName().trim());
        merchant.setRegion(StringUtils.hasText(request.getRegion()) ? request.getRegion().trim() : null);
        merchant.setContactName(request.getContactName().trim());
        merchant.setPhone(StringUtils.hasText(request.getPhone()) ? request.getPhone().trim() : null);
        merchantMapper.updateById(merchant);

        user.setNickname(request.getContactName().trim());
        user.setPhone(StringUtils.hasText(request.getPhone()) ? request.getPhone().trim() : null);
        userMapper.updateById(user);

        return getProfile();
    }

    private Merchant getMerchantOrThrow() {
        Merchant merchant = merchantMapper.selectById(merchantContext.currentMerchantId());
        if (merchant == null) {
            throw BusinessException.of(404, "商户不存在");
        }
        return merchant;
    }

    private User getCurrentUserOrThrow() {
        User user = userMapper.selectById(RoleChecker.currentUserId());
        if (user == null) {
            throw BusinessException.of(404, "用户不存在");
        }
        return user;
    }
}
