import { budgetAlertToast } from './budget-alert-message';

describe('budgetAlertToast', () => {
  it.each([
    [{ scope: 'CATEGORY', status: 'EXCEEDED', categoryName: 'Lazer' }, 'error', 'Meta de Lazer estourada!'],
    [{ scope: 'CATEGORY', status: 'WARNING', categoryName: 'Lazer' }, 'warning', 'Você está perto da meta de Lazer.'],
    [{ scope: 'GLOBAL', status: 'EXCEEDED' }, 'error', 'Meta mensal estourada!'],
    [{ scope: 'GLOBAL', status: 'WARNING' }, 'warning', 'Você já usou 80% da sua meta mensal.'],
  ] as const)('%o', (alert, kind, message) => {
    expect(budgetAlertToast(alert)).toEqual({ kind, message });
  });
});
