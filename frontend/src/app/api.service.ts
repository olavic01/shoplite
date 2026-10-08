import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

export interface Product { id: number; name: string; description: string; price: number; }
export interface Stock { productId: number; quantity: number; }
export interface Order { id: number; productId: number; quantity: number; status: string; createdAt: string; }
export interface Note { orderId: number; message: string; at: string; }
export interface Auth { token: string; email: string; name: string; }

@Injectable({ providedIn: 'root' })
export class ApiService {
  private http = inject(HttpClient);

  register(body: { email: string; name: string; password: string }) {
    return this.http.post<Auth>('/api/auth/register', body);
  }
  login(body: { email: string; password: string }) {
    return this.http.post<Auth>('/api/auth/login', body);
  }
  products() { return this.http.get<Product[]>('/api/products'); }
  stock() { return this.http.get<Stock[]>('/api/inventory'); }
  orders() { return this.http.get<Order[]>('/api/orders'); }
  notifications() { return this.http.get<Note[]>('/api/notifications'); }
  placeOrder(productId: number, quantity: number) {
    return this.http.post<Order>('/api/orders', { productId, quantity });
  }
}
