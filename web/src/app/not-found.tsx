import Link from "next/link";
export default function NotFound() {
  return (
    <div className="container page-space">
      <span className="eyebrow">PÁGINA NÃO ENCONTRADA</span>
      <h1 className="mt-3 mb-5 text-5xl">Vamos encontrar outro caminho?</h1>
      <p className="mb-7">Este endereço não está disponível.</p>
      <Link href="/" className="button button-primary">
        Explorar projetos
      </Link>
    </div>
  );
}
