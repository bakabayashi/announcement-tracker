import { Component, inject } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-login',
  imports: [ButtonModule, CardModule],
  template: `
    <div class="login">
      <p-card>
        <div class="content">
          <i class="pi pi-map-marker logo"></i>
          <h1>Announcement Tracker</h1>
          <p class="muted">Śledzenie ogłoszeń nieruchomości i samochodów.</p>
          <p-button
            label="Zaloguj przez Google"
            icon="pi pi-google"
            (onClick)="auth.loginWithGoogle()"
          />
        </div>
      </p-card>
    </div>
  `,
  styles: [
    `
      .login {
        min-height: 100vh;
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 1rem;
      }
      .content {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 0.75rem;
        text-align: center;
        padding: 1rem 2rem;
      }
      .logo {
        font-size: 2.5rem;
        color: var(--p-primary-color, #3b82f6);
      }
      h1 {
        margin: 0;
        font-size: 1.5rem;
      }
    `,
  ],
})
export class LoginComponent {
  protected auth = inject(AuthService);
}
