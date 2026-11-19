import { useEffect, type ReactNode } from 'react';

interface Props {
  title: string;
  onClose: () => void;
  children: ReactNode;
  footer?: ReactNode;
}

export function Modal({ title, onClose, children, footer }: Props) {
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose();
    document.addEventListener('keydown', onKey);
    document.documentElement.classList.add('is-clipped');
    return () => {
      document.removeEventListener('keydown', onKey);
      document.documentElement.classList.remove('is-clipped');
    };
  }, [onClose]);

  return (
    <div className="modal is-active">
      <div className="modal-background" onClick={onClose} />
      <div className="modal-card">
        <header className="modal-card-head">
          <p className="modal-card-title">{title}</p>
          <button className="delete" aria-label="Schließen" onClick={onClose} />
        </header>
        <section className="modal-card-body">{children}</section>
        {footer && <footer className="modal-card-foot buttons is-right">{footer}</footer>}
      </div>
    </div>
  );
}
