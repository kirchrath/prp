import type { LocationListDto } from '../api/stock';

interface Props {
  locations: LocationListDto[];
  value: string | undefined;
  onChange: (id: string | undefined) => void;
  /** Label of the empty option. */
  emptyLabel?: string;
  /** Location (and its subtree) that must not be selectable, e.g. when choosing a parent. */
  exclude?: string;
}

/** Locations sorted as a tree. */
export function sortAsTree(locations: LocationListDto[]): { location: LocationListDto; depth: number }[] {
  const result: { location: LocationListDto; depth: number }[] = [];
  const walk = (parent: string | undefined, depth: number) => {
    locations
      .filter((l) => l.parentId === parent)
      .sort((a, b) => a.name.localeCompare(b.name, 'de'))
      .forEach((l) => {
        result.push({ location: l, depth });
        walk(l.id, depth + 1);
      });
  };
  walk(undefined, 0);
  return result;
}

/** The location and all its descendants. */
export function subtree(locations: LocationListDto[], rootId: string): Set<string> {
  const ids = new Set([rootId]);
  for (const { location } of sortAsTree(locations)) {
    if (location.parentId && ids.has(location.parentId)) ids.add(location.id);
  }
  return ids;
}

export function LocationSelect({ locations, value, onChange, emptyLabel = 'Bitte wählen…', exclude }: Props) {
  const excluded = exclude ? subtree(locations, exclude) : new Set<string>();
  return (
    <div className="select is-fullwidth">
      <select aria-label="Lagerort" value={value ?? ''} onChange={(e) => onChange(e.target.value || undefined)}>
        <option value="">{emptyLabel}</option>
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
