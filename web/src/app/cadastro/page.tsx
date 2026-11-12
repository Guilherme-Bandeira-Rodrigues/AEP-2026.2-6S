import { Suspense } from "react";
import { AuthForm } from "@/components/auth-form";
import { Loading } from "@/components/ui";
export const metadata = { title: "Criar conta" };
export default function Page() {
  return (
    <Suspense fallback={<Loading />}>
      <AuthForm mode="cadastro" />
    </Suspense>
  );
}
