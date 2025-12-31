import { registerPlugin } from '@capacitor/core';

import type { pdftronPlugin } from './definitions';

const pdftron = registerPlugin<pdftronPlugin>('pdftron', {
  web: () => import('./web').then((m) => new m.pdftronWeb()),
});

export * from './definitions';
export { pdftron };
