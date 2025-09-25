package com.cmict.internalpaas.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NavigationItemDto {
    private String label;
    private String route;
    private String page;
    private String badge;
    private boolean active;
}
