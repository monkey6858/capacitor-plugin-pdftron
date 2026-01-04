import { WebPlugin } from '@capacitor/core';

import type { PDFTronPlugin } from './definitions';

export class PDFTronWeb extends WebPlugin implements PDFTronPlugin {
  // 基本方法
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
