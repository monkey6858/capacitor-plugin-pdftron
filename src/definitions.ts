export interface PDFTronPlugin {
  // 基本方法
  echo(options: { value: string }): Promise<{ value: string }>;

  // 初始化
  initialize(options: { settings: string; viewerElement: string }): Promise<void>;
}
