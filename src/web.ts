import { WebPlugin } from '@capacitor/core';

import type { PDFTronPlugin } from './definitions';

export class PDFTronWeb extends WebPlugin implements PDFTronPlugin {
  // 基本方法
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }

  // 初始化
  async initialize(options: { settings: string; viewerElement: string }): Promise<void> {
    console.log('web options', options);
  }

  // 保存
  async saveDocument(): Promise<void> {}
}
