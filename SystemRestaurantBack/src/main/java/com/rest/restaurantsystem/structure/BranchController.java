package com.rest.restaurantsystem.structure;

import com.rest.restaurantsystem.common.ActiveStatusRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/branches")
public class BranchController {

    private final BranchService service;
    private final BranchAssignmentService assignmentService;

    public BranchController(BranchService service, BranchAssignmentService assignmentService) {
        this.service = service;
        this.assignmentService = assignmentService;
    }

    @GetMapping
    public List<BranchResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public BranchResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BranchResponse create(@Valid @RequestBody BranchRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public BranchResponse update(@PathVariable Long id, @Valid @RequestBody BranchRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/active")
    public BranchResponse changeActive(@PathVariable Long id, @RequestBody ActiveStatusRequest request) {
        return service.changeActive(id, request.active());
    }

    @GetMapping("/{id}/assignments")
    public List<BranchAssignmentUserResponse> findAssignments(@PathVariable Long id) {
        return assignmentService.findAll(id);
    }

    @PutMapping("/{id}/assignments")
    public List<BranchAssignmentUserResponse> replaceAssignments(
            @PathVariable Long id,
            @Valid @RequestBody BranchAssignmentRequest request
    ) {
        return assignmentService.replace(id, request);
    }
}
