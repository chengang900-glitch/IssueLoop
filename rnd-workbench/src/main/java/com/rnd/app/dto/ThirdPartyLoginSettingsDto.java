package com.rnd.app.dto;

import lombok.Data;

@Data
public class ThirdPartyLoginSettingsDto {
    private boolean enabled;
    private boolean feishuEnabled;
    private boolean dingtalkEnabled;
    private boolean wecomEnabled;
}
