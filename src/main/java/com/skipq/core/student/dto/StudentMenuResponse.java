package com.skipq.core.student.dto;

import com.skipq.core.menu.dto.MenuItemResponse;

import java.util.List;

public record StudentMenuResponse(
        List<MenuItemResponse> items
) {}
