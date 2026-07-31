import crypto from "node:crypto";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { spawnSync } from "node:child_process";

const root = path.resolve(process.argv[2] || "D:\\3D objects\\Biology");
const masterCatalogPath = path.join(root, "biology-catalog.json");
const requiredArchiveFiles = new Set([
  "checksums.json",
  "manifest.json",
  "model.glb",
  "README.txt",
  "thumbnail.png"
]);
const errors = [];
const warnings = [];

function fail(message) {
  errors.push(message);
}

function sha256(filePath) {
  const hash = crypto.createHash("sha256");
  hash.update(fs.readFileSync(filePath));
  return hash.digest("hex");
}

function readJson(filePath, label = filePath) {
  try {
    return JSON.parse(fs.readFileSync(filePath, "utf8"));
  } catch (error) {
    fail(`${label}: invalid JSON (${error.message})`);
    return null;
  }
}

function resolveCatalogPath(catalogPath, relativePath, label) {
  if (typeof relativePath !== "string" || relativePath.trim() === "") {
    fail(`${label}: missing relative path`);
    return null;
  }
  const resolved = path.resolve(
    path.dirname(catalogPath),
    relativePath.replaceAll("/", path.sep)
  );
  if (resolved !== root && !resolved.startsWith(`${root}${path.sep}`)) {
    fail(`${label}: path escapes CDN root (${relativePath})`);
    return null;
  }
  return resolved;
}

function verifyPng(filePath, label) {
  if (!fs.existsSync(filePath)) {
    fail(`${label}: thumbnail is missing`);
    return null;
  }
  const buffer = fs.readFileSync(filePath);
  const signature = "89504e470d0a1a0a";
  if (buffer.length < 24 || buffer.subarray(0, 8).toString("hex") !== signature) {
    fail(`${label}: thumbnail is not a valid PNG`);
    return null;
  }
  const width = buffer.readUInt32BE(16);
  const height = buffer.readUInt32BE(20);
  if (width < 128 || height < 128) {
    fail(`${label}: thumbnail is too small (${width}x${height})`);
  }
  return { width, height, bytes: buffer.length };
}

function verifyGlb(filePath, label) {
  const buffer = fs.readFileSync(filePath);
  if (
    buffer.length < 20 ||
    buffer.subarray(0, 4).toString("ascii") !== "glTF" ||
    buffer.readUInt32LE(4) !== 2 ||
    buffer.readUInt32LE(8) !== buffer.length
  ) {
    fail(`${label}: invalid GLB 2.0 header`);
    return;
  }
  const jsonLength = buffer.readUInt32LE(12);
  const jsonType = buffer.subarray(16, 20).toString("ascii");
  if (jsonType !== "JSON" || 20 + jsonLength > buffer.length) {
    fail(`${label}: invalid GLB JSON chunk`);
    return;
  }
  try {
    JSON.parse(
      buffer
        .subarray(20, 20 + jsonLength)
        .toString("utf8")
        .replace(/\0|\s+$/g, "")
    );
  } catch (error) {
    fail(`${label}: malformed GLB JSON (${error.message})`);
  }
}

function archiveEntries(zipPath, label) {
  const result = spawnSync("tar", ["-tf", zipPath], {
    encoding: "utf8",
    windowsHide: true,
    maxBuffer: 10 * 1024 * 1024
  });
  if (result.status !== 0) {
    fail(`${label}: ZIP cannot be listed (${result.stderr.trim()})`);
    return [];
  }
  return result.stdout
    .split(/\r?\n/)
    .map(entry => entry.replaceAll("\\", "/").replace(/^\.\/+/, "").replace(/\/$/, ""))
    .filter(Boolean);
}

