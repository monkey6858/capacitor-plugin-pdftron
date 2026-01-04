export interface PDFTronPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
