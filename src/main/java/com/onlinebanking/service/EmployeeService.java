package com.onlinebanking.service;

import com.onlinebanking.entity.*;
import com.onlinebanking.repository.*;
import com.onlinebanking.util.IdGeneratorUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeService {
    private final EmployeeRepository empRepo;
    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final BranchRepository branchRepo;
    private final PasswordEncoder encoder;

    public Employee addEmployee(String fullName, String email, String mobile,
                                String designation, Long branchId) {
        String username = email.split("@")[0].replaceAll("[^a-zA-Z0-9]", "");
        Role role = roleRepo.findByName("ROLE_EMPLOYEE").orElseThrow();
        User user = userRepo.save(User.builder()
            .username(username).email(email)
            .password(encoder.encode("Employee@123"))
            .enabled(true).accountNonLocked(true)
            .roles(Set.of(role)).build());
        Branch branch = branchId != null ? branchRepo.findById(branchId).orElse(null) : null;
        return empRepo.save(Employee.builder()
            .employeeId(IdGeneratorUtil.generateEmployeeId())
            .fullName(fullName).email(email).mobileNumber(mobile)
            .designation(designation).branch(branch).user(user).active(true).build());
    }

    public List<Employee> getAll() { return empRepo.findAll(); }
    public Employee getById(Long id) { return empRepo.findById(id).orElseThrow(); }
    public Employee getByUserId(Long id) { return empRepo.findByUser_Id(id).orElseThrow(); }
    public void delete(Long id) { empRepo.deleteById(id); }
    public void toggleActive(Long id) {
        Employee e = empRepo.findById(id).orElseThrow();
        e.setActive(!e.isActive()); empRepo.save(e);
    }
}
