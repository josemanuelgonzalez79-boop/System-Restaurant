package com.rest.restaurantsystem.structure;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/branches")
public class BranchController {

    private final BranchService service;

    public BranchController(BranchService service) {
        this.service = service;
    }

    @GetMapping
    public List<BranchResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public BranchResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }
}
