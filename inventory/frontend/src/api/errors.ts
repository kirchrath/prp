import { ResponseError } from './generated';

/** Extracts a readable message from a failed API call (Spring ProblemDetail -> "detail"). */
export async function errorMessage(error: unknown): Promise<string> {
  if (error instanceof ResponseError) {
    try {
      const body = await error.response.clone().json();
      if (body?.detail) {
        return body.detail as string;
      }
    } catch {
      // no JSON body
    }
    if (error.response.status === 401) {
      return 'Nicht angemeldet';
    }
    if (error.response.status === 403) {
      return 'Keine Berechtigung';
    }
    return `Fehler ${error.response.status}`;
  }
  return error instanceof Error ? error.message : String(error);
}
