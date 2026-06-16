package com.vwholesale.worker.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vwholesale.common.context.MerchantContext;
import com.vwholesale.common.enums.UserRole;
import com.vwholesale.common.security.RoleChecker;
import com.vwholesale.user.entity.User;
import com.vwholesale.worker.dto.WorkerVO;
import com.vwholesale.worker.entity.Worker;
import com.vwholesale.worker.mapper.WorkerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkerService {

    private final WorkerMapper workerMapper;
    private final MerchantContext merchantContext;

    public List<WorkerVO> listForBoss() {
        RoleChecker.requireBoss();
        return workerMapper.selectList(new LambdaQueryWrapper<Worker>()
                        .eq(Worker::getMerchantId, merchantContext.currentMerchantId())
                        .eq(Worker::getStatus, 1)
                        .orderByAsc(Worker::getId))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Transactional
    public Long ensureWorkerProfile(User user) {
        if (!UserRole.WORKER.name().equals(user.getRole())) {
            return null;
        }
        Worker worker = workerMapper.selectOne(new LambdaQueryWrapper<Worker>()
                .eq(Worker::getUserId, user.getId())
                .eq(Worker::getMerchantId, user.getMerchantId()));
        if (worker == null) {
            worker = new Worker();
            worker.setMerchantId(user.getMerchantId());
            worker.setUserId(user.getId());
            worker.setName(StringUtils.hasText(user.getNickname()) ? user.getNickname() : "员工");
            worker.setStatus(1);
            workerMapper.insert(worker);
        } else if (StringUtils.hasText(user.getNickname()) && !user.getNickname().equals(worker.getName())) {
            worker.setName(user.getNickname());
            workerMapper.updateById(worker);
        }
        return worker.getId();
    }

    public Worker getWorkerOrThrow(Long workerId) {
        Worker worker = workerMapper.selectOne(new LambdaQueryWrapper<Worker>()
                .eq(Worker::getId, workerId)
                .eq(Worker::getMerchantId, merchantContext.currentMerchantId()));
        if (worker == null || worker.getStatus() == null || worker.getStatus() == 0) {
            throw com.vwholesale.common.exception.BusinessException.of(404, "员工不存在或已停用");
        }
        return worker;
    }

    private WorkerVO toVO(Worker worker) {
        return WorkerVO.builder()
                .id(worker.getId())
                .userId(worker.getUserId())
                .name(worker.getName())
                .phone(worker.getPhone())
                .status(worker.getStatus())
                .build();
    }
}
