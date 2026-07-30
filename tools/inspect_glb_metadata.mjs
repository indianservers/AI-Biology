import fs from "node:fs";
import path from "node:path";

const roots = process.argv.slice(2);

function readGlbJson(filePath) {
  const file = fs.openSync(filePath, "r");
  try {
    const header = Buffer.alloc(20);
    fs.readSync(file, header, 0, header.length, 0);
    if (header.toString("ascii", 0, 4) !== "glTF") {
      throw new Error("Not a GLB file");
    }
    const jsonLength = header.readUInt32LE(12);
    if (header.toString("ascii", 16, 20) !== "JSON") {
      throw new Error("GLB has no JSON chunk");
    }
    const json = Buffer.alloc(jsonLength);
    fs.readSync(file, json, 0, jsonLength, 20);
    return JSON.parse(json.toString("utf8").replace(/\0+$/g, "").trim());
  } finally {
    fs.closeSync(file);
  }
}

for (const root of roots) {
  for (const entry of fs.readdirSync(root, { withFileTypes: true })) {
    if (!entry.isFile() || path.extname(entry.name).toLowerCase() !== ".glb") continue;
    const filePath = path.join(root, entry.name);
    const gltf = readGlbJson(filePath);
    const names = (items = []) =>
      items.map((item, index) => item.name || `#${index}`).filter(Boolean);
    const report = {
      file: filePath,
      nodes: names(gltf.nodes),
      meshes: names(gltf.meshes),
      materials: names(gltf.materials),
      animations: names(gltf.animations),
      scenes: names(gltf.scenes),
      nodeCount: gltf.nodes?.length || 0,
      meshCount: gltf.meshes?.length || 0,
      materialCount: gltf.materials?.length || 0,
      animationCount: gltf.animations?.length || 0
    };
    process.stdout.write(`${JSON.stringify(report)}\n`);
  }
}
