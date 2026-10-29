import { Component, provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MoneyInputDirective, sanitizeMoney } from './money-input';

describe('sanitizeMoney', () => {
  it.each([
    ['', '', ''],
    ['12', '12', '12'],
    ['1234,56', '1234,56', '1234.56'],
    ['1234.56', '1234,56', '1234.56'],
    ['12,', '12,', '12'],
    [',5', ',5', '0.5'],
    ['1,234', '1,23', '1.23'],
    ['abc12x', '12', '12'],
    ['-5', '5', '5'],
    ['1,2,3', '1,23', '1.23'],
    ['9007199254740993,99', '9007199254740993,99', '9007199254740993.99'],
  ])('"%s" → mostra "%s" e envia "%s"', (raw, display, value) => {
    expect(sanitizeMoney(raw)).toEqual({ display, value });
  });
});

@Component({
  imports: [ReactiveFormsModule, MoneyInputDirective],
  template: `<input appMoneyInput [formControl]="control" />`,
})
class Host {
  readonly control = new FormControl('', { nonNullable: true });
}

describe('MoneyInputDirective', () => {
  async function setup() {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] });
    const fixture = TestBed.createComponent(Host);
    await fixture.whenStable();
    return { fixture, input: (fixture.nativeElement as HTMLElement).querySelector('input') as HTMLInputElement };
  }

  it('guarda o decimal da API e mostra com vírgula', async () => {
    const { fixture, input } = await setup();

    input.value = '1234,5';
    input.dispatchEvent(new Event('input'));
    expect(fixture.componentInstance.control.value).toBe('1234.5');

    fixture.componentInstance.control.setValue('99.90');
    expect(input.value).toBe('99,90');
  });

  it('descarta caracteres inválidos ao digitar e limita a duas casas', async () => {
    const { fixture, input } = await setup();

    input.value = 'R$ 1.5abc9';
    input.dispatchEvent(new Event('input'));

    expect(input.value).toBe('1,59');
    expect(fixture.componentInstance.control.value).toBe('1.59');
  });

  it('exibe vazio para valor nulo e tem teclado decimal', async () => {
    const { fixture, input } = await setup();

    fixture.componentInstance.control.setValue('');
    expect(input.value).toBe('');
    expect(input.getAttribute('inputmode')).toBe('decimal');
  });
});
