import { Component, inject, OnInit } from '@angular/core';
import { DatePipe } from '@angular/common';
import { EnrollmentService } from '../../services/enrollment.service';
import { CourseService } from '../../services/course.service';

@Component({
  selector: 'app-my-courses',
  standalone: true,
  imports: [DatePipe],
  template: `
    <div class="my-courses-container">
      <h2>My Courses</h2>

      @if (enrollmentService.loading()) {
        <p class="loading">Loading enrollments...</p>
      } @else {
        @if (enrollmentService.enrollments().length === 0) {
          <div class="empty-state">
            <p>You are not enrolled in any courses yet.</p>
            <p>Visit the <a href="/courses">Course Catalog</a> to enroll.</p>
          </div>
        } @else {
          <div class="enrollments-list">
            @for (enrollment of enrollmentService.enrollments(); track enrollment.id) {
              <div class="enrollment-card">
                <div class="enrollment-info">
                  <h3>{{ enrollment.courseTitle }}</h3>
                  <p class="enrolled-date">Enrolled: {{ enrollment.enrolledAt | date:'mediumDate' }}</p>
                </div>
                <button class="btn-cancel" (click)="onCancel(enrollment.courseId)">
                  Cancel
                </button>
              </div>
            }
          </div>
        }
      }
    </div>
  `,
  styles: [`
    .my-courses-container { padding: 2rem; max-width: 800px; margin: 0 auto; }
    h2 { color: #1a237e; margin-bottom: 1.5rem; }
    .loading { color: #666; }
    .empty-state {
      text-align: center; padding: 3rem; background: white;
      border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.08);
    }
    .empty-state a { color: #1a237e; }
    .enrollments-list { display: flex; flex-direction: column; gap: 0.75rem; }
    .enrollment-card {
      display: flex; justify-content: space-between; align-items: center;
      background: white; padding: 1.25rem 1.5rem; border-radius: 8px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.08); border: 1px solid #eee;
    }
    .enrollment-info h3 { margin: 0 0 0.25rem; color: #333; }
    .enrolled-date { margin: 0; color: #666; font-size: 0.85rem; }
    .btn-cancel {
      padding: 0.5rem 1rem; background: white; color: #c62828;
      border: 1px solid #c62828; border-radius: 4px; cursor: pointer;
      white-space: nowrap;
    }
    .btn-cancel:hover { background: #ffebee; }
  `]
})
export class MyCoursesComponent implements OnInit {
  enrollmentService = inject(EnrollmentService);
  private courseService = inject(CourseService);

  ngOnInit(): void {
    this.enrollmentService.loadEnrollments();
  }

  onCancel(courseId: number): void {
    this.enrollmentService.cancel(courseId).subscribe({
      next: () => this.courseService.loadCourses()
    });
  }
}
