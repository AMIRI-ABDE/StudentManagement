package ma.ac.emi.studentmanagement.controller;

import jakarta.validation.Valid;
import ma.ac.emi.studentmanagement.dto.ProfileUpdateRequest;
import ma.ac.emi.studentmanagement.dto.StudentDto;
import ma.ac.emi.studentmanagement.entity.Student;
import ma.ac.emi.studentmanagement.repository.StudentRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final StudentRepository studentRepo;

    public ProfileController(StudentRepository studentRepo) {
        this.studentRepo = studentRepo;
    }

    @GetMapping
    public StudentDto getProfile(@AuthenticationPrincipal Student student) {
        return new StudentDto(student.getId(), student.getName(), student.getEmail(), student.getRole().name());
    }

    @PutMapping
    public StudentDto updateProfile(@AuthenticationPrincipal Student student,
                                    @Valid @RequestBody ProfileUpdateRequest request) {
        student.setName(request.name());
        Student saved = studentRepo.save(student);
        return new StudentDto(saved.getId(), saved.getName(), saved.getEmail(), saved.getRole().name());
    }
}
