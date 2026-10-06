package ma.ac.emi.studentmanagement.repository;

import ma.ac.emi.studentmanagement.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {
}