function verifyArchive(zipPath, model, standaloneManifest) {
  const label = model.id;
  const entries = archiveEntries(zipPath, label);
  for (const entry of entries) {
    if (entry.startsWith("/") || entry.split("/").includes("..")) {
      fail(`${label}: unsafe ZIP entry (${entry})`);
    }
  }
  for (const required of requiredArchiveFiles) {
    if (!entries.includes(required)) {
      fail(`${label}: ZIP is missing ${required}`);
    }
  }
  if (entries.some(entry => !requiredArchiveFiles.has(entry))) {
    warnings.push(`${label}: ZIP contains extra files`);
  }

  const extractionRoot = fs.mkdtempSync(path.join(os.tmpdir(), "biology-cdn-verify-"));
  try {
    const extraction = spawnSync("tar", ["-xf", zipPath, "-C", extractionRoot], {
      encoding: "utf8",
      windowsHide: true,
      maxBuffer: 10 * 1024 * 1024
    });
    if (extraction.status !== 0) {
      fail(`${label}: ZIP cannot be extracted (${extraction.stderr.trim()})`);
      return;
    }

    const manifestPath = path.join(extractionRoot, "manifest.json");
    const checksumsPath = path.join(extractionRoot, "checksums.json");
    const modelPath = path.join(extractionRoot, "model.glb");
    const thumbnailPath = path.join(extractionRoot, "thumbnail.png");
    const packageManifest = readJson(manifestPath, `${label} package manifest`);
    const checksums = readJson(checksumsPath, `${label} checksums`);
    if (!packageManifest || !checksums) return;

    if (JSON.stringify(packageManifest) !== JSON.stringify(standaloneManifest)) {
      fail(`${label}: packaged and standalone manifests differ`);
    }
    if (packageManifest.id !== model.id) {
      fail(`${label}: manifest ID is ${packageManifest.id}`);
    }
    if (fs.statSync(modelPath).size !== model.modelSizeBytes) {
      fail(`${label}: extracted model size differs from catalog`);
    }
    const modelHash = sha256(modelPath);
    if (
      modelHash !== packageManifest.model?.sha256 ||
      modelHash !== checksums.files?.["model.glb"]
    ) {
      fail(`${label}: extracted model checksum mismatch`);
    }
    const thumbnailHash = sha256(thumbnailPath);
    if (
      thumbnailHash !== packageManifest.thumbnail?.sha256 ||
      thumbnailHash !== checksums.files?.["thumbnail.png"]
    ) {
      fail(`${label}: extracted thumbnail checksum mismatch`);
    }
    verifyGlb(modelPath, label);
    verifyPng(thumbnailPath, `${label} packaged`);
  } finally {
    fs.rmSync(extractionRoot, {
      recursive: true,
      force: true,
      maxRetries: 5,
      retryDelay: 200
    });
  }
}

function walkFiles(directory, predicate, results = []) {
  for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
    const entryPath = path.join(directory, entry.name);
    if (entry.isDirectory()) walkFiles(entryPath, predicate, results);
    else if (predicate(entryPath)) results.push(entryPath);
  }
  return results;
}

if (!fs.existsSync(masterCatalogPath)) {
  throw new Error(`Master catalog not found: ${masterCatalogPath}`);
}

const masterCatalog = readJson(masterCatalogPath, "master catalog");
if (!masterCatalog) process.exit(1);
if (masterCatalog.schemaVersion !== 1) fail("master catalog: unsupported schemaVersion");
if (!Array.isArray(masterCatalog.models) || masterCatalog.models.length === 0) {
  fail("master catalog: models must be a non-empty array");
}

const ids = new Set();
const packagePaths = new Set();
const masterById = new Map();
let packageBytes = 0;
let modelBytes = 0;
let thumbnailBytes = 0;

for (const model of masterCatalog.models || []) {
  const label = model.id || "<missing-id>";
  if (!model.id || ids.has(model.id)) fail(`${label}: duplicate or missing model ID`);
  ids.add(model.id);
  masterById.set(model.id, model);

  for (const field of ["title", "scientificName", "categoryId", "shortDescription"]) {
    if (typeof model[field] !== "string" || model[field].trim() === "") {
      fail(`${label}: missing ${field}`);
    }
  }

  const zipPath = resolveCatalogPath(masterCatalogPath, model.packagePath, `${label} package`);
  const manifestPath = resolveCatalogPath(
    masterCatalogPath,
    model.manifestPath,
    `${label} manifest`
  );
  const thumbnailPath = resolveCatalogPath(
    masterCatalogPath,
    model.thumbnailPath,
    `${label} thumbnail`
  );
  if (!zipPath || !manifestPath || !thumbnailPath) continue;
  if (packagePaths.has(zipPath)) fail(`${label}: duplicate package path`);
  packagePaths.add(zipPath);

  if (!fs.existsSync(zipPath)) {
    fail(`${label}: ZIP is missing`);
    continue;
  }
  if (!fs.existsSync(manifestPath)) {
    fail(`${label}: standalone manifest is missing`);
    continue;
  }
  const zipSize = fs.statSync(zipPath).size;
  packageBytes += zipSize;
  if (zipSize !== model.packageSizeBytes) fail(`${label}: package size mismatch`);
  if (sha256(zipPath) !== model.packageSha256) fail(`${label}: package SHA-256 mismatch`);

  const standaloneManifest = readJson(manifestPath, `${label} standalone manifest`);
  if (!standaloneManifest) continue;
  const png = verifyPng(thumbnailPath, label);
  if (png) thumbnailBytes += png.bytes;
  if (standaloneManifest.model?.sizeBytes !== model.modelSizeBytes) {
    fail(`${label}: manifest and catalog model sizes differ`);
  }
  modelBytes += model.modelSizeBytes || 0;
  verifyArchive(zipPath, model, standaloneManifest);
}

