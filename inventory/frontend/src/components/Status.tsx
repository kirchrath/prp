export function Loading() {
  return (
    <div className="has-text-centered py-5">
      <span className="loader-inline" /> Lade…
    </div>
  );
}

export function ErrorBox({ message, onRetry }: { message: string; onRetry: () => void }) {
  return (
    <div className="notification is-danger is-light">
      {message}
      <button className="button is-small is-danger is-outlined ml-3" onClick={onRetry}>
        Erneut versuchen
      </button>
    </div>
  );
}

export function Empty({ text }: { text: string }) {
  return <p className="has-text-grey has-text-centered py-5">{text}</p>;
}
