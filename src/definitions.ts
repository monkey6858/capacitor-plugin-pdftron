export interface pdftronPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
