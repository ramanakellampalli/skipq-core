package com.skipq.core.config;

import com.skipq.core.menu.MenuCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VendorImageService {

    private static final int IMAGE_COUNT = 5;

    private static final Map<String, String> KEYWORD_TO_TAG = Map.ofEntries(
            Map.entry("breakfast", "breakfast"),
            Map.entry("morning", "breakfast"),
            Map.entry("tiffin", "breakfast"),
            Map.entry("idli", "breakfast"),
            Map.entry("dosa", "breakfast"),
            Map.entry("chicken", "non-veg"),
            Map.entry("mutton", "non-veg"),
            Map.entry("fish", "non-veg"),
            Map.entry("egg", "non-veg"),
            Map.entry("non-veg", "non-veg"),
            Map.entry("nonveg", "non-veg"),
            Map.entry("meat", "non-veg"),
            Map.entry("paneer", "veg"),
            Map.entry("veg", "veg"),
            Map.entry("dal", "veg"),
            Map.entry("sabzi", "veg"),
            Map.entry("rice", "rice"),
            Map.entry("biryani", "rice"),
            Map.entry("pulao", "rice"),
            Map.entry("fried rice", "rice"),
            Map.entry("juice", "beverages"),
            Map.entry("coffee", "beverages"),
            Map.entry("tea", "beverages"),
            Map.entry("lassi", "beverages"),
            Map.entry("shake", "beverages"),
            Map.entry("drinks", "beverages"),
            Map.entry("beverage", "beverages"),
            Map.entry("snack", "snacks"),
            Map.entry("starter", "snacks"),
            Map.entry("chaat", "snacks"),
            Map.entry("chat", "snacks"),
            Map.entry("pakora", "snacks"),
            Map.entry("samosa", "snacks")
    );

    private final MenuCategoryRepository categoryRepository;
    private final R2ImageService r2ImageService;

    public List<String> getImagesForVendor(UUID vendorId) {
        List<String> categoryNames = categoryRepository
                .findAllByVendorIdOrdered(vendorId)
                .stream()
                .map(c -> c.getName().toLowerCase())
                .toList();

        List<String> tags = resolveTags(categoryNames);

        if (tags.isEmpty()) {
            tags = List.of("veg", "snacks");
        }

        return r2ImageService.getRandomImages(tags, IMAGE_COUNT);
    }

    private List<String> resolveTags(List<String> categoryNames) {
        List<String> tags = new ArrayList<>();
        for (String name : categoryNames) {
            for (Map.Entry<String, String> entry : KEYWORD_TO_TAG.entrySet()) {
                if (name.contains(entry.getKey()) && !tags.contains(entry.getValue())) {
                    tags.add(entry.getValue());
                }
            }
        }
        return tags;
    }
}
