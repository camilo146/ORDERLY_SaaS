import { Component, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';

export interface OnboardingStep {
  id: number;
  title: string;
  subtitle: string;
  icon: string;
  completed: boolean;
  minuteEstimate: number;
}

@Component({
  selector: 'app-onboarding',
  standalone: true,
  imports: [CommonModule, RouterLink, LucideAngularModule],
  templateUrl: './onboarding.component.html',
  styleUrl: './onboarding.component.css'
})
export class OnboardingComponent {
  currentStep = signal(0);

  steps = signal<OnboardingStep[]>([
    {
      id: 0,
      title: 'Configura tu negocio',
      subtitle: 'Completa el nombre, tipo de negocio y moneda.',
      icon: 'building-2',
      completed: false,
      minuteEstimate: 0
    },
    {
      id: 1,
      title: 'Personaliza tu perfil',
      subtitle: 'Agrega tu logo y datos de contacto.',
      icon: 'circle-user',
      completed: false,
      minuteEstimate: 3
    },
    {
      id: 2,
      title: 'Crea tu catálogo',
      subtitle: 'Agrega al menos 3 productos con precio y descripción.',
      icon: 'package',
      completed: false,
      minuteEstimate: 10
    },
    {
      id: 3,
      title: 'Conecta WhatsApp',
      subtitle: 'Escanea el código QR con tu celular para activar el bot.',
      icon: 'smartphone',
      completed: false,
      minuteEstimate: 18
    },
    {
      id: 4,
      title: '¡Primer pedido demo!',
      subtitle: 'Recibe un pedido de prueba y confirma que todo funciona.',
      icon: 'shopping-cart',
      completed: false,
      minuteEstimate: 25
    },
    {
      id: 5,
      title: '¡Listo para producción!',
      subtitle: 'Tu negocio está activo. Comparte tu enlace con tus clientes.',
      icon: 'rocket',
      completed: false,
      minuteEstimate: 30
    }
  ]);

  progressPercent = computed(() => {
    const completed = this.steps().filter(s => s.completed).length;
    return Math.round((completed / this.steps().length) * 100);
  });

  activeStep = computed(() => this.steps()[this.currentStep()]);

  goTo(index: number): void {
    if (index >= 0 && index < this.steps().length) {
      this.currentStep.set(index);
    }
  }

  completeCurrentStep(): void {
    this.steps.update(steps => {
      const updated = [...steps];
      updated[this.currentStep()] = { ...updated[this.currentStep()], completed: true };
      return updated;
    });
    if (this.currentStep() < this.steps().length - 1) {
      this.currentStep.update(i => i + 1);
    }
  }

  getStepRoute(stepId: number): string {
    const routes: Record<number, string> = {
      0: '/settings',
      1: '/settings',
      2: '/products',
      3: '/settings',
      4: '/dashboard',
      5: '/dashboard'
    };
    return routes[stepId] ?? '/dashboard';
  }
}
