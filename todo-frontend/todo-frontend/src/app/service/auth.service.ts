import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { JwtHelperService } from '@auth0/angular-jwt';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private urlAuth = 'http://localhost:8080/api/v1/auth';
  private jwtHelper: JwtHelperService = new JwtHelperService();

  private readonly TOKEN_KEY = 'auth-token';

  constructor(private http: HttpClient) {}

  login(email: string, password: string): Observable<any> {
    const url = `${this.urlAuth}/authenticate`;
    debugger;
    return this.http.post<any>(url, { email, password }).pipe(
      tap((response) => {
        if (response && response.token) {
          this.setToken(response.token);
        }
      })
    );
  }

  setToken(token: string): void {
    localStorage.setItem(this.TOKEN_KEY, token);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  isLoggedIn(): boolean {
    // check if token exists and not expired
    const token = this.getToken();
    return token != null && !this.isTokenExpired(token);
  }
  isTokenExpired(token: string): boolean {
    return this.jwtHelper.isTokenExpired(token);
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    // Perform any additional cleanup, such as redirecting to the login page
  }

  getUserRoles() {
    debugger;
    // Retrieve user roles from JWT token stored in localStorage
    const token = this.getToken();
    if (token) {
      const decodedToken = this.jwtHelper.decodeToken(token);
      console.log(decodedToken.roles);

      return decodedToken.roles;
    }
    return [];
  }
}
