import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { AuthService } from '../../core/services/auth.service';
import { ProductService } from '../../core/services/product.service';
import { CreateProductPayload, ProductResponse } from '../../core/models/orderly.models';

@Component({
  selector: 'app-products',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './products.component.html',
  styleUrl: './products.component.css'
})
export class ProductsComponent implements OnInit {
  private readonly auth    = inject(AuthService);
  private readonly prodSvc = inject(ProductService);

  products        = signal<ProductResponse[]>([]);
  loading         = signal(true);
  error           = signal<string | null>(null);
  showForm        = signal(false);
  saving          = signal(false);
  selectedImage   = signal<File | null>(null);
  imagePreviewUrl = signal<string | null>(null);

  editingProduct  = signal<ProductResponse | null>(null);
  deletingId      = signal<string | null>(null);

  newProduct: CreateProductPayload = this.emptyProduct();
  editPayload: CreateProductPayload = this.emptyProduct();

  get businessId(): string | null { return this.auth.getActiveBusinessId(); }

  ngOnInit(): void { this.load(); }

  async load(): Promise<void> {
    const bId = this.businessId;
    if (!bId) { this.error.set('Sin negocio activo.'); this.loading.set(false); return; }
    try {
      this.products.set(await this.prodSvc.list(bId));
    } catch (e: any) {
      this.error.set(e?.error?.message ?? 'Error cargando productos.');
    } finally {
      this.loading.set(false);
    }
  }

  async createProduct(): Promise<void> {
    const bId = this.businessId;
    if (!bId) return;
    this.saving.set(true);
    try {
      let created = await this.prodSvc.create(bId, this.newProduct);
      if (this.selectedImage()) {
        created = await this.prodSvc.uploadImage(bId, created.id, this.selectedImage()!);
      }
      this.products.update(list => [created, ...list]);
      this.newProduct = this.emptyProduct();
      this.clearImageSelection();
      this.showForm.set(false);
    } catch (e: any) {
      this.error.set(e?.error?.message ?? 'Error creando producto.');
    } finally {
      this.saving.set(false);
    }
  }

  startEdit(prod: ProductResponse): void {
    this.editingProduct.set(prod);
    this.editPayload = {
      name:        prod.name,
      description: prod.description ?? '',
      price:       prod.price,
      available:   prod.available,
      stock:       prod.stock ?? null,
    };
    this.deletingId.set(null);
  }

  cancelEdit(): void { this.editingProduct.set(null); }

  async saveEdit(): Promise<void> {
    const bId  = this.businessId;
    const prod = this.editingProduct();
    if (!bId || !prod) return;
    this.saving.set(true);
    try {
      const updated = await this.prodSvc.update(bId, prod.id, this.editPayload);
      this.products.update(list => list.map(p => p.id === updated.id ? updated : p));
      this.editingProduct.set(null);
    } catch (e: any) {
      this.error.set(e?.error?.message ?? 'Error actualizando producto.');
    } finally {
      this.saving.set(false);
    }
  }

  requestDelete(id: string): void {
    this.deletingId.set(id);
    this.editingProduct.set(null);
  }

  cancelDelete(): void { this.deletingId.set(null); }

  async deleteProduct(id: string): Promise<void> {
    const bId = this.businessId;
    if (!bId) return;
    this.saving.set(true);
    try {
      await this.prodSvc.delete(bId, id);
      this.products.update(list => list.filter(p => p.id !== id));
      this.deletingId.set(null);
    } catch (e: any) {
      this.error.set(e?.error?.message ?? 'Error eliminando producto.');
    } finally {
      this.saving.set(false);
    }
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 }).format(amount);
  }

  dismissError(): void { this.error.set(null); }

  onImageSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file  = input.files?.[0] ?? null;
    this.selectedImage.set(file);
    if (!file) { this.imagePreviewUrl.set(null); return; }
    const reader = new FileReader();
    reader.onload = () => this.imagePreviewUrl.set(reader.result as string);
    reader.readAsDataURL(file);
  }

  clearImageSelection(): void {
    this.selectedImage.set(null);
    this.imagePreviewUrl.set(null);
  }

  resolveImageUrl(path?: string): string | null {
    if (!path) return null;
    if (path.startsWith('http://') || path.startsWith('https://')) return path;
    return path.startsWith('/') ? path : `/${path}`;
  }

  private emptyProduct(): CreateProductPayload {
    return { name: '', description: '', price: 0, available: true, stock: null };
  }
}
