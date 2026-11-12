"use client";
export default function ErrorPage({
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <div className="container page-space">
      <h1 className="text-4xl mb-4">Não foi possível abrir esta página.</h1>
      <p className="mb-6">Tente novamente em instantes.</p>
      <button className="button button-primary" onClick={reset}>
        Tentar novamente
      </button>
    </div>
  );
}
