import { redirect } from "next/navigation";

// La raíz no tiene contenido propio: el punto de entrada es el login de cliente.
export default function RootPage() {
  redirect("/login-cliente");
}
