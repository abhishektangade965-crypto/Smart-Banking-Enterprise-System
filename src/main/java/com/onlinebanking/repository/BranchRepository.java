package com.onlinebanking.repository;

import com.onlinebanking.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BranchRepository extends JpaRepository<Branch, Long> {
    Optional<Branch> findByBranchId(String branchId);
    Optional<Branch> findByIfscCode(String ifscCode);
    List<Branch> findByActive(boolean active);
}
