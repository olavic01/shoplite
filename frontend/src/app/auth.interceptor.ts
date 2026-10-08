import { HttpInterceptorFn } from '@angular/common/http';
import { tap } from 'rxjs';

export const TOKEN_KEY = 'shoplite.session';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const raw = localStorage.getItem(TOKEN_KEY);
  const token = raw ? JSON.parse(raw).token : null;
  const authed = token && !req.url.startsWith('/api/auth/') ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;
  return next(authed).pipe(
    tap({
      error: (e) => {
        // Session expired or token rejected: go back to sign-in.
        if (e.status === 401 && !req.url.startsWith('/api/auth/') && token) {
          localStorage.removeItem(TOKEN_KEY);
          location.reload();
        }
      },
    }),
  );
};
