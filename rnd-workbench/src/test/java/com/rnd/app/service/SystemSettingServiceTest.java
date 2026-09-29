package com.rnd.app.service;

import com.rnd.app.entity.SystemSetting;
import com.rnd.app.dto.ThirdPartyLoginSettingsDto;
import com.rnd.app.repository.SystemSettingRepository;
import com.rnd.app.util.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemSettingServiceTest {
    @Mock SystemSettingRepository settingRepo;
    @InjectMocks SystemSettingService service;

    @Test void prefixIsNormalizedBeforeSaving() {
        service.updateProjectCodePrefix(" dev ");
        ArgumentCaptor<SystemSetting> captor = ArgumentCaptor.forClass(SystemSetting.class);
        verify(settingRepo).save(captor.capture());
        assertEquals("DEV", captor.getValue().getSettingValue());
    }

    @Test void invalidPrefixIsRejected() {
        assertThrows(BusinessException.class, () -> service.updateProjectCodePrefix("研发项目"));
    }

    @Test void thirdPartyLoginDefaultsToDisabledWhenSettingsAreMissing() {
        ThirdPartyLoginSettingsDto settings = service.getThirdPartyLoginSettings();
        assertFalse(settings.isEnabled());
        assertFalse(settings.isFeishuEnabled());
        assertFalse(settings.isDingtalkEnabled());
        assertFalse(settings.isWecomEnabled());
    }

    @Test void thirdPartyLoginRequiresOneProviderWhenEnabled() {
        ThirdPartyLoginSettingsDto settings = new ThirdPartyLoginSettingsDto();
        settings.setEnabled(true);
        assertThrows(BusinessException.class, () -> service.updateThirdPartyLoginSettings(settings));
    }

    @Test void thirdPartyLoginSavesSelectedProviders() {
        ThirdPartyLoginSettingsDto settings = new ThirdPartyLoginSettingsDto();
        settings.setEnabled(true);
        settings.setFeishuEnabled(true);
        settings.setDingtalkEnabled(false);
        settings.setWecomEnabled(true);
        service.updateThirdPartyLoginSettings(settings);
        verify(settingRepo, org.mockito.Mockito.times(4)).save(any(SystemSetting.class));
    }

    @Test void disabledMasterHidesProvidersFromPublicConfiguration() {
        when(settingRepo.findById(SystemSettingService.THIRD_PARTY_LOGIN_ENABLED))
                .thenReturn(java.util.Optional.of(SystemSetting.builder().settingKey(SystemSettingService.THIRD_PARTY_LOGIN_ENABLED).settingValue("false").build()));
        when(settingRepo.findById(SystemSettingService.THIRD_PARTY_LOGIN_FEISHU_ENABLED))
                .thenReturn(java.util.Optional.of(SystemSetting.builder().settingKey(SystemSettingService.THIRD_PARTY_LOGIN_FEISHU_ENABLED).settingValue("true").build()));
        ThirdPartyLoginSettingsDto settings = service.getEnabledThirdPartyLoginSettings();
        assertFalse(settings.isFeishuEnabled());
    }
}
