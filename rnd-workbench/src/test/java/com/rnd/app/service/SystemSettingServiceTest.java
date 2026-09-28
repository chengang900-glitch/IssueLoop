package com.rnd.app.service;

import com.rnd.app.entity.SystemSetting;
import com.rnd.app.repository.SystemSettingRepository;
import com.rnd.app.util.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

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
}
