import { Component, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';

interface FaqItem {
  q: string;
  a: string;
  category: string;
}

@Component({
  selector: 'app-faq',
  standalone: true,
  imports: [RouterLink, LucideAngularModule],
  templateUrl: './faq.component.html',
  styleUrl: './faq.component.css'
})
export class FaqComponent {
  openIndex = signal<number | null>(null);

  toggle(i: number) {
    this.openIndex.update(v => v === i ? null : i);
  }

  items: FaqItem[] = [
    {
      category: 'General',
      q: '¿Qué es Orderly y para qué tipo de negocios es?',
      a: 'Orderly es una plataforma SaaS que automatiza la gestión de pedidos por WhatsApp. Está diseñado para restaurantes, farmacias, tiendas, cafeterías, distribuidores y cualquier negocio que quiera tomar pedidos sin intervención humana constante.'
    },
    {
      category: 'General',
      q: '¿Necesito conocimientos técnicos para configurarlo?',
      a: 'No. El proceso de configuración está guiado paso a paso. Solo necesitas escanear el código QR para conectar WhatsApp, agregar tus productos y el bot empieza a funcionar. La mayoría de negocios completan la configuración en menos de 30 minutos.'
    },
    {
      category: 'General',
      q: '¿Mis clientes necesitan instalar alguna app?',
      a: 'No. Tus clientes solo necesitan WhatsApp, que ya tienen instalado. El bot funciona directamente en el chat, igual que cualquier conversación normal.'
    },
    {
      category: 'WhatsApp',
      q: '¿Qué número de WhatsApp necesito?',
      a: 'Puedes usar cualquier número de teléfono que no esté activo en otro dispositivo. Recomendamos un número de SIM dedicado al negocio. Puede ser un número normal o WhatsApp Business.'
    },
    {
      category: 'WhatsApp',
      q: '¿El bot puede recibir imágenes y comprobantes de pago?',
      a: 'Sí. El bot puede recibir imágenes de comprobantes de pago de tus clientes. Los operadores pueden acceder a ellos desde el detalle del pedido en el panel de control.'
    },
    {
      category: 'WhatsApp',
      q: '¿Qué pasa si un cliente necesita atención humana?',
      a: 'El operador puede tomar el control en cualquier momento. El bot reconoce el comando #bot para reactivarse. También puedes configurar respuestas para situaciones especiales.'
    },
    {
      category: 'Precios',
      q: '¿Los 14 días de prueba son realmente gratis?',
      a: 'Sí, completamente gratis. No pedimos tarjeta de crédito para activar la prueba. Al terminar los 14 días, eliges si continúas con un plan de pago o cancelas sin penalización.'
    },
    {
      category: 'Precios',
      q: '¿Puedo cambiar de plan en cualquier momento?',
      a: 'Sí. Puedes subir de plan en cualquier momento y el cambio es inmediato. Para bajar de plan, el cambio se aplica al inicio del siguiente período de facturación.'
    },
    {
      category: 'Precios',
      q: '¿Qué pasa si supero el límite de pedidos del plan?',
      a: 'Te notificamos cuando estás al 80% del límite mensual. Si llegas al 100%, el bot seguirá funcionando pero recibirás una notificación para que elijas subir de plan o esperar al próximo mes.'
    },
    {
      category: 'Técnico',
      q: '¿Cómo funciona el bot exactamente?',
      a: 'El bot usa una máquina de estados para rastrear en qué parte del proceso de compra está cada cliente. Cuando un cliente escribe, el bot responde según el estado: presentar menú, agregar al carrito, solicitar dirección, confirmar pago. Las sesiones se mantienen activas por 30 minutos.'
    },
    {
      category: 'Técnico',
      q: '¿Puedo personalizar los mensajes del bot?',
      a: 'Sí. Desde la sección de configuración puedes personalizar los mensajes de bienvenida, menú, confirmación de pedido, notificaciones de estado y más. También puedes dar nombre y emoji propio al bot.'
    },
    {
      category: 'Técnico',
      q: '¿El bot funciona fuera del horario de atención?',
      a: 'Puedes configurar los horarios de atención del negocio. Cuando el cliente escribe fuera del horario, el bot responde automáticamente informando que el servicio no está disponible y cuándo vuelve a abrir.'
    }
  ];
}
