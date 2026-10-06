import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    <div class="auth-container">
      <div class="auth-card">
        <h2>Login</h2>

        @if (errorMsg()) {
          <div class="alert alert-error">{{ errorMsg() }}</div>
        }

        <form (ngSubmit)="onSubmit()">
          <div class="form-group">
            <label for="email">Email</label>
            <input id="email" type="email" [(ngModel)]="email" name="email"
                   placeholder="your@email.com" required />
          </div>

          <div class="form-group">
            <label for="password">Password</label>
            <input id="password" type="password" [(ngModel)]="password" name="password"
                   placeholder="••••••" required />
          </div>

          <button type="submit" class="btn-primary" [disabled]="loading()">
            @if (loading()) { Logging in... } @else { Login }
          </button>
        </form>

        <p class="auth-link">
          Don't have an account? <a routerLink="/register">Register here</a>
        </p>
      </div>
    </div>
  `,
  styles: [`
    .auth-container {
      display: flex; justify-content: center; align-items: center;
      min-height: calc(100vh - 64px); background: #f5f5f5;
    }
    .auth-card {
      background: white; padding: 2rem; border-radius: 8px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.1); width: 100%; max-width: 400px;
    }
    h2 { text-align: center; margin-bottom: 1.5rem; color: #1a237e; }
    .form-group { margin-bottom: 1rem; }
    .form-group label { display: block; margin-bottom: 0.25rem; font-weight: 500; }
    .form-group input {
      width: 100%; padding: 0.6rem; border: 1px solid #ddd;
      border-radius: 4px; font-size: 1rem; box-sizing: border-box;
    }
    .btn-primary {
      width: 100%; padding: 0.75rem; background: #1a237e;
      color: white; border: none; border-radius: 4px; font-size: 1rem;
      cursor: pointer; margin-top: 0.5rem;
    }
    .btn-primary:hover { background: #283593; }
    .btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }
    .alert-error {
      background: #ffebee; color: #c62828; padding: 0.75rem;
      border-radius: 4px; margin-bottom: 1rem;
    }
    .auth-link { text-align: center; margin-top: 1rem; }
    .auth-link a { color: #1a237e; }
  `]
})
export class LoginComponent {
  private authService = inject(AuthService);
  private router = inject(Router);

  email = '';
  password = '';
  loading = signal(false);
  errorMsg = signal('');

  onSubmit(): void {
    this.loading.set(true);
    this.errorMsg.set('');

    this.authService.login({ email: this.email, password: this.password }).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/courses']);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMsg.set(err.error?.error || 'Invalid credentials');
      }
    });
  }
}
