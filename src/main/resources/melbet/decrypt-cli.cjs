/**
 * Melbet Digitain payload decrypt CLI (CommonJS).
 * Stdin: base64 payload or JSON {"payload":"..."}.
 * Stdout: decrypted JSON.
 */
'use strict';
const fs = require('fs');
const path = require('path');

async function main() {
  const input = fs.readFileSync(0, 'utf8').trim();
  if (!input) {
    console.error('empty stdin');
    process.exit(2);
  }
  let b64 = input;
  if (input.startsWith('{')) {
    const obj = JSON.parse(input);
    b64 = obj.payload;
  }
  if (!b64 || typeof b64 !== 'string') {
    console.error('missing payload');
    process.exit(2);
  }

  const dir = __dirname;
  const code = fs.readFileSync(path.join(dir, 'decrypt.js'), 'utf8');
  // eslint-disable-next-line no-eval
  eval(code.replace('var createDecryptor=', 'global.createDecryptor='));
  const p = await global.createDecryptor({
    locateFile: (f) => path.join(dir, f),
    wasmBinary: fs.readFileSync(path.join(dir, 'decrypt.wasm')),
  });
  p.wasmAlloc = p.cwrap('wasm_alloc', 'number', ['number']);
  p.wasmFree = p.cwrap('wasm_free', null, ['number']);
  p.wasmDecrypt = p.cwrap('wasm_decrypt', 'number', ['number', 'number']);
  p.wasmGetResult = p.cwrap('wasm_get_result', 'number', []);
  p.wasmGetResultLen = p.cwrap('wasm_get_result_len', 'number', []);
  p.wasmFreeResult = p.cwrap('wasm_free_result', null, []);

  const bin = Buffer.from(b64, 'base64');
  const ptr = p.wasmAlloc(bin.length);
  if (!ptr) {
    console.error('wasm_alloc failed');
    process.exit(3);
  }
  p.HEAPU8.set(bin, ptr);
  const rc = p.wasmDecrypt(ptr, bin.length);
  p.wasmFree(ptr);
  if (rc !== 0) {
    console.error('wasm_decrypt rc=' + rc);
    process.exit(3);
  }
  const outPtr = p.wasmGetResult();
  const outLen = p.wasmGetResultLen();
  const json = p.UTF8ToString(outPtr, outLen);
  p.wasmFreeResult();
  process.stdout.write(json);
}

main().catch((e) => {
  console.error(e && e.stack ? e.stack : String(e));
  process.exit(1);
});
