/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

import { loadMikuprojectCoreApi } from "../../../vendor/miku-project/scripts/lib/core-api-loader.mjs";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const repoRoot = path.resolve(__dirname, "../../..");
const vendorRoot = path.resolve(repoRoot, "vendor/miku-project");

const [xmlPath, outputDir] = process.argv.slice(2);
if (!xmlPath || !outputDir) {
  console.error("usage: node src/test/node/export-upstream-report-dir.mjs <input.xml> <output.dir>");
  process.exit(2);
}

const { api, dispose } = loadMikuprojectCoreApi({ rootDir: vendorRoot });
try {
  const xmlText = fs.readFileSync(xmlPath, "utf8");
  const model = api.msProject.importFromXml(xmlText);
  const reportBundle = api.report.all.export(model);
  fs.mkdirSync(outputDir, { recursive: true });

  for (const entry of reportBundle.entries) {
    const outputPath = path.resolve(outputDir, entry.name);
    fs.mkdirSync(path.dirname(outputPath), { recursive: true });
    fs.writeFileSync(outputPath, Buffer.from(entry.data));
  }
} finally {
  dispose();
}
