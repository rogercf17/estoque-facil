import type { FormEvent } from 'react'
import { FormField } from '../components/FormField'

// Tela vazia da Sprint 0: layout e acessibilidade prontos, sem chamada à API.
// A autenticação (services/authService.ts) é ligada na Sprint 1.
export function LoginPage() {
  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
  }

  return (
    <main className="login">
      <form className="login__card" onSubmit={handleSubmit}>
        <h1>Estoque Fácil</h1>
        <h2>Entrar</h2>
        <FormField id="email" label="E-mail" type="email" autoComplete="username" />
        <FormField id="password" label="Senha" type="password" autoComplete="current-password" />
        <button type="submit" className="btn">
          Entrar
        </button>
      </form>
    </main>
  )
}
