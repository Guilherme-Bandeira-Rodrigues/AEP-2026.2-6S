"use client";

import { useRef, useState, type FormEvent } from "react";
import { useAuth } from "@/contexts/auth-context";
import { errorMessage, fieldErrors } from "@/lib/errors";
import { validate } from "@/lib/validation";
import type { ErrosCampos } from "@/types/domain";

export function useForm(
  kind: Parameters<typeof validate>[0],
  action: (values: Record<string, string>) => Promise<void>,
) {
  const { handleError } = useAuth();
  const locked = useRef(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [fields, setFields] = useState<ErrosCampos>({});
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (locked.current) return;
    const form = event.currentTarget;
    const values = Object.fromEntries(new FormData(form)) as Record<
      string,
      string
    >;
    const errors = validate(kind, values);
    setFields(errors);
    setError("");
    if (Object.keys(errors).length) {
      setError("Confira os campos destacados.");
      focusField(form, Object.keys(errors)[0]);
      return;
    }
    locked.current = true;
    setBusy(true);
    try {
      await action(values);
    } catch (e) {
      setError(errorMessage(e));
      setFields(fieldErrors(e));
      const firstField = Object.keys(fieldErrors(e))[0];
      if (firstField) focusField(form, firstField);
      handleError(e);
    } finally {
      locked.current = false;
      setBusy(false);
    }
  }
  function attrs(name: string) {
    return {
      id: name,
      name,
      "aria-invalid": Boolean(fields[name]),
      "aria-describedby": fields[name] ? `${name}-error` : undefined,
    };
  }
  return { busy, error, fields, submit, attrs };
}

function focusField(form: HTMLFormElement, name: string) {
  const customControl = Array.from(
    form.querySelectorAll<HTMLElement>("[data-field-name]"),
  ).find((element) => element.dataset.fieldName === name);
  const target = customControl || form.elements.namedItem(name);
  if (target instanceof HTMLElement) target.focus();
}
