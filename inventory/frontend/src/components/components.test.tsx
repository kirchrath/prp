import { act, render, renderHook, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { ArticleImage } from './ArticleImage';
import { Field } from './Field';
import { LocationSelect, sortAsTree, subtree } from './LocationSelect';
import { Modal } from './Modal';
import { NotificationProvider, useNotify } from './Notifications';
import { Empty, ErrorBox, Loading } from './Status';
import { useAsync } from '../hooks/useAsync';
import { apiError, loc } from '../test/utils';

const locations = [loc('b', 'Bad'), loc('k', 'Keller'), loc('r', 'Regal', 'k'), loc('f', 'Fach', 'r')];

describe('LocationSelect', () => {
  it('sorts as tree and computes subtrees', () => {
    expect(sortAsTree(locations).map((x) => `${x.depth}${x.location.name}`)).toEqual(['0Bad', '0Keller', '1Regal', '2Fach']);
    expect([...subtree(locations, 'r')]).toEqual(['r', 'f']);
  });

  it('selects and excludes', async () => {
    const onChange = vi.fn();
    render(<LocationSelect locations={locations} value="b" onChange={onChange} exclude="r" emptyLabel="Alle" />);
    expect(screen.queryByRole('option', { name: /Fach/ })).toBeNull();
    await userEvent.selectOptions(screen.getByLabelText('Lagerort'), 'k');
    await userEvent.selectOptions(screen.getByLabelText('Lagerort'), 'Alle');
    expect(onChange.mock.calls).toEqual([['k'], [undefined]]);
  });

  it('defaults', () => {
    render(<LocationSelect locations={[]} value={undefined} onChange={vi.fn()} />);
    expect(screen.getByRole('option')).toHaveTextContent('Bitte wählen…');
  });
});

describe('small components', () => {
  it('render', async () => {
    const retry = vi.fn();
    render(
      <>
        <Loading />
        <ErrorBox message="kaputt" onRetry={retry} />
        <Empty text="leer" />
        <Field label="Name" help="Hilfe"><input /></Field>
        <Field label="Ohne"><input /></Field>
        <ArticleImage url="x.png" />
        <ArticleImage />
      </>,
    );
    await userEvent.click(screen.getByText('Erneut versuchen'));
    expect(retry).toHaveBeenCalled();
    expect(screen.getByLabelText('Name')).toBeInTheDocument();
    expect(screen.getByText('Hilfe')).toBeInTheDocument();
    expect(screen.getByText('📦')).toBeInTheDocument();
  });

  it('modal closes on escape, background and button', async () => {
    const onClose = vi.fn();
    const { container, unmount } = render(<Modal title="T" onClose={onClose} footer={<span>F</span>}>Inhalt</Modal>);
    expect(document.documentElement).toHaveClass('is-clipped');
    await userEvent.keyboard('{Escape}');
    await userEvent.keyboard('a');
    await userEvent.click(container.querySelector('.modal-background')!);
    await userEvent.click(screen.getByLabelText('Schließen'));
    expect(onClose).toHaveBeenCalledTimes(3);
    unmount();
    expect(document.documentElement).not.toHaveClass('is-clipped');
    render(<Modal title="Ohne Footer" onClose={onClose}>x</Modal>);
    expect(screen.queryByText('F')).toBeNull();
  });
});

function Notifier() {
  const { notify, fail } = useNotify();
  return (
    <>
      <button onClick={() => notify('success', 'gut')}>ok</button>
      <button onClick={() => void fail(apiError(409, 'konflikt'))}>fail</button>
    </>
  );
}

describe('notifications', () => {
  it('shows and removes messages', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    render(<NotificationProvider><Notifier /></NotificationProvider>);
    await userEvent.click(screen.getByText('ok'));
    await userEvent.click(screen.getByText('fail'));
    expect(await screen.findByText('konflikt')).toBeInTheDocument();
    await userEvent.click(screen.getAllByLabelText('Meldung schließen')[0]);
    expect(screen.queryByText('gut')).toBeNull();
    act(() => vi.advanceTimersByTime(9000));
    expect(screen.queryByText('konflikt')).toBeNull();
    vi.useRealTimers();
  });

  it('fails outside the provider', () => {
    vi.spyOn(console, 'error').mockImplementation(() => undefined);
    expect(() => render(<Notifier />)).toThrow('NotificationProvider');
  });
});

describe('useAsync', () => {
  it('loads, reloads and reports errors', async () => {
    const loader = vi.fn().mockResolvedValueOnce(1).mockRejectedValueOnce(new Error('weg'));
    const { result } = renderHook(() => useAsync(loader, []));
    await waitFor(() => expect(result.current.data).toBe(1));
    act(() => result.current.reload());
    await waitFor(() => expect(result.current.error).toBe('weg'));
    expect(result.current.loading).toBe(false);
  });

  it('ignores results after unmount', async () => {
    let resolve: (v: number) => void = () => undefined;
    let reject: (e: Error) => void = () => undefined;
    const first = renderHook(() => useAsync(() => new Promise<number>((r) => { resolve = r; }), []));
    first.unmount();
    resolve(1);
    const second = renderHook(() => useAsync(() => new Promise<number>((_, r) => { reject = r; }), []));
    second.unmount();
    reject(new Error('x'));
    await new Promise((r) => setTimeout(r, 10));
    expect(first.result.current.data).toBeUndefined();
    expect(second.result.current.error).toBeUndefined();
  });
});
