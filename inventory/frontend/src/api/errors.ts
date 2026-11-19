/** Extracts a readable message from a failed API call (Spring ProblemDetail -> "detail"). */
export async function errorMessage(error: unknown): Promise<string> {
  const response = (error as { response?: unknown } | null)?.response;
  if (response instanceof Response) {
    const body = await response.clone().json().catch(() => undefined);
    if (body?.detail) {
      return body.detail as string;
    }
    return { 401: 'Nicht angemeldet', 403: 'Keine Berechtigung', 409: 'Von jemand anderem geändert – bitte neu laden' }[
      response.status
    ] ?? `Fehler ${response.status}`;
  }
  return error instanceof Error ? error.message : String(error);
}

/** Id of a created resource from the Location header of a 201 response. */
export function createdId(response: { raw: Response }): string {
  const location = response.raw.headers.get('Location') ?? '';
  return location.substring(location.lastIndexOf('/') + 1);
}
