import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { SessionService } from './session.service';

export const apiInterceptor: HttpInterceptorFn = (req, next) => {
  const session = inject(SessionService);
  const user = session.currentUser();
  if (!user) {
    return next(req);
  }
  const request = req.clone({
    setHeaders: {
      'X-User-Id': user.id,
      'X-Roles': user.roles.join(','),
    },
  });

  return next(request);
};
