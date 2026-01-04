import { registerPlugin } from '@capacitor/core';

import type { PDFTronPlugin } from './definitions';

const PDFTron = registerPlugin<PDFTronPlugin>('PDFTron', {
  web: () => import('./web').then((m) => new m.PDFTronWeb()),
});

export * from './definitions';
export { PDFTron };
