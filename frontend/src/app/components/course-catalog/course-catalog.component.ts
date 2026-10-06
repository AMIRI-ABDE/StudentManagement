import { Component, inject, OnInit } from '@angular/core';
import { CourseService } from '../../services/course.service';
import { EnrollmentService } from '../../services/enrollment.service';

@Component({
  selector: 'app-course-catalog',
  standalone: true,
  template: `
    <div class="catalog-container">
      <h2>Course Catalog</h2>

      @if (courseService.loading()) {
        <p class="loading">Loading courses...</p>
      } @else {
        <div class="course-grid">
          @for (course of courseService.courses(); track course.id) {
            <div class="course-card">
              <h3>{{ course.title }}</h3>
              <div class="course-meta">
                <span>Capacity: {{ course.currentEnrollment }} / {{ course.maxCapacity }}</span>
                @if (course.full) {
                  <span class="badge badge-full">Full</span>
                } @else {
                  <span class="badge badge-open">Open</span>
                }
              </div>
              <div class="course-actions">
                @if (enrollmentService.isEnrolled(course.id)) {
                  <button class="btn-cancel" (click)="onCancel(course.id)">Cancel Enrollment</button>
                } @else if (!course.full) {
                  <button class="btn-enroll" (click)="onEnroll(course.id)">Enroll</button>
                } @else {
                  <button class="btn-disabled" disabled>Course Full</button>
                }
              </div>
            </div>
          } @empty {
            <p>No courses available.</p>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .catalog-container { padding: 2rem; max-width: 1000px; margin: 0 auto; }
    h2 { color: #1a237e; margin-bottom: 1.5rem; }
    .loading { color: #666; }
    .course-grid {
      display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 1rem;
    }
    .course-card {
      background: white; padding: 1.5rem; border-radius: 8px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.08); border: 1px solid #eee;
    }
    .course-card h3 { margin: 0 0 0.75rem; color: #333; }
    .course-meta {
      display: flex; justify-content: space-between; align-items: center;
      margin-bottom: 1rem; color: #666; font-size: 0.9rem;
    }
    .badge {
      padding: 0.2rem 0.6rem; border-radius: 12px; font-size: 0.8rem; font-weight: 500;
    }
    .badge-open { background: #e8f5e9; color: #2e7d32; }
    .badge-full { background: #ffebee; color: #c62828; }
    .course-actions { display: flex; gap: 0.5rem; }
    .btn-enroll {
      flex: 1; padding: 0.5rem; background: #1a237e; color: white;
      border: none; border-radius: 4px; cursor: pointer;
    }
    .btn-enroll:hover { background: #283593; }
    .btn-cancel {
      flex: 1; padding: 0.5rem; background: white; color: #c62828;
      border: 1px solid #c62828; border-radius: 4px; cursor: pointer;
    }
    .btn-cancel:hover { background: #ffebee; }
    .btn-disabled {
      flex: 1; padding: 0.5rem; background: #eee; color: #999;
      border: none; border-radius: 4px; cursor: not-allowed;
    }
  `]
})
export class CourseCatalogComponent implements OnInit {
  courseService = inject(CourseService);
  enrollmentService = inject(EnrollmentService);

  ngOnInit(): void {
    this.courseService.loadCourses();
    this.enrollmentService.loadEnrollments();
  }

  onEnroll(courseId: number): void {
    this.enrollmentService.enroll(courseId).subscribe({
      next: () => this.courseService.loadCourses()
    });
  }

  onCancel(courseId: number): void {
    this.enrollmentService.cancel(courseId).subscribe({
      next: () => this.courseService.loadCourses()
    });
  }
}