const supportingCatalogPaths = [
  path.join(root, "anatomy-catalog.json"),
  ...walkFiles(
    root,
    filePath => path.basename(filePath).toLowerCase() === "catalog.json"
  )
].filter((filePath, index, values) => values.indexOf(filePath) === index);

for (const catalogPath of supportingCatalogPaths) {
  const catalog = readJson(catalogPath);
  if (!catalog) continue;
  if (catalog.catalogVersion !== masterCatalog.catalogVersion) {
    fail(`${path.relative(root, catalogPath)}: catalogVersion differs from master`);
  }
  const localIds = new Set();
  for (const model of catalog.models || []) {
    if (localIds.has(model.id)) {
      fail(`${path.relative(root, catalogPath)}: duplicate ID ${model.id}`);
    }
    localIds.add(model.id);
    const masterModel = masterById.get(model.id);
    if (!masterModel) {
      fail(`${path.relative(root, catalogPath)}: unknown model ${model.id}`);
      continue;
    }
    if (
      model.packageSha256 !== masterModel.packageSha256 ||
      model.packageSizeBytes !== masterModel.packageSizeBytes
    ) {
      fail(`${path.relative(root, catalogPath)}: stale package metadata for ${model.id}`);
    }
    for (const [field, suffix] of [
      ["packagePath", "package"],
      ["manifestPath", "manifest"],
      ["thumbnailPath", "thumbnail"]
    ]) {
      const resolved = resolveCatalogPath(catalogPath, model[field], `${model.id} ${suffix}`);
      if (resolved && !fs.existsSync(resolved)) {
        fail(`${path.relative(root, catalogPath)}: unresolved ${field} for ${model.id}`);
      }
    }
  }
}

const onDiskZips = walkFiles(
  root,
  filePath =>
    filePath.toLowerCase().endsWith(".zip") &&
    filePath.split(path.sep).includes("_cdn_packages")
).map(filePath => path.resolve(filePath));
for (const zipPath of onDiskZips) {
  if (!packagePaths.has(zipPath)) {
    fail(`Orphan ZIP not listed in master catalog: ${path.relative(root, zipPath)}`);
  }
}
for (const zipPath of packagePaths) {
  if (!onDiskZips.includes(zipPath)) {
    fail(`Catalog ZIP is outside the package inventory: ${path.relative(root, zipPath)}`);
  }
}

const summary = {
  root,
  catalogVersion: masterCatalog.catalogVersion,
  models: masterCatalog.models.length,
  supportingCatalogs: supportingCatalogPaths.length,
  zipPackages: onDiskZips.length,
  packageMiB: Number((packageBytes / 1024 / 1024).toFixed(3)),
  unpackedModelMiB: Number((modelBytes / 1024 / 1024).toFixed(3)),
  thumbnailMiB: Number((thumbnailBytes / 1024 / 1024).toFixed(3)),
  warnings: warnings.length,
  errors: errors.length
};

process.stdout.write(`${JSON.stringify(summary, null, 2)}\n`);
for (const warning of warnings) process.stderr.write(`WARNING: ${warning}\n`);
for (const error of errors) process.stderr.write(`ERROR: ${error}\n`);
process.exitCode = errors.length === 0 ? 0 : 1;
