import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { Business } from '../../core/models/orderly.models';
import { LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, LucideAngularModule],
  templateUrl: './layout.component.html',
  styleUrl: './layout.component.css'
})
export class LayoutComponent implements OnInit {
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  businesses: Business[] = [];
  activeBusinessId: string | null = null;

  ngOnInit(): void {
    this.businesses = this.auth.getBusinesses();
    this.activeBusinessId = this.auth.getActiveBusinessId();
  }

  switchBusiness(id: string): void {
    this.auth.setActiveBusinessId(id);
    this.activeBusinessId = id;
    // reload current route to refresh data
    const url = this.router.url;
    this.router.navigateByUrl('/login', { skipLocationChange: true }).then(() =>
      this.router.navigate([url])
    );
  }

  logout(): void {
    this.auth.logout();
  }
}
