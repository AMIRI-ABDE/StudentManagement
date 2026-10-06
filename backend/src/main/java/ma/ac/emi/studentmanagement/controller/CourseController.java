package ma.ac.emi.studentmanagement.controller;

import jakarta.validation.Valid;
import ma.ac.emi.studentmanagement.dto.CourseDto;
import ma.ac.emi.studentmanagement.entity.Course;
import ma.ac.emi.studentmanagement.repository.CourseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseRepository courseRepo;

    public CourseController(CourseRepository courseRepo) {
        this.courseRepo = courseRepo;
    }

    @GetMapping
    public List<CourseDto> getAllCourses() {
        return courseRepo.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @GetMapping("/{id}")
    public CourseDto getCourse(@PathVariable Long id) {
        Course course = courseRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        return toDto(course);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public CourseDto createCourse(@Valid @RequestBody Course course) {
        course.setCurrentEnrollment(0);
        Course saved = courseRepo.save(course);
        return toDto(saved);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CourseDto updateCourse(@PathVariable Long id, @Valid @RequestBody Course updated) {
        Course course = courseRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        course.setTitle(updated.getTitle());
        course.setMaxCapacity(updated.getMaxCapacity());
        Course saved = courseRepo.save(course);
        return toDto(saved);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteCourse(@PathVariable Long id) {
        if (!courseRepo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
        }
        courseRepo.deleteById(id);
    }

    private CourseDto toDto(Course c) {
        return new CourseDto(c.getId(), c.getTitle(), c.getMaxCapacity(), c.getCurrentEnrollment(), c.isFull());
    }
}
