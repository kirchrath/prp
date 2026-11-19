import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { apiError, audit, fakeApi, loc, renderWith } from '../test/utils';
import { LocationsPage } from './LocationsPage';

const tree = [{ ...loc('k', 'Keller'), description: 'unten' }, loc('r', 'Regal', 'k'), loc('b', 'Bad')];

describe('LocationsPage', () => {
  it('creates, edits and deletes locations', async () => {
    const api = fakeApi();
    api.locations.listLocations.mockResolvedValue(tree);
    api.locations.getLocation.mockResolvedValue({ audit: audit(2), data: { name: 'Regal', parentId: 'k' } });
    renderWith(<LocationsPage />, api);
    expect(await screen.findByText('Regal')).toBeInTheDocument();
    expect(screen.getByText('· unten')).toBeInTheDocument();

    await userEvent.click(screen.getByText('+ Lagerort'));
    expect(screen.getByText('Speichern')).toBeDisabled();
    await userEvent.type(screen.getByLabelText('Name'), 'Garage');
    await userEvent.type(screen.getByLabelText('Beschreibung'), 'draußen');
    await userEvent.click(screen.getByText('Speichern'));
    expect(api.locations.createLocation).toHaveBeenCalledWith({ locationSaveDto: { data: { name: 'Garage', description: 'draußen', parentId: undefined } } });

    await userEvent.click(screen.getByLabelText('Unterort von Bad anlegen'));
    expect(within(document.querySelector('.modal') as HTMLElement).getByLabelText('Lagerort')).toHaveValue('b');
    await userEvent.type(screen.getByLabelText('Name'), 'Schrank');
    api.locations.createLocation.mockRejectedValueOnce(apiError(404, 'Lagerort fehlt'));
    await userEvent.click(screen.getByText('Speichern'));
    expect(await screen.findByText('Lagerort fehlt')).toBeInTheDocument();
    await userEvent.click(screen.getByText('Abbrechen'));

    await userEvent.click(screen.getByLabelText('Regal bearbeiten'));
    const modal = document.querySelector('.modal') as HTMLElement;
    // a location cannot be moved below itself
    expect(within(modal).queryByRole('option', { name: /Regal/ })).toBeNull();
    await userEvent.click(within(modal).getByText('Speichern'));
    expect(api.locations.updateLocation).toHaveBeenCalledWith({ id: 'r', locationSaveDto: { audit: audit(2), data: { name: 'Regal', description: undefined, parentId: 'k' } } });

    api.locations.getLocation.mockRejectedValueOnce(apiError(404, 'weg'));
    await userEvent.click(screen.getByLabelText('Regal bearbeiten'));
    expect(await screen.findByText('weg')).toBeInTheDocument();

    vi.spyOn(window, 'confirm').mockReturnValueOnce(false).mockReturnValue(true);
    await userEvent.click(screen.getByLabelText('Bad löschen'));
    expect(api.locations.deleteLocation).not.toHaveBeenCalled();
    await userEvent.click(screen.getByLabelText('Bad löschen'));
    expect(api.locations.deleteLocation).toHaveBeenCalledWith({ id: 'b', version: 2 });
    expect(await screen.findByText('Lagerort gelöscht')).toBeInTheDocument();
    api.locations.deleteLocation.mockRejectedValueOnce(apiError(409, 'Lagerort enthält noch Bestand'));
    await userEvent.click(screen.getByLabelText('Keller löschen'));
    expect(await screen.findByText('Lagerort enthält noch Bestand')).toBeInTheDocument();
  });

  it('shows empty list and errors', async () => {
    const api = fakeApi();
    api.locations.listLocations.mockRejectedValueOnce(new Error('weg'));
    renderWith(<LocationsPage />, api);
    await userEvent.click(await screen.findByText('Erneut versuchen'));
    expect(await screen.findByText(/Noch keine Lagerorte/)).toBeInTheDocument();
  });

  it('creates a location while the list is loading', async () => {
    const api = fakeApi();
    api.locations.listLocations.mockImplementation(() => new Promise(() => undefined));
    renderWith(<LocationsPage />, api);
    await userEvent.click(screen.getByText('+ Lagerort'));
    expect(screen.getByRole('option', { name: '– (oberste Ebene)' })).toBeInTheDocument();
  });
});
