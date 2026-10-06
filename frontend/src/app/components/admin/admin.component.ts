import { Component, inject, OnInit, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { CourseService } from '../../services/course.service';
import { Course } from '../../models/course.model';
import { environment } from '../../../environments/environment';

interface StudentInfo {
  id: number;
  name: string;
  email: string;
  role: string;
}

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="admin-container">
      <h2 class="page-title">Admin Panel</h2>

      <!-- Create Course Form -->
      <div class="card form-card">
        <h3>{{ editingCourse ? 'Edit Course' : 'Create New Course' }}</h3>
        <form (ngSubmit)="saveCourse()">
          <div class="form-group">
            <label for="title">Course Title</label>
            <input id="title" name="title" [(ngModel)]="courseForm.title"
                   placeholder="Enter course title" required />
          </div>
          <div class="form-group">
            <label for="maxCapacity">Max Capacity</label>
            <input id="maxCapacity" name="maxCapacity" type="number"
                   [(ngModel)]="courseForm.maxCapacity" placeholder="30" required min="1" />
          </div>
          <div class="form-actions">
            <button type="submit" class="btn-primary">
              {{ editingCourse ? 'Update' : 'Create' }}
            </button>
            @if (editingCourse) {
              <button type="button" class="btn-secondary" (click)="cancelEdit()">Cancel</button>
            }
          </div>
        </form>
        @if (formMessage()) {
          <p class="form-message" [class.error]="formError()">{{ formMessage() }}</p>
        }
      </div>

      <!-- Courses List -->
      <div class="card">
        <h3>All Courses</h3>
        @if (courseService.loading()) {
          <p>Loading...</p>
        } @else {
          <table class="data-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Title</th>
                <th>Capacity</th>
                <th>Enrolled</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              @for (course of courseService.courses(); track course.id) {
                <tr>
                  <td>{{ course.id }}</td>
                  <td>{{ course.title }}</td>
                  <td>{{ course.maxCapacity }}</td>
                  <td>{{ course.currentEnrollment }}</td>
                  <td>
                    <span class="badge" [class.badge-full]="course.full"
                          [class.badge-open]="!course.full">
                      {{ course.full ? 'Full' : 'Open' }}
                    </span>
                  </td>
                  <td class="actions">
                    <button class="btn-edit" (click)="startEdit(course)">Edit</button>
                    <button class="btn-delete" (click)="deleteCourse(course.id)">Delete</button>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        }
      </div>

      <!-- Students List -->
      <div class="card">
        <h3>All Students</h3>
        @if (loadingStudents()) {
          <p>Loading...</p>
        } @else {
          <table class="data-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Name</th>
                <th>Email</th>
                <th>Role</th>
              </tr>
            </thead>
            <tbody>
              @for (student of students(); track student.id) {
                <tr>
                  <td>{{ student.id }}</td>
                  <td>{{ student.name }}</td>
                  <td>{{ student.email }}</td>
                  <td>
                    <span class="badge" [class.badge-admin]="student.role === 'ADMIN'"
                          [class.badge-student]="student.role === 'STUDENT'">
                      {{ student.role }}
                    </span>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        }
      </div>
    </div>
  `,
  styles: [`
    .admin-container {
      max-width: 960px;
      margin: 2rem auto;
      padding: 0 1rem;
    }
    .page-title {
      color: #1a237e;
      margin-bottom: 1.5rem;
    }
    .card {
      background: white;
      border-radius: 8px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.1);
      padding: 1.5rem;
      margin-bottom: 1.5rem;
    }
    .card h3 {
      margin-top: 0;
      margin-bottom: 1rem;
      color: #333;
    }
    .form-card { max-width: 500px; }
    .form-group {
      margin-bottom: 1rem;
    }
    .form-group label {
      display: block;
      margin-bottom: 0.25rem;
      font-weight: 500;
      color: #555;
    }
    .form-group input {
      width: 100%;
      padding: 0.5rem 0.75rem;
      border: 1px solid #ddd;
      border-radius: 4px;
      font-size: 1rem;
      box-sizing: border-box;
    }
    .form-group input:focus {
      outline: none;
      border-color: #1a237e;
    }
    .form-actions {
      display: flex;
      gap: 0.5rem;
    }
    .btn-primary {
      background: #1a237e;
      color: white;
      border: none;
      padding: 0.5rem 1.5rem;
      border-radius: 4px;
      cursor: pointer;
      font-size: 1rem;
    }
    .btn-primary:hover { background: #283593; }
    .btn-secondary {
      background: #eee;
      color: #333;
      border: none;
      padding: 0.5rem 1.5rem;
      border-radius: 4px;
      cursor: pointer;
      font-size: 1rem;
    }
    .btn-secondary:hover { background: #ddd; }
    .form-message {
      margin-top: 0.75rem;
      color: #2e7d32;
      font-size: 0.9rem;
    }
    .form-message.error { color: #c62828; }
    .data-table {
      width: 100%;
      border-collapse: collapse;
    }
    .data-table th, .data-table td {
      padding: 0.75rem;
      text-align: left;
      border-bottom: 1px solid #eee;
    }
    .data-table th {
      background: #f5f5f5;
      font-weight: 600;
      color: #555;
    }
    .badge {
      display: inline-block;
      padding: 0.2rem 0.6rem;
      border-radius: 12px;
      font-size: 0.8rem;
      font-weight: 500;
    }
    .badge-open { background: #e8f5e9; color: #2e7d32; }
    .badge-full { background: #ffebee; color: #c62828; }
    .badge-admin { background: #e8eaf6; color: #1a237e; }
    .badge-student { background: #e0f7fa; color: #00695c; }
    .actions {
      display: flex;
      gap: 0.5rem;
    }
    .btn-edit {
      background: #ff8f00;
      color: white;
      border: none;
      padding: 0.3rem 0.75rem;
      border-radius: 4px;
      cursor: pointer;
      font-size: 0.85rem;
    }
    .btn-edit:hover { background: #ff6f00; }
    .btn-delete {
      background: #c62828;
      color: white;
      border: none;
      padding: 0.3rem 0.75rem;
      border-radius: 4px;
      cursor: pointer;
      font-size: 0.85rem;
    }
    .btn-delete:hover { background: #b71c1c; }
  `]
})
export class AdminComponent implements OnInit {
  courseService = inject(CourseService);
  private http = inject(HttpClient);
  private auth = inject(AuthService);

  students = signal<StudentInfo[]>([]);
  loadingStudents = signal(false);
  formMessage = signal('');
  formError = signal(false);

  courseForm = { title: '', maxCapacity: 30 };
  editingCourse: Course | null = null;

  ngOnInit(): void {
    this.courseService.loadCourses();
    this.loadStudents();
  }

  loadStudents(): void {
    this.loadingStudents.set(true);
    this.http.get<StudentInfo[]>(`${environment.apiUrl}/admin/students`).subscribe({
      next: (data) => {
        this.students.set(data);
        this.loadingStudents.set(false);
      },
      error: () => this.loadingStudents.set(false)
    });
  }

  saveCourse(): void {
    if (!this.courseForm.title.trim()) return;

    if (this.editingCourse) {
      this.courseService.updateCourse(this.editingCourse.id, this.courseForm).subscribe({
        next: () => {
          this.formMessage.set('Course updated successfully!');
          this.formError.set(false);
          this.cancelEdit();
        },
        error: () => {
          this.formMessage.set('Failed to update course.');
          this.formError.set(true);
        }
      });
    } else {
      this.courseService.createCourse(this.courseForm).subscribe({
        next: () => {
          this.formMessage.set('Course created successfully!');
          this.formError.set(false);
          this.courseForm = { title: '', maxCapacity: 30 };
        },
        error: () => {
          this.formMessage.set('Failed to create course.');
          this.formError.set(true);
        }
      });
    }
  }

  startEdit(course: Course): void {
    this.editingCourse = course;
    this.courseForm = { title: course.title, maxCapacity: course.maxCapacity };
    this.formMessage.set('');
  }

  cancelEdit(): void {
    this.editingCourse = null;
    this.courseForm = { title: '', maxCapacity: 30 };
  }

  deleteCourse(id: number): void {
    if (confirm('Are you sure you want to delete this course?')) {
      this.courseService.deleteCourse(id).subscribe({
        next: () => {
          this.formMessage.set('Course deleted.');
          this.formError.set(false);
        },
        error: () => {
          this.formMessage.set('Failed to delete course.');
          this.formError.set(true);
        }
      });
    }
  }
}
