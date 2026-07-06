package com.onlinebanking.repository;

import com.onlinebanking.entity.Branch;
import com.onlinebanking.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByEmployeeId(String employeeId);
    Optional<Employee> findByEmail(String email);
    Optional<Employee> findByUser_Id(Long userId);
    List<Employee> findByBranch(Branch branch);
    List<Employee> findByActive(boolean active);
}
