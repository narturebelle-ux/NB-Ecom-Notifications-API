package com.narturebelle.mails.dto;

import jakarta.validation.constraints.NotNull;

public class UpdateNotificationPreferenceRequest {

    @NotNull
    private Boolean enabled;

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
