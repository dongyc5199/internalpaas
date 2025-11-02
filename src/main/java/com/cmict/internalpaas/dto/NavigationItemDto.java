package com.cmict.internalpaas.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
    private List<NavigationSubItemDto> submenu; // 子菜单项

    /**
     * 判断是否有子菜单
     */
    public boolean hasSubmenu() {
        return submenu != null && !submenu.isEmpty();
    }
}
