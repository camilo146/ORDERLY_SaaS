import { Component, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-contact',
  standalone: true,
  imports: [RouterLink, LucideAngularModule, FormsModule],
  templateUrl: './contact.component.html',
  styleUrl: './contact.component.css'
})
export class ContactComponent {
  name = signal('');
  email = signal('');
  business = signal('');
  message = signal('');
  sending = signal(false);
  sent = signal(false);

  isValid() {
    return this.name().trim() && this.email().trim() && this.message().trim();
  }

  async submit() {
    if (!this.isValid() || this.sending()) return;
    this.sending.set(true);
    await new Promise(r => setTimeout(r, 1200));
    this.sending.set(false);
    this.sent.set(true);
  }

  channels = [
    {
      icon: 'mail',
      title: 'Email',
      value: 'hola@orderly.app',
      desc: 'Respondemos en menos de 24h'
    },
    {
      icon: 'message-circle',
      title: 'WhatsApp',
      value: 'Escríbenos al bot',
      desc: 'Soporte inmediato en horario laboral'
    },
    {
      icon: 'clock',
      title: 'Horario de soporte',
      value: 'Lun – Vie · 9 am – 7 pm',
      desc: 'Hora de Bogotá (UTC-5)'
    }
  ];
}
