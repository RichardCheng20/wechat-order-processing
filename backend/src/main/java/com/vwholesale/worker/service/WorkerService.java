package com.vwholesale.worker.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vwholesale.common.context.MerchantContext;
import com.vwholesale.common.enums.UserRole;
import com.vwholesale.common.security.RoleChecker;
import com.vwholesale.common.exception.BusinessException;
import com.vwholesale.common.security.RoleChecker;
import com.vwholesale.user.entity.User;
import com.vwholesale.worker.dto.PersonnelCreateRequest;
import com.vwholesale.worker.dto.PersonnelUpdateRequest;
import com.vwholesale.worker.dto.WorkerVO;
import com.vwholesale.worker.entity.Worker;
import com.vwholesale.worker.mapper.WorkerMapper;
import com.vwholesale.worker.support.PersonnelJobRole;
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
        return listActiveWorkers().stream().map(this::toVO).toList();
    }

    public List<WorkerVO> listPersonnel() {
        RoleChecker.requireBoss();
        return listActiveWorkers().stream().map(this::toVO).toList();
    }

    @Transactional
    public WorkerVO createPersonnel(PersonnelCreateRequest request) {
        RoleChecker.requireBoss();
        assertValidJobRole(request.getJobRole());
        String name = request.getName().trim();
        String phone = normalizePhone(request.getPhone());
        assertPhoneAvailable(phone, null);

        Worker worker = new Worker();
        worker.setMerchantId(merchantContext.currentMerchantId());
        worker.setName(name);
        worker.setPhone(phone);
        worker.setJobRole(PersonnelJobRole.normalize(request.getJobRole()));
        worker.setStatus(1);
        insertWorkerWithCode(worker);
        return toVO(worker);
    }

    @Transactional
    public WorkerVO updatePersonnel(Long id, PersonnelUpdateRequest request) {
        RoleChecker.requireBoss();
        assertValidJobRole(request.getJobRole());
        Worker worker = getPersonnelOrThrow(id);
        String phone = normalizePhone(request.getPhone());
        assertPhoneAvailable(phone, worker.getId());

        worker.setName(request.getName().trim());
        worker.setPhone(phone);
        worker.setJobRole(PersonnelJobRole.normalize(request.getJobRole()));
        workerMapper.updateById(worker);
        return toVO(worker);
    }

    @Transactional
    public void disablePersonnel(Long id) {
        RoleChecker.requireBoss();
        Worker worker = getPersonnelOrThrow(id);
        worker.setStatus(0);
        workerMapper.updateById(worker);
    }

    private List<Worker> listActiveWorkers() {
        return workerMapper.selectList(new LambdaQueryWrapper<Worker>()
                .eq(Worker::getMerchantId, merchantContext.currentMerchantId())
                .eq(Worker::getStatus, 1)
                .orderByDesc(Worker::getId));
    }

    private Worker getPersonnelOrThrow(Long id) {
        Worker worker = workerMapper.selectOne(new LambdaQueryWrapper<Worker>()
                .eq(Worker::getId, id)
                .eq(Worker::getMerchantId, merchantContext.currentMerchantId()));
        if (worker == null || worker.getStatus() == null || worker.getStatus() == 0) {
            throw BusinessException.of(404, "人员不存在或已停用");
        }
        return worker;
    }

    private void assertValidJobRole(String jobRole) {
        if (!PersonnelJobRole.isValid(jobRole)) {
            throw BusinessException.of(400, "人员角色无效");
        }
    }

    private String normalizePhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            throw BusinessException.of(400, "请输入电话号码");
        }
        return phone.trim();
    }

    private void assertPhoneAvailable(String phone, Long excludeId) {
        LambdaQueryWrapper<Worker> query = new LambdaQueryWrapper<Worker>()
                .eq(Worker::getMerchantId, merchantContext.currentMerchantId())
                .eq(Worker::getPhone, phone)
                .eq(Worker::getStatus, 1);
        if (excludeId != null) {
            query.ne(Worker::getId, excludeId);
        }
        if (workerMapper.selectCount(query) > 0) {
            throw BusinessException.of(400, "该手机号已存在");
        }
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
            worker.setPhone(user.getPhone());
            worker.setJobRole(PersonnelJobRole.DELIVERY);
            worker.setStatus(1);
            insertWorkerWithCode(worker);
        } else {
            if (!StringUtils.hasText(worker.getWorkerCode())) {
                worker.setWorkerCode(formatWorkerCode(worker.getId()));
                workerMapper.updateById(worker);
            }
            if (StringUtils.hasText(user.getNickname()) && !user.getNickname().equals(worker.getName())) {
                worker.setName(user.getNickname());
                workerMapper.updateById(worker);
            }
        }
        return worker.getId();
    }

    public String resolveWorkerCode(Long workerId) {
        if (workerId == null) {
            return null;
        }
        Worker worker = workerMapper.selectOne(new LambdaQueryWrapper<Worker>()
                .eq(Worker::getId, workerId)
                .eq(Worker::getMerchantId, merchantContext.currentMerchantId()));
        return worker != null ? worker.getWorkerCode() : null;
    }

    public Worker getWorkerByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        return workerMapper.selectOne(new LambdaQueryWrapper<Worker>()
                .eq(Worker::getUserId, userId)
                .eq(Worker::getMerchantId, merchantContext.currentMerchantId()));
    }

    private static String formatWorkerCode(Long workerId) {
        return String.format("PS%06d", workerId);
    }

    private void insertWorkerWithCode(Worker worker) {
        worker.setWorkerCode(buildTempWorkerCode(worker));
        workerMapper.insert(worker);
        worker.setWorkerCode(formatWorkerCode(worker.getId()));
        workerMapper.updateById(worker);
    }

    private static String buildTempWorkerCode(Worker worker) {
        long seed = worker.getUserId() != null
                ? worker.getUserId()
                : System.currentTimeMillis();
        return String.format("TMP%08d", Math.floorMod(seed, 100_000_000L));
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
                .workerCode(worker.getWorkerCode())
                .name(worker.getName())
                .phone(worker.getPhone())
                .jobRole(worker.getJobRole())
                .jobRoleLabel(PersonnelJobRole.labelOf(worker.getJobRole()))
                .bound(worker.getUserId() != null)
                .status(worker.getStatus())
                .build();
    }
}
