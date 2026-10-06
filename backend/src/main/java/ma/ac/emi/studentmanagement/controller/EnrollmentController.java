package ma.ac.emi.studentmanagement.controller;

import ma.ac.emi.studentmanagement.dto.EnrollmentDto;
import ma.ac.emi.studentmanagement.entity.Student;
import ma.ac.emi.studentmanagement.service.EnrollmentService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @GetMapping
    public List<EnrollmentDto> getMyEnrollments(@AuthenticationPrincipal Student student) {
        return enrollmentService.getStudentEnrollments(student.getId());
    }

    @PostMapping("/{courseId}")
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollmentDto enroll(@AuthenticationPrincipal Student student,
                                @PathVariable Long courseId) {
        return enrollmentService.enroll(student.getId(), courseId);
    }

    @DeleteMapping("/{courseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@AuthenticationPrincipal Student student,
                       @PathVariable Long courseId) {
        enrollmentService.cancel(student.getId(), courseId);
    }
}
