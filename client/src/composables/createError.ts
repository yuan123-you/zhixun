/** 创建标准化的应用错误对象 */
export function createError(options: { statusCode: number; message: string; fatal?: boolean }) {
  const err = new Error(options.message) as any
  err.statusCode = options.statusCode
  err.fatal = options.fatal ?? false
  return err
}
