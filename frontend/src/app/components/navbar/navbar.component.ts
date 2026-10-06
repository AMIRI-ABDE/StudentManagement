import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  template: `
    <nav class="navbar">
      <a class="navbar-brand" routerLink="/">Student Management</a>

      <div class="navbar-links">
        @if (auth.isLoggedIn()) {
          <a routerLink="/courses" routerLinkActive="active">Courses</a>
          <a routerLink="/my-courses" routerLinkActive="active">My Courses</a>
          <a routerLink="/profile" routerLinkActive="active">Profile</a>
          @if (auth.isAdmin()) {
            <a routerLink="/admin" routerLinkActive="active" class="admin-link">Admin</a>
          }
          <button class="btn-logout" (click)="auth.logout()">Logout</button>
        } @else {
          <a routerLink="/login" routerLinkActive="active">Login</a>
          <a routerLink="/register" routerLinkActive="active">Register</a>
        }
      </div>
    </nav>
  `,
  styles: [`
    .navbar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 1rem 2rem;
      background: #1a237e;
      color: white;
    }
    .navbar-brand {
      font-size: 1.25rem;
      font-weight: bold;
      color: white;
      text-decoration: none;
    }
    .navbar-links {
      display: flex;
      align-items: center;
      gap: 1rem;
    }
    .navbar-links a {
      color: rgba(255,255,255,0.8);
      text-decoration: none;
      padding: 0.5rem 0.75rem;
      border-radius: 4px;
      transition: background 0.2s;
    }
    .navbar-links a:hover, .navbar-links a.active {
      color: white;
      background: rgba(255,255,255,0.15);
    }
    .user-info {
      color: rgba(255,255,255,0.7);
      font-size: 0.9rem;
    }
    .btn-logout {
      background: rgba(255,255,255,0.15);
      color: white;
      border: 1px solid rgba(255,255,255,0.3);
      padding: 0.4rem 1rem;
      border-radius: 4px;
      cursor: pointer;
    }
    .btn-logout:hover {
      background: rgba(255,255,255,0.25);
    }
    .admin-link {
      color: #ffd54f !important;
      font-weight: 600;
    }
  `]
})
export class NavbarComponent {
  auth = inject(AuthService);
}
