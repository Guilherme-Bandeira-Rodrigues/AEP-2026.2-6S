import type { ErrosCampos } from "@/types/domain";

export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
    public fields: ErrosCampos = {},
  ) {
    super(message);
    this.name = "ApiError";
  }
}
export function errorMessage(error: unknown): string {
  return error instanceof Error
    ? error.message
    : "Não foi possível concluir. Tente novamente.";
}
export function fieldErrors(error: unknown): ErrosCampos {
  return error instanceof ApiError ? error.fields : {};
}
