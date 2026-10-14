package com.rest.restaurantsystem.structure;

import com.rest.restaurantsystem.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BranchService {

    private final BranchRepository repository;

    public BranchService(BranchRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<BranchResponse> findAll() {
        return repository.findAllByOrderBySortOrderAscNameAsc()
                .stream()
                .map(BranchResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BranchResponse findById(Long id) {
        return BranchResponse.from(getEntity(id));
    }

    @Transactional
    public void updateLocalDetails(String name, String address, String phone, String timezone) {
        Branch branch = repository.findAll().stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el restaurante local."));
        branch.updateLocalDetails(name, address, phone, timezone);
    }

    Branch getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el restaurante local."));
    }
}
