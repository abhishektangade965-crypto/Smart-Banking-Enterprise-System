package com.onlinebanking.service;

import com.onlinebanking.entity.Branch;
import com.onlinebanking.repository.BranchRepository;
import com.onlinebanking.util.IdGeneratorUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BranchService {
    private final BranchRepository branchRepo;

    public Branch add(String name, String ifsc, String address, String city, String state, String manager) {
        return branchRepo.save(Branch.builder()
            .branchId(IdGeneratorUtil.generateBranchId())
            .branchName(name).ifscCode(ifsc)
            .address(address).city(city).state(state)
            .manager(manager).active(true).build());
    }
    public List<Branch> getAll() { return branchRepo.findAll(); }
    public Branch getById(Long id) { return branchRepo.findById(id).orElseThrow(); }
    public Branch save(Branch branch) { return branchRepo.save(branch); }
    public void delete(Long id) { branchRepo.deleteById(id); }
}
