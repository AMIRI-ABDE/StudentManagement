package ma.ac.emi.studentmanagement.service;

import ma.ac.emi.studentmanagement.dto.EnrollmentDto;
import ma.ac.emi.studentmanagement.entity.Course;
import ma.ac.emi.studentmanagement.entity.Enrollment;
import ma.ac.emi.studentmanagement.entity.Student;
import ma.ac.emi.studentmanagement.repository.CourseRepository;
import ma.ac.emi.studentmanagement.repository.EnrollmentRepository;
import ma.ac.emi.studentmanagement.repository.StudentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepo;
    private final StudentRepository studentRepo;
    private final CourseRepository courseRepo;

    public EnrollmentService(EnrollmentRepository enrollmentRepo,
                             StudentRepository studentRepo,
                             CourseRepository courseRepo) {
        this.enrollmentRepo = enrollmentRepo;
        this.studentRepo = studentRepo;
        this.courseRepo = courseRepo;
    }

    public List<EnrollmentDto> getStudentEnrollments(Long studentId) {
        return enrollmentRepo.findByStudentId(studentId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public EnrollmentDto enroll(Long studentId, Long courseId) {
        Student student = studentRepo.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        if (enrollmentRepo.existsByStudentIdAndCourseId(studentId, courseId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already enrolled in this course");
        }
        if (course.isFull()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Course is full");
        }

        course.setCurrentEnrollment(course.getCurrentEnrollment() + 1);
        courseRepo.save(course);

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment = enrollmentRepo.save(enrollment);

        return toDto(enrollment);
    }

    @Transactional
    public void cancel(Long studentId, Long courseId) {
        Enrollment enrollment = enrollmentRepo.findByStudentIdAndCourseId(studentId, courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Enrollment not found"));

        Course course = enrollment.getCourse();
        course.setCurrentEnrollment(Math.max(0, course.getCurrentEnrollment() - 1));
        courseRepo.save(course);

        enrollmentRepo.delete(enrollment);
    }

    private EnrollmentDto toDto(Enrollment e) {
        return new EnrollmentDto(
                e.getId(),
                e.getCourse().getId(),
                e.getCourse().getTitle(),
                e.getEnrolledAt()
        );
    }
}
