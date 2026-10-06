import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Student } from '../models/student.model';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ProfileService {

  private readonly apiUrl = `${environment.apiUrl}/profile`;

  constructor(private http: HttpClient) {}

  getProfile(): Observable<Student> {
    return this.http.get<Student>(this.apiUrl);
  }

  updateProfile(data: { name: string }): Observable<Student> {
    return this.http.put<Student>(this.apiUrl, data);
  }
}
