import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { Enrollment } from '../models/enrollment.model';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class EnrollmentService {

  private readonly apiUrl = `${environment.apiUrl}/enrollments`;

  /** Signal-based state */
  enrollments = signal<Enrollment[]>([]);
  loading = signal(false);

  constructor(private http: HttpClient) {}

  loadEnrollments(): void {
    this.loading.set(true);
    this.http.get<Enrollment[]>(this.apiUrl).subscribe({
      next: (data) => {
        this.enrollments.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  enroll(courseId: number): Observable<Enrollment> {
    return this.http.post<Enrollment>(`${this.apiUrl}/${courseId}`, {}).pipe(
      tap(() => this.loadEnrollments())
    );
  }

  cancel(courseId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${courseId}`).pipe(
      tap(() => this.loadEnrollments())
    );
  }

  isEnrolled(courseId: number): boolean {
    return this.enrollments().some(e => e.courseId === courseId);
  }
}
