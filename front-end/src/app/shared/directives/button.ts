import { Directive, input } from '@angular/core';

export type ButtonVariant = 'primario' | 'secundario' | 'terciario';
export type ButtonSize = 'sm' | 'md' | 'fw';

@Directive({
  selector: '[appButton], a[appButton]',
  host: {
    '[class]': '"btn btn-" + variant() + " btn-" + size() + (fullWidth() ? " btn-full" : "")'
  }
})
export class Button {
  constructor() {}
  variant = input<ButtonVariant>('primario');
  size = input<ButtonSize>('md');
  fullWidth = input<boolean>(false);
}
