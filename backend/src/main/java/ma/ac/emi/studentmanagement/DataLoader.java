package ma.ac.emi.studentmanagement;

import ma.ac.emi.studentmanagement.entity.Course;
import ma.ac.emi.studentmanagement.entity.Role;
import ma.ac.emi.studentmanagement.entity.Student;
import ma.ac.emi.studentmanagement.repository.CourseRepository;
import ma.ac.emi.studentmanagement.repository.StudentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    private final CourseRepository courseRepo;
    private final StudentRepository studentRepo;
    private final PasswordEncoder passwordEncoder;

    public DataLoader(CourseRepository courseRepo,
                      StudentRepository studentRepo,
                      PasswordEncoder passwordEncoder) {
        this.courseRepo = courseRepo;
        this.studentRepo = studentRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Seed courses
        if (courseRepo.count() == 0) {
            courseRepo.save(createCourse("Mathematics", 30));
            courseRepo.save(createCourse("Physics", 25));
            courseRepo.save(createCourse("Computer Science", 35));
            courseRepo.save(createCourse("English Literature", 20));
            courseRepo.save(createCourse("Chemistry", 28));
        }

        // Seed admin user
        if (!studentRepo.existsByEmail("admin@emi.ac.ma")) {
            Student admin = new Student();
            admin.setName("Admin");
            admin.setEmail("admin@emi.ac.ma");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(Role.ADMIN);
            studentRepo.save(admin);
        }

        // Seed demo student
        if (!studentRepo.existsByEmail("student@emi.ac.ma")) {
            Student student = new Student();
            student.setName("Demo Student");
            student.setEmail("student@emi.ac.ma");
            student.setPassword(passwordEncoder.encode("student123"));
            student.setRole(Role.STUDENT);
            studentRepo.save(student);
        }
    }

    private Course createCourse(String title, int maxCapacity) {
        Course course = new Course();
        course.setTitle(title);
        course.setMaxCapacity(maxCapacity);
        course.setCurrentEnrollment(0);
        return course;
    }
}
