import { WebPlugin } from '@capacitor/core';

import type { pdftronPlugin } from './definitions';

export class pdftronWeb extends WebPlugin implements pdftronPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
