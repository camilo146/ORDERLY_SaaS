import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink, LucideAngularModule],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent {
  features = [
    {
      icon: 'bot',
      title: 'Bot conversacional 24/7',
      description: 'Tu negocio atiende clientes a cualquier hora sin intervención humana. El bot guía desde el menú hasta la confirmación del pago.'
    },
    {
      icon: 'zap',
      title: 'Pedidos en tiempo real',
      description: 'Cada pedido aparece al instante en tu Kanban. Confirma, despacha y entrega sin perder de vista ningún pedido del día.'
    },
    {
      icon: 'package',
      title: 'Catálogo digital',
      description: 'Gestiona productos, precios, imágenes y stock desde el panel. Los cambios se reflejan en el bot de forma inmediata.'
    },
    {
      icon: 'message-circle',
      title: 'WhatsApp nativo',
      description: 'Sin apps nuevas para tus clientes. Piden por el canal que ya usan cada día con una tasa de conversión 3x mayor que e-commerce.'
    },
    {
      icon: 'bar-chart-3',
      title: 'Analytics integrado',
      description: 'Visualiza métricas clave: pedidos por día, productos más vendidos, horarios pico y alertas de clientes inactivos.'
    },
    {
      icon: 'users',
      title: 'Equipo multi-rol',
      description: 'Admins gestionan el negocio y el menú. Operadores atienden pedidos del turno. Cada rol ve solo lo que necesita.'
    }
  ];

  steps = [
    {
      num: '01',
      icon: 'smartphone',
      title: 'Conecta tu WhatsApp',
      desc: 'Escanea el código QR en tu dashboard y vincula tu número de WhatsApp Business en segundos.'
    },
    {
      num: '02',
      icon: 'package',
      title: 'Configura tu catálogo',
      desc: 'Agrega tus productos con nombre, precio e imagen. Personaliza el bot con el nombre y emoji de tu marca.'
    },
    {
      num: '03',
      icon: 'zap',
      title: 'Recibe pedidos',
      desc: 'El bot atiende a tus clientes 24/7 y cada pedido aparece en tu Kanban en tiempo real, listo para gestionar.'
    }
  ];

  testimonials = [
    {
      initial: 'M',
      name: 'Miguel Ángel Torres',
      role: 'Dueño · Pizzería Don Carlo',
      quote: 'Antes perdíamos pedidos por WhatsApp porque no dábamos abasto. Con Orderly el bot toma los pedidos solo y nosotros solo cocinamos. Subimos 40% en ventas el primer mes.'
    },
    {
      initial: 'S',
      name: 'Sandra Vidal',
      role: 'Gerente · Farmacia Vida Nueva',
      quote: 'Nuestros clientes adoraron poder pedir medicamentos por WhatsApp sin necesidad de llamar. El panel es clarísimo y el equipo lo aprendió en un día.'
    },
    {
      initial: 'R',
      name: 'Rodrigo Méndez',
      role: 'Fundador · Mercado Express',
      quote: 'El onboarding fue increíblemente rápido. En 30 minutos teníamos el bot funcionando con todo nuestro catálogo. El soporte es impecable.'
    }
  ];

  plans = [
    {
      name: 'Starter',
      price: 29,
      desc: 'Para negocios que están empezando',
      featured: false,
      cta: 'Comenzar gratis',
      ctaRoute: '/register',
      features: ['1 número de WhatsApp', 'Hasta 500 pedidos/mes', 'Catálogo ilimitado', '1 operador', 'Soporte por email']
    },
    {
      name: 'Growth',
      price: 79,
      desc: 'Para negocios en crecimiento',
      featured: true,
      cta: 'Comenzar gratis',
      ctaRoute: '/register',
      features: ['1 número de WhatsApp', 'Hasta 2,000 pedidos/mes', 'Catálogo ilimitado', '3 operadores', 'Analytics avanzado', 'Soporte prioritario']
    },
    {
      name: 'Business',
      price: 199,
      desc: 'Para operaciones de alto volumen',
      featured: false,
      cta: 'Hablar con ventas',
      ctaRoute: '/contact',
      features: ['Múltiples negocios', 'Pedidos ilimitados', 'Operadores ilimitados', 'API de integración', 'Soporte WhatsApp 24/7']
    }
  ];

  stars = [1, 2, 3, 4, 5];
}
