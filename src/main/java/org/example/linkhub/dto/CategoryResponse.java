package org.example.linkhub.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CategoryResponse {
    private Long id;
    private String name;
    private String description;
    private List<CategoryResponse> children = new ArrayList<>();
}
