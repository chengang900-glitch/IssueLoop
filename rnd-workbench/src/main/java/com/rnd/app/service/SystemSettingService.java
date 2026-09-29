package com.rnd.app.service;

import com.rnd.app.entity.SystemSetting;
import com.rnd.app.dto.ThirdPartyLoginSettingsDto;
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
    public static final String THIRD_PARTY_LOGIN_ENABLED = "third_party_login_enabled";
    public static final String THIRD_PARTY_LOGIN_FEISHU_ENABLED = "third_party_login_feishu_enabled";
    public static final String THIRD_PARTY_LOGIN_DINGTALK_ENABLED = "third_party_login_dingtalk_enabled";
    public static final String THIRD_PARTY_LOGIN_WECOM_ENABLED = "third_party_login_wecom_enabled";
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

    public ThirdPartyLoginSettingsDto getThirdPartyLoginSettings() {
        ThirdPartyLoginSettingsDto dto = new ThirdPartyLoginSettingsDto();
        dto.setEnabled(readBoolean(THIRD_PARTY_LOGIN_ENABLED));
        dto.setFeishuEnabled(readBoolean(THIRD_PARTY_LOGIN_FEISHU_ENABLED));
        dto.setDingtalkEnabled(readBoolean(THIRD_PARTY_LOGIN_DINGTALK_ENABLED));
        dto.setWecomEnabled(readBoolean(THIRD_PARTY_LOGIN_WECOM_ENABLED));
        return dto;
    }

    public ThirdPartyLoginSettingsDto getEnabledThirdPartyLoginSettings() {
        ThirdPartyLoginSettingsDto dto = getThirdPartyLoginSettings();
        if (!dto.isEnabled()) {
            dto.setFeishuEnabled(false);
            dto.setDingtalkEnabled(false);
            dto.setWecomEnabled(false);
        }
        return dto;
    }

    @Transactional
    public void updateThirdPartyLoginSettings(ThirdPartyLoginSettingsDto request) {
        if (request == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "第三方登录参数不能为空");
        boolean hasProvider = request.isFeishuEnabled() || request.isDingtalkEnabled() || request.isWecomEnabled();
        if (request.isEnabled() && !hasProvider) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "启用第三方协同 APP 登录时，至少选择一个平台");
        }
        saveBoolean(THIRD_PARTY_LOGIN_ENABLED, request.isEnabled());
        saveBoolean(THIRD_PARTY_LOGIN_FEISHU_ENABLED, request.isFeishuEnabled());
        saveBoolean(THIRD_PARTY_LOGIN_DINGTALK_ENABLED, request.isDingtalkEnabled());
        saveBoolean(THIRD_PARTY_LOGIN_WECOM_ENABLED, request.isWecomEnabled());
    }

    private boolean readBoolean(String key) {
        return settingRepo.findById(key).map(SystemSetting::getSettingValue)
                .map(value -> "true".equalsIgnoreCase(value)).orElse(false);
    }

    private void saveBoolean(String key, boolean value) {
        settingRepo.save(SystemSetting.builder().settingKey(key).settingValue(Boolean.toString(value)).build());
    }
}
