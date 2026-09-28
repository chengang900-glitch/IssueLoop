package com.rnd.app.service;

import com.rnd.app.entity.SystemSetting;
import com.rnd.app.repository.SystemSettingRepository;
import com.rnd.app.util.BusinessException;
import com.rnd.app.util.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SystemSettingService {
    public static final String PROJECT_CODE_PREFIX = "project_code_prefix";
    private final SystemSettingRepository settingRepo;

    public String getProjectCodePrefix() {
        return settingRepo.findById(PROJECT_CODE_PREFIX).map(SystemSetting::getSettingValue).orElse("PRJ");
    }

    @Transactional
    public String getProjectCodePrefixForUpdate() {
        return settingRepo.findForUpdate(PROJECT_CODE_PREFIX).map(SystemSetting::getSettingValue).orElse("PRJ");
    }

    @Transactional
    public void updateProjectCodePrefix(String prefix) {
        String normalized = prefix == null ? "" : prefix.trim().toUpperCase();
        if (!normalized.matches("[A-Z0-9-]{1,16}")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "项目编号前缀仅支持 1-16 位大写字母、数字或连字符");
        }
        settingRepo.save(SystemSetting.builder().settingKey(PROJECT_CODE_PREFIX).settingValue(normalized).build());
    }
}
