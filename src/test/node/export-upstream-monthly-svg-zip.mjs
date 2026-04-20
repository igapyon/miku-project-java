/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

import { loadMikuprojectCoreApi } from "../../../vendor/mikuproject/scripts/lib/core-api-loader.mjs";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const repoRoot = path.resolve(__dirname, "../../..");
const vendorRoot = path.resolve(repoRoot, "vendor/mikuproject");

const [xmlPath, outputZip] = process.argv.slice(2);
if (!xmlPath || !outputZip) {
  console.error("usage: node src/test/node/export-upstream-monthly-svg-zip.mjs <input.xml> <output.zip>");
  process.exit(2);
}

const { api, dispose } = loadMikuprojectCoreApi({ rootDir: vendorRoot });
try {
  const xmlText = fs.readFileSync(xmlPath, "utf8");
  const model = api.msProject.importFromXml(xmlText);
  const archive = api.report.svg.exportMonthlyCalendar(model);
  fs.mkdirSync(path.dirname(path.resolve(outputZip)), { recursive: true });
  fs.writeFileSync(outputZip, Buffer.from(archive.zipBytes));
} finally {
  dispose();
}
