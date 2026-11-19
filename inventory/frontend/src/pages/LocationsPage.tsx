import { useState } from 'react';
import { useApi } from '../api/ApiContext';
import type { LocationDto } from '../api/generated';
import { Field } from '../components/Field';
import { LocationSelect, sortAsTree } from '../components/LocationSelect';
import { Modal } from '../components/Modal';
import { useNotify } from '../components/Notifications';
import { Empty, ErrorBox, Loading } from '../components/Status';
import { useAsync } from '../hooks/useAsync';

type Editing = { location?: LocationDto; parentId?: number };

export function LocationsPage() {
  const api = useApi();
  const { notify, fail } = useNotify();
  const locations = useAsync(() => api.locations.listLocations(), [api]);
  const [editing, setEditing] = useState<Editing>();

  const remove = async (l: LocationDto) => {
    if (!window.confirm(`Lagerort "${l.path}" löschen?`)) return;
    try {
      await api.locations.deleteLocation({ id: l.id });
      notify('success', 'Lagerort gelöscht');
      locations.reload();
    } catch (e) {
      await fail(e);
    }
  };

  return (
    <>
      <div className="level is-mobile">
        <div className="level-left">
          <h1 className="title">Lagerorte</h1>
        </div>
        <div className="level-right">
          <button className="button is-primary" onClick={() => setEditing({})}>
            + Lagerort
          </button>
        </div>
      </div>

      {locations.error && <ErrorBox message={locations.error} onRetry={locations.reload} />}
      {locations.loading && !locations.data ? (
        <Loading />
      ) : (locations.data ?? []).length === 0 ? (
        <Empty text="Noch keine Lagerorte. Lege z. B. „Küche“ oder „Keller“ an." />
      ) : (
        <div className="box p-0">
          <table className="table is-fullwidth is-hoverable">
            <tbody>
              {sortAsTree(locations.data!).map(({ location: l, depth }) => (
                <tr key={l.id}>
                  <td>
                    <span className="tree-indent" style={{ width: `${depth * 1.5}rem` }} />
                    {depth > 0 && <span className="has-text-grey mr-1">└</span>}
                    <strong>{l.name}</strong>
                    {l.description && <small className="has-text-grey"> · {l.description}</small>}
                  </td>
                  <td className="has-text-right">
                    <div className="buttons are-small is-right is-flex-wrap-nowrap">
                      <button className="button is-light" title="Unter-Lagerort anlegen" onClick={() => setEditing({ parentId: l.id })}>
                        + Unterort
                      </button>
                      <button className="button is-light" onClick={() => setEditing({ location: l })}>
                        ✎
                      </button>
                      <button className="button is-danger is-light" onClick={() => remove(l)}>
                        🗑
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {editing && (
        <LocationModal
          editing={editing}
          locations={locations.data ?? []}
          onClose={() => setEditing(undefined)}
          onSaved={() => {
            setEditing(undefined);
            locations.reload();
          }}
        />
      )}
    </>
  );
}

function LocationModal({
  editing,
  locations,
  onClose,
  onSaved,
}: {
  editing: Editing;
  locations: LocationDto[];
  onClose: () => void;
  onSaved: () => void;
}) {
  const api = useApi();
  const { fail } = useNotify();
  const [name, setName] = useState(editing.location?.name ?? '');
  const [description, setDescription] = useState(editing.location?.description ?? '');
  const [parentId, setParentId] = useState<number | undefined>(editing.location?.parentId ?? editing.parentId);

  const save = async () => {
    const locationRequest = { name, description: description || undefined, parentId };
    try {
      if (editing.location) await api.locations.updateLocation({ id: editing.location.id, locationRequest });
      else await api.locations.createLocation({ locationRequest });
      onSaved();
    } catch (e) {
      await fail(e);
    }
  };

  return (
    <Modal
      title={editing.location ? 'Lagerort bearbeiten' : 'Neuer Lagerort'}
      onClose={onClose}
      footer={
        <>
          <button className="button" onClick={onClose}>
            Abbrechen
          </button>
          <button className="button is-primary" disabled={!name.trim()} onClick={save}>
            Speichern
          </button>
        </>
      }
    >
      <Field label="Name">
        <input className="input" value={name} onChange={(e) => setName(e.target.value)} autoFocus />
      </Field>
      <Field label="Beschreibung">
        <input className="input" value={description} onChange={(e) => setDescription(e.target.value)} />
      </Field>
      <Field label="Übergeordneter Lagerort">
        <LocationSelect
          locations={locations}
          value={parentId}
          onChange={setParentId}
          emptyLabel="– (oberste Ebene)"
          exclude={editing.location?.id}
        />
      </Field>
    </Modal>
  );
}
