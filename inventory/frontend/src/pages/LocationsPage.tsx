import { useState } from 'react';
import { useApi } from '../api/ApiContext';
import type { LocationDto, LocationListDto } from '../api/stock';
import { Field } from '../components/Field';
import { LocationSelect, sortAsTree } from '../components/LocationSelect';
import { Modal } from '../components/Modal';
import { useNotify } from '../components/Notifications';
import { Empty, ErrorBox, Loading } from '../components/Status';
import { useAsync } from '../hooks/useAsync';

type Editing = { id?: string; location?: LocationDto; parentId?: string };

export function LocationsPage() {
  const api = useApi();
  const { notify, fail } = useNotify();
  const locations = useAsync(() => api.locations.listLocations(), [api]);
  const [editing, setEditing] = useState<Editing>();

  const edit = async (l: LocationListDto) => {
    try {
      setEditing({ id: l.id, location: await api.locations.getLocation({ id: l.id }) });
    } catch (e) {
      await fail(e);
    }
  };

  const remove = async (l: LocationListDto) => {
    if (!window.confirm(`Lagerort "${l.path}" löschen?`)) return;
    try {
      const { audit } = await api.locations.getLocation({ id: l.id });
      await api.locations.deleteLocation({ id: l.id, version: audit.version });
      notify('success', 'Lagerort gelöscht');
      locations.reload();
    } catch (e) {
      await fail(e);
    }
  };

  let content;
  if (locations.error) {
    content = <ErrorBox message={locations.error} onRetry={locations.reload} />;
  } else if (!locations.data) {
    content = <Loading />;
  } else if (locations.data.length === 0) {
    content = <Empty text="Noch keine Lagerorte. Lege z. B. „Küche“ oder „Keller“ an." />;
  } else {
    content = (
      <div className="box p-0">
        <table className="table is-fullwidth is-hoverable">
          <tbody>
            {sortAsTree(locations.data).map(({ location: l, depth }) => (
              <tr key={l.id}>
                <td>
                  <span className="tree-indent" style={{ width: `${depth * 1.5}rem` }} />
                  <strong>{l.name}</strong>
                  {l.description && <small className="has-text-grey"> · {l.description}</small>}
                </td>
                <td className="has-text-right">
                  <div className="buttons are-small is-right is-flex-wrap-nowrap">
                    <button className="button is-light" aria-label={`Unterort von ${l.name} anlegen`} onClick={() => setEditing({ parentId: l.id })}>
                      + Unterort
                    </button>
                    <button className="button is-light" aria-label={`${l.name} bearbeiten`} onClick={() => edit(l)}>
                      ✎
                    </button>
                    <button className="button is-danger is-light" aria-label={`${l.name} löschen`} onClick={() => remove(l)}>
                      🗑
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  }

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
      {content}
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

function LocationModal({ editing, locations, onClose, onSaved }: {
  editing: Editing;
  locations: LocationListDto[];
  onClose: () => void;
  onSaved: () => void;
}) {
  const api = useApi();
  const { fail } = useNotify();
  const data = editing.location?.data;
  const [name, setName] = useState(data?.name ?? '');
  const [description, setDescription] = useState(data?.description ?? '');
  const [parentId, setParentId] = useState(data ? data.parentId : editing.parentId);

  const save = async () => {
    const locationDataDto = { name, description: description || undefined, parentId };
    try {
      if (editing.location) {
        await api.locations.updateLocation({ id: editing.id!, locationSaveDto: { audit: editing.location.audit, data: locationDataDto } });
      } else {
        await api.locations.createLocation({ locationSaveDto: { data: locationDataDto } });
      }
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
        <input className="input" value={name} onChange={(e) => setName(e.target.value)} />
      </Field>
      <Field label="Beschreibung">
        <input className="input" value={description} onChange={(e) => setDescription(e.target.value)} />
      </Field>
      <div className="field">
        <p className="label">Übergeordneter Lagerort</p>
        <LocationSelect locations={locations} value={parentId} onChange={setParentId} emptyLabel="– (oberste Ebene)" exclude={editing.id} />
      </div>
    </Modal>
  );
}
