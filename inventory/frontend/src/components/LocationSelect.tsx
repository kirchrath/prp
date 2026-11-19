import type { LocationDto } from '../api/generated';

interface Props {
  locations: LocationDto[];
  value: number | undefined;
  onChange: (id: number | undefined) => void;
  /** Label of the empty option; without it a location must be chosen. */
  emptyLabel?: string;
  /** Location (and its subtree) that must not be selectable, e.g. when choosing a parent. */
  exclude?: number;
  required?: boolean;
}

/** Locations sorted as a tree; nesting is shown by indentation. */
export function sortAsTree(locations: LocationDto[]): { location: LocationDto; depth: number }[] {
  const children = new Map<number | undefined, LocationDto[]>();
  for (const l of locations) {
    const list = children.get(l.parentId) ?? [];
    list.push(l);
    children.set(l.parentId, list);
  }
  const result: { location: LocationDto; depth: number }[] = [];
  const walk = (parent: number | undefined, depth: number) => {
    for (const l of (children.get(parent) ?? []).sort((a, b) => a.name.localeCompare(b.name, 'de'))) {
      result.push({ location: l, depth });
      walk(l.id, depth + 1);
    }
  };
  walk(undefined, 0);
  return result;
}

export function LocationSelect({ locations, value, onChange, emptyLabel, exclude, required }: Props) {
  const excluded = new Set<number>();
  if (exclude !== undefined) {
    excluded.add(exclude);
    let changed = true;
    while (changed) {
      changed = false;
      for (const l of locations) {
        if (l.parentId !== undefined && excluded.has(l.parentId) && !excluded.has(l.id)) {
          excluded.add(l.id);
          changed = true;
        }
      }
    }
  }
  return (
    <div className="select is-fullwidth">
      <select
        value={value ?? ''}
        required={required}
        onChange={(e) => onChange(e.target.value ? Number(e.target.value) : undefined)}
      >
        {(emptyLabel !== undefined || value === undefined) && <option value="">{emptyLabel ?? 'Bitte wählen…'}</option>}
        {sortAsTree(locations)
          .filter(({ location }) => !excluded.has(location.id))
          .map(({ location, depth }) => (
            <option key={location.id} value={location.id}>
              {'  '.repeat(depth * 2)}
              {location.name}
            </option>
          ))}
      </select>
    </div>
  );
}
