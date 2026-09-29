package org.example.linkhub.controller;

import jakarta.validation.Valid;
import org.example.linkhub.dto.CategoryCreateRequest;
import org.example.linkhub.dto.CategoryResponse;
import org.example.linkhub.service.CategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService){
        this.categoryService = categoryService;
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryCreateRequest request){
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.createCategory(request));
    }

    @GetMapping("/tree")
    public List<CategoryResponse> tree(){
        return categoryService.getTree();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id ){
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
