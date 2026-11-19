import type { ReactNode } from 'react';

export function Field({ label, children, help }: { label: string; children: ReactNode; help?: string }) {
  return (
    <div className="field">
      <label className="label">
        {label}
        <div className="control">{children}</div>
      </label>
      {help && <p className="help">{help}</p>}
    </div>
  );
}
