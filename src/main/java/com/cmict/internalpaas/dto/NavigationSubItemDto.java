package com.cmict.internalpaas.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NavigationSubItemDto {
    private String label;
    private String route;
    private String icon;
    private boolean active;
}
