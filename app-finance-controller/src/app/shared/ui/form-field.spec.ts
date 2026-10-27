import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { requiredText } from '../forms/validators';
import { FORM_FIELD_IMPORTS } from './form-field';

@Component({
  selector: 'app-test-host',
  imports: [FORM_FIELD_IMPORTS, ReactiveFormsModule],
  template: `
    <app-form-field label="Nome" help="Como aparece nos relatórios." [control]="name">
      <input appFieldControl [formControl]="name" />
    </app-form-field>
  `,
})
class HostComponent {
  readonly name = new FormControl('', { nonNullable: true, validators: [requiredText] });
}

describe('FormFieldComponent', () => {
  function setup() {
    const fixture = TestBed.createComponent(HostComponent);
    fixture.detectChanges();
    const root: HTMLElement = fixture.nativeElement;
    const input = root.querySelector('input');
    const label = root.querySelector('label');
    if (!input || !label) {
      throw new Error('campo não renderizado');
    }
    return { fixture, root, input, label, control: fixture.componentInstance.name };
  }

  it('liga o rótulo ao controle e descreve a ajuda', () => {
    const { input, label, root } = setup();

    expect(label.getAttribute('for')).toBe(input.id);
    expect(input.id).not.toBe('');
    const help = root.querySelector('p');
    expect(help?.id).toBeTruthy();
    expect(input.getAttribute('aria-describedby')).toBe(help?.id);
    expect(input.getAttribute('aria-invalid')).toBeNull();
  });

  it('mostra o erro com role=alert, aria-invalid e aria-describedby depois de tocar', async () => {
    const { fixture, root, input, control } = setup();

    control.markAsTouched();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const alert = root.querySelector('[role="alert"]');
    expect(alert?.textContent?.trim()).toBe('Campo obrigatório');
    expect(input.getAttribute('aria-invalid')).toBe('true');
    expect(input.getAttribute('aria-describedby')?.split(' ')).toContain(alert?.id ?? 'x');
  });

  it('remove o erro quando o valor fica válido', async () => {
    const { fixture, root, input, control } = setup();
    control.markAsTouched();
    control.setValue('Mercado');
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(root.querySelector('[role="alert"]')).toBeNull();
    expect(input.getAttribute('aria-invalid')).toBeNull();
  });
});
