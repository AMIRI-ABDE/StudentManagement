package ma.ac.emi.studentmanagement.controller;

import ma.ac.emi.studentmanagement.dto.StudentDto;
import ma.ac.emi.studentmanagement.repository.StudentRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final StudentRepository studentRepo;

    public AdminController(StudentRepository studentRepo) {
        this.studentRepo = studentRepo;
    }

    @GetMapping("/students")
    public List<StudentDto> getAllStudents() {
        return studentRepo.findAll().stream()
                .map(s -> new StudentDto(s.getId(), s.getName(), s.getEmail(), s.getRole().name()))
                .toList();
    }
}
