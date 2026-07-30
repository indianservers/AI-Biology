import fs from "node:fs";
import http from "node:http";
import path from "node:path";
import { spawn } from "node:child_process";
import { fileURLToPath } from "node:url";

const projectRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const viewerScript = path.join(projectRoot, "app", "src", "main", "assets", "model-viewer.min.js");
const outputDirectory = path.join(projectRoot, "app", "build", "cdn-thumbnails");
const chrome = "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe";
const roots = process.argv.slice(2);
const models = new Map();
const captures = new Map();

fs.mkdirSync(outputDirectory, { recursive: true });
for (const root of roots) {
  for (const entry of fs.readdirSync(root, { withFileTypes: true })) {
    if (entry.isFile() && path.extname(entry.name).toLowerCase() === ".glb") {
      models.set(entry.name.toLowerCase(), path.join(root, entry.name));
    }
  }
}

const server = http.createServer((request, response) => {
  const url = new URL(request.url, "http://127.0.0.1");
  if (url.pathname === "/model-viewer.min.js") {
    response.writeHead(200, { "Content-Type": "text/javascript" });
    fs.createReadStream(viewerScript).pipe(response);
    return;
  }
  if (url.pathname === "/model.glb") {
    const model = models.get((url.searchParams.get("name") || "").toLowerCase());
    if (!model) {
      response.writeHead(404).end();
      return;
    }
    response.writeHead(200, {
      "Content-Type": "model/gltf-binary",
      "Access-Control-Allow-Origin": "*"
    });
    fs.createReadStream(model).pipe(response);
    return;
  }
  if (url.pathname === "/render.html") {
    const name = url.searchParams.get("name") || "";
    response.writeHead(200, { "Content-Type": "text/html; charset=utf-8" });
    response.end(`<!doctype html>
<html>
<head>
  <meta charset="utf-8">
  <script type="module" src="/model-viewer.min.js"></script>
  <style>
    html,body{width:100%;height:100%;margin:0;overflow:hidden;background:#031326}
    model-viewer{width:100%;height:100%;background:#031326;--poster-color:transparent}
  </style>
</head>
<body>
  <model-viewer id="viewer" src="/model.glb?name=${encodeURIComponent(name)}"
    camera-controls interaction-prompt="none" exposure="1.05" shadow-intensity="0.8"
    camera-orbit="25deg 72deg auto"></model-viewer>
  <script>
    const viewer = document.getElementById("viewer");
    viewer.addEventListener("load", async () => {
      await new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)));
      const blob = await viewer.toBlob({ idealAspect: true });
      await fetch("/thumbnail?name=${encodeURIComponent(name)}", {
        method: "POST",
        body: await blob.arrayBuffer()
      });
    });
  </script>
</body>
</html>`);
    return;
  }
  if (url.pathname === "/thumbnail" && request.method === "POST") {
    const name = (url.searchParams.get("name") || "").toLowerCase();
    const chunks = [];
    request.on("data", chunk => chunks.push(chunk));
    request.on("end", () => {
      const capture = captures.get(name);
      if (!capture) {
        response.writeHead(404).end();
        return;
      }
      fs.writeFileSync(capture.path, Buffer.concat(chunks));
      capture.resolve();
      response.writeHead(204).end();
    });
    return;
  }
  response.writeHead(404).end();
});

await new Promise(resolve => server.listen(4178, "127.0.0.1", resolve));
try {
  for (const [name] of models) {
    const slug = path.basename(name, ".glb").replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");
    const screenshot = path.join(outputDirectory, `${slug}.png`);
    const url = `http://127.0.0.1:4178/render.html?name=${encodeURIComponent(name)}`;
    const browser = spawn(chrome, [
      "--headless=new",
      "--disable-gpu-sandbox",
      "--use-angle=swiftshader",
      "--enable-webgl",
      "--ignore-gpu-blocklist",
      "--hide-scrollbars",
      "--window-size=600,600",
      url
    ], { windowsHide: true, stdio: "ignore" });
    const capture = new Promise((resolve, reject) => {
      const timeout = setTimeout(
        () => reject(new Error(`Timed out rendering ${name}`)),
        90_000
      );
      captures.set(name, {
        path: screenshot,
        resolve: () => {
          clearTimeout(timeout);
          resolve();
        }
      });
    });
    try {
      await capture;
      process.stdout.write(`${name} -> ${screenshot} (${fs.statSync(screenshot).size} bytes)\n`);
    } catch (error) {
      fs.rmSync(screenshot, { force: true });
      process.stderr.write(`${error.message}\n`);
    } finally {
      captures.delete(name);
      browser.kill();
    }
  }
} finally {
  await new Promise(resolve => server.close(resolve));
}
