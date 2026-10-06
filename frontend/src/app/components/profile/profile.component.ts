import { Component, inject, signal, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ProfileService } from '../../services/profile.service';
import { Student } from '../../models/student.model';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="profile-container">
      <div class="profile-card">
        <h2>My Profile</h2>

        @if (successMsg()) {
          <div class="alert alert-success">{{ successMsg() }}</div>
        }
        @if (errorMsg()) {
          <div class="alert alert-error">{{ errorMsg() }}</div>
        }

        @if (profile(); as p) {
          <div class="profile-info">
            <p><strong>Email:</strong> {{ p.email }}</p>
            <p><strong>Role:</strong> {{ p.role }}</p>
          </div>

          <form (ngSubmit)="onUpdate()">
            <div class="form-group">
              <label for="name">Name</label>
              <input id="name" type="text" [(ngModel)]="editName" name="name" required />
            </div>
            <button type="submit" class="btn-primary" [disabled]="saving()">
              @if (saving()) { Saving... } @else { Update Profile }
            </button>
          </form>
        } @else {
          <p>Loading profile...</p>
        }
      </div>
    </div>
  `,
  styles: [`
    .profile-container {
      display: flex; justify-content: center; padding: 2rem; background: #f5f5f5;
      min-height: calc(100vh - 64px);
    }
    .profile-card {
      background: white; padding: 2rem; border-radius: 8px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.1); width: 100%; max-width: 500px;
      align-self: flex-start;
    }
    h2 { color: #1a237e; margin-bottom: 1.5rem; }
    .profile-info { margin-bottom: 1.5rem; }
    .profile-info p { margin: 0.5rem 0; }
    .form-group { margin-bottom: 1rem; }
    .form-group label { display: block; margin-bottom: 0.25rem; font-weight: 500; }
    .form-group input {
      width: 100%; padding: 0.6rem; border: 1px solid #ddd;
      border-radius: 4px; font-size: 1rem; box-sizing: border-box;
    }
    .btn-primary {
      padding: 0.75rem 1.5rem; background: #1a237e;
      color: white; border: none; border-radius: 4px; cursor: pointer;
    }
    .btn-primary:hover { background: #283593; }
    .btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }
    .alert-success {
      background: #e8f5e9; color: #2e7d32; padding: 0.75rem;
      border-radius: 4px; margin-bottom: 1rem;
    }
    .alert-error {
      background: #ffebee; color: #c62828; padding: 0.75rem;
      border-radius: 4px; margin-bottom: 1rem;
    }
  `]
})
export class ProfileComponent implements OnInit {
  private profileService = inject(ProfileService);

  profile = signal<Student | null>(null);
  editName = '';
  saving = signal(false);
  successMsg = signal('');
  errorMsg = signal('');

  ngOnInit(): void {
    this.profileService.getProfile().subscribe({
      next: (p) => {
        this.profile.set(p);
        this.editName = p.name;
      }
    });
  }

  onUpdate(): void {
    this.saving.set(true);
    this.successMsg.set('');
    this.errorMsg.set('');

    this.profileService.updateProfile({ name: this.editName }).subscribe({
      next: (p) => {
        this.profile.set(p);
        this.saving.set(false);
        this.successMsg.set('Profile updated successfully!');
      },
      error: () => {
        this.saving.set(false);
        this.errorMsg.set('Failed to update profile');
      }
    });
  }
}
