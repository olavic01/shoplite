import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { ApiService, Auth, Note, Order, Product, Stock } from './api.service';
import { TOKEN_KEY } from './auth.interceptor';

@Component({
  selector: 'app-root',
  imports: [FormsModule, CurrencyPipe, DatePipe],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
})
export class AppComponent implements OnInit, OnDestroy {
  private api = inject(ApiService);
  private timer?: ReturnType<typeof setInterval>;
  private toastTimer?: ReturnType<typeof setTimeout>;

  session = signal<Auth | null>(this.loadSession());
  mode = signal<'login' | 'register'>('login');
  form = { name: '', email: '', password: '' };
  formError = signal('');
  busy = signal(false);

  products = signal<Product[] | null>(null);
  stock = signal<Record<number, number>>({});
  orders = signal<Order[]>([]);
  notes = signal<Note[]>([]);
  qty: Record<number, number> = {};
  loadError = signal('');
  toast = signal<{ text: string; kind: 'ok' | 'error' } | null>(null);
  ordering = signal<number | null>(null);

  productName = computed(() => {
    const map: Record<number, string> = {};
    (this.products() ?? []).forEach((p) => (map[p.id] = p.name));
    return map;
  });

  ngOnInit() {
    this.loadProducts();
    if (this.session()) this.startPolling();
  }
  ngOnDestroy() { clearInterval(this.timer); }

  private loadSession(): Auth | null {
    try { return JSON.parse(localStorage.getItem(TOKEN_KEY) ?? 'null'); } catch { return null; }
  }

  // ---- auth ----
  submitAuth() {
    this.formError.set('');
    this.busy.set(true);
    const call = this.mode() === 'login'
      ? this.api.login({ email: this.form.email, password: this.form.password })
      : this.api.register({ ...this.form });
    call.subscribe({
      next: (auth) => {
        localStorage.setItem(TOKEN_KEY, JSON.stringify(auth));
        this.session.set(auth);
        this.busy.set(false);
        this.form = { name: '', email: '', password: '' };
        this.startPolling();
      },
      error: (e: HttpErrorResponse) => {
        this.busy.set(false);
        this.formError.set(this.authMessage(e));
      },
    });
  }

  private authMessage(e: HttpErrorResponse): string {
    if (e.status === 401) return 'Wrong email or password.';
    if (e.status === 409) return 'That email is already registered. Sign in instead.';
    if (e.status === 400) return 'Check the form: use a valid email and a password of at least 6 characters.';
    if (e.status === 0) return 'Cannot reach the server. Check that the app is running.';
    return 'Something went wrong. Try again in a moment.';
  }

  signOut() {
    localStorage.removeItem(TOKEN_KEY);
    this.session.set(null);
    clearInterval(this.timer);
    this.orders.set([]);
    this.notes.set([]);
    this.stock.set({});
  }

  // ---- data ----
  loadProducts() {
    this.api.products().subscribe({
      next: (p) => { this.products.set(p); this.loadError.set(''); },
      error: () => this.loadError.set('Could not load the catalog. The product service may be down.'),
    });
  }

  private startPolling() {
    this.refresh();
    clearInterval(this.timer);
    this.timer = setInterval(() => this.refresh(), 4000);
  }

  refresh() {
    if (!this.session()) return;
    this.api.stock().subscribe({
      next: (rows: Stock[]) => {
        const m: Record<number, number> = {};
        rows.forEach((r) => (m[r.productId] = r.quantity));
        this.stock.set(m);
      },
      error: () => {},
    });
    this.api.orders().subscribe({ next: (o) => this.orders.set(o), error: () => {} });
    this.api.notifications().subscribe({ next: (n) => this.notes.set(n), error: () => {} });
  }

  // ---- ordering ----
  quantityFor(id: number) { return this.qty[id] ?? 1; }

  stockLabel(id: number): { text: string; level: 'ok' | 'low' | 'out' | 'unknown' } {
    const left = this.stock()[id];
    if (left === undefined) return { text: this.session() ? 'Checking stock' : 'Sign in to see stock', level: 'unknown' };
    if (left <= 0) return { text: 'Out of stock', level: 'out' };
    if (left <= 10) return { text: `Only ${left} left`, level: 'low' };
    return { text: `${left} in stock`, level: 'ok' };
  }

  order(p: Product) {
    if (!this.session()) {
      this.showToast('Sign in to place an order.', 'error');
      return;
    }
    const q = Math.max(1, Math.floor(Number(this.quantityFor(p.id)) || 1));
    this.ordering.set(p.id);
    this.api.placeOrder(p.id, q).subscribe({
      next: (o) => {
        this.ordering.set(null);
        this.showToast(`Order #${o.id} placed: ${q} × ${p.name}.`, 'ok');
        this.qty[p.id] = 1;
        setTimeout(() => this.refresh(), 1200); // give Kafka consumers a moment
      },
      error: () => {
        this.ordering.set(null);
        this.showToast('The order did not go through. Try again.', 'error');
      },
    });
  }

  showToast(text: string, kind: 'ok' | 'error') {
    this.toast.set({ text, kind });
    clearTimeout(this.toastTimer);
    this.toastTimer = setTimeout(() => this.toast.set(null), 5000);
  }
}
