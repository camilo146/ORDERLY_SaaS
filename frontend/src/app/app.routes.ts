import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  // ─── Public marketing site ───────────────────────────────────
  {
    path: '',
    loadComponent: () =>
      import('./features/public/layout/public-layout.component').then(m => m.PublicLayoutComponent),
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./features/public/home/home.component').then(m => m.HomeComponent),
        pathMatch: 'full'
      },
      {
        path: 'features',
        loadComponent: () =>
          import('./features/public/features-page/features-page.component').then(m => m.FeaturesPageComponent)
      },
      {
        path: 'pricing',
        loadComponent: () =>
          import('./features/public/pricing/pricing.component').then(m => m.PricingComponent)
      },
      {
        path: 'faq',
        loadComponent: () =>
          import('./features/public/faq/faq.component').then(m => m.FaqComponent)
      },
      {
        path: 'contact',
        loadComponent: () =>
          import('./features/public/contact/contact.component').then(m => m.ContactComponent)
      }
    ]
  },
  // ─── Auth pages ──────────────────────────────────────────────
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./features/auth/register/register.component').then(m => m.RegisterComponent)
  },
  {
    path: 'verify-email',
    loadComponent: () =>
      import('./features/auth/verify-email/verify-email.component').then(m => m.VerifyEmailComponent)
  },
  {
    path: 'forgot-password',
    loadComponent: () =>
      import('./features/auth/forgot-password/forgot-password.component').then(m => m.ForgotPasswordComponent)
  },
  {
    path: 'reset-password',
    loadComponent: () =>
      import('./features/auth/reset-password/reset-password.component').then(m => m.ResetPasswordComponent)
  },
  // ─── App (protected) ─────────────────────────────────────────
  {
    path: '',
    loadComponent: () =>
      import('./shared/layout/layout.component').then(m => m.LayoutComponent),
    canActivate: [authGuard],
    children: [
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent)
      },
      {
        path: 'dashboard/orders/:id',
        loadComponent: () =>
          import('./features/dashboard/order-detail/order-detail.component')
            .then(m => m.OrderDetailComponent)
      },
      {
        path: 'admin',
        loadComponent: () =>
          import('./features/super-admin/super-admin.component').then(m => m.SuperAdminComponent),
        canActivate: [roleGuard],
        data: { roles: ['SUPER_ADMIN'] }
      },
      {
        path: 'products',
        loadComponent: () =>
          import('./features/products/products.component').then(m => m.ProductsComponent),
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'SUPER_ADMIN'] }
      },
      {
        path: 'operator',
        loadComponent: () =>
          import('./features/operator/operator.component').then(m => m.OperatorComponent)
      },
      {
        path: 'onboarding',
        loadComponent: () =>
          import('./features/onboarding/onboarding.component').then(m => m.OnboardingComponent)
      },
      {
        path: 'settings',
        loadComponent: () =>
          import('./features/settings/settings.component').then(m => m.SettingsComponent)
      },
      {
        path: 'complaints',
        loadComponent: () =>
          import('./features/complaints/complaints.component').then(m => m.ComplaintsComponent),
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'SUPER_ADMIN'] }
      },
      {
        path: 'billing',
        loadComponent: () =>
          import('./features/billing/billing.component').then(m => m.BillingComponent),
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'SUPER_ADMIN'] }
      }
    ]
  },
  { path: '**', redirectTo: '' }
];
