package com.cmict.internalpaas.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NavigationSectionDto {
    private String title;
    private List<NavigationItemDto> items;
}
