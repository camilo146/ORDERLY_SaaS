import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-features-page',
  standalone: true,
  imports: [RouterLink, LucideAngularModule],
  templateUrl: './features-page.component.html',
  styleUrl: './features-page.component.css'
})
export class FeaturesPageComponent {
  categories = [
    {
      label: 'Bot & Automatización',
      icon: 'bot',
      headline: 'Vende mientras duermes',
      sub: 'Tu bot nunca descansa. Atiende consultas, toma pedidos y confirma pagos sin que tú estés presente.',
      items: [
        { icon: 'message-circle', title: 'Menú interactivo', desc: 'El bot presenta tu catálogo por categorías y permite al cliente navegar de forma natural mediante respuestas numéricas.' },
        { icon: 'shopping-cart', title: 'Carrito conversacional', desc: 'Cada cliente tiene su propio carrito. Puede agregar, modificar o eliminar productos antes de confirmar.' },
        { icon: 'zap', title: 'Máquina de estados', desc: 'El bot recuerda en qué parte del proceso está cada cliente. Sesión activa por 30 minutos para no perder el contexto.' },
        { icon: 'clock', title: 'Disponibilidad 24/7', desc: 'Sin turnos, sin días libres. El bot responde de inmediato a cualquier hora, incluso cuando el negocio está cerrado.' },
        { icon: 'bot', title: 'Identidad propia', desc: 'Dale nombre y emoji al bot. "Hola, soy Carlita 🍕 de Don Carlo". Cada negocio tiene su propia personalidad.' },
        { icon: 'users', title: 'Traspaso a humano', desc: 'Con el comando #humano el operador toma el control. Con #bot se reactiva la automatización.' }
      ]
    },
    {
      label: 'Gestión de Pedidos',
      icon: 'clipboard-list',
      headline: 'Control total en tiempo real',
      sub: 'Cada pedido que entra por WhatsApp aparece instantáneamente en tu panel Kanban con toda la información del cliente.',
      items: [
        { icon: 'layout-dashboard', title: 'Panel Kanban', desc: 'Visualiza todos tus pedidos organizados por estado: Pendiente, Confirmado, En camino y Entregado.' },
        { icon: 'zap', title: 'Actualizaciones en vivo', desc: 'Cada cambio de estado se refleja al instante en el panel y se notifica automáticamente al cliente por WhatsApp.' },
        { icon: 'truck', title: 'Seguimiento de entrega', desc: 'El cliente recibe notificación cuando su pedido es confirmado, despachado y entregado.' },
        { icon: 'file-text', title: 'Detalle completo', desc: 'Cada pedido muestra: cliente, items, cantidades, dirección, método de pago y comprobante adjunto.' },
        { icon: 'x-circle', title: 'Gestión de cancelaciones', desc: 'Cancela pedidos con motivo. El cliente es notificado automáticamente por WhatsApp.' },
        { icon: 'clock', title: 'Historial completo', desc: 'Accede al historial de todos los pedidos con filtros por fecha, estado y cliente.' }
      ]
    },
    {
      label: 'Catálogo & Productos',
      icon: 'package',
      headline: 'Tu menú digital, siempre actualizado',
      sub: 'Gestiona todo tu catálogo desde el panel. Cambios instantáneos que el bot refleja de inmediato.',
      items: [
        { icon: 'package', title: 'Productos ilimitados', desc: 'Agrega todos los productos que necesites, organizados por categorías que el bot presentará en orden.' },
        { icon: 'image', title: 'Imágenes de producto', desc: 'Sube fotos de tus productos. El bot las puede compartir cuando el cliente lo solicite.' },
        { icon: 'trending-up', title: 'Control de stock', desc: 'Activa el control de stock para que el bot sepa cuándo un producto está agotado y no lo ofrezca.' },
        { icon: 'pencil', title: 'Edición rápida', desc: 'Modifica nombre, precio o imagen en segundos. Los cambios se reflejan en el bot al instante.' },
        { icon: 'trash-2', title: 'Activar / Desactivar', desc: 'Pausa productos temporalmente sin eliminarlos. Ideal para productos de temporada o stock agotado.' },
        { icon: 'star', title: 'Organización por categorías', desc: 'Estructura tu catálogo en categorías. El bot las presenta como opciones numeradas al cliente.' }
      ]
    },
    {
      label: 'Equipo & Operaciones',
      icon: 'users',
      headline: 'Roles para cada miembro',
      sub: 'Define quién puede hacer qué. Admins con control total, operadores enfocados solo en los pedidos del turno.',
      items: [
        { icon: 'shield', title: 'Rol Admin', desc: 'Acceso completo: productos, configuración, dashboard, analytics, quejas y gestión del canal WhatsApp.' },
        { icon: 'headphones', title: 'Rol Operador', desc: 'Panel simplificado enfocado en los pedidos activos. Sin acceso a configuración ni datos sensibles.' },
        { icon: 'building-2', title: 'Multi-negocio', desc: 'En planes Business gestiona múltiples locales desde una sola cuenta con sus propios equipos.' },
        { icon: 'alert-circle', title: 'Quejas y reclamos', desc: 'Los clientes pueden reportar problemas vía WhatsApp. El equipo las gestiona desde el panel.' },
        { icon: 'toggle-right', title: 'Estado del servicio', desc: 'Activa o pausa el servicio con un clic. Ideal para horarios de atención o días especiales.' },
        { icon: 'clock', title: 'Horarios de atención', desc: 'Configura los horarios del negocio. El bot informa automáticamente cuando está fuera de horario.' }
      ]
    }
  ];
}
