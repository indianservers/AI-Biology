import fs from "node:fs";
import http from "node:http";
import path from "node:path";

const root = path.resolve(process.argv[2] || "D:\\3D objects\\Biology");
const port = Number(process.argv[3] || 8765);
const types = new Map([
  [".json", "application/json; charset=utf-8"],
  [".zip", "application/zip"],
  [".png", "image/png"],
  [".jpg", "image/jpeg"],
  [".jpeg", "image/jpeg"],
  [".webp", "image/webp"],
  [".dzi", "application/xml; charset=utf-8"],
  [".glb", "model/gltf-binary"]
]);

http.createServer((request, response) => {
  const pathname = decodeURIComponent(new URL(request.url, "http://localhost").pathname);
  const file = path.resolve(root, `.${pathname}`);
  if (!file.startsWith(`${root}${path.sep}`) || !fs.existsSync(file) || !fs.statSync(file).isFile()) {
    response.writeHead(404).end("Not found");
    return;
  }
  response.writeHead(200, {
    "Content-Type": types.get(path.extname(file).toLowerCase()) || "application/octet-stream",
    "Content-Length": fs.statSync(file).size,
    "Access-Control-Allow-Origin": "*",
    "Cache-Control": "no-cache"
  });
  fs.createReadStream(file).pipe(response);
}).listen(port, "0.0.0.0", () => {
  process.stdout.write(`Biology CDN: http://127.0.0.1:${port}/\n`);
});
