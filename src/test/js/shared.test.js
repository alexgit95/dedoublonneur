// Tests unitaires legers pour shared.js, sans framework/bundler : execution directe via
// `node src/test/js/shared.test.js` (aucune dependance npm, coherent avec la contrainte
// "framework-free frontend" de la capability responsive-ui).
"use strict";

const assert = require("assert");
const path = require("path");

const DedoublonneurUI = require(path.join(__dirname, "..", "..", "main", "resources", "static", "js", "shared.js"));

async function testDebounceDelaysRecomputation() {
  let callCount = 0;
  const debounced = DedoublonneurUI.debounce(() => {
    callCount++;
  }, 30);

  // Simule l'utilisateur qui deplace le curseur plusieurs fois rapidement.
  debounced();
  debounced();
  debounced();

  assert.strictEqual(callCount, 0, "ne doit pas appeler fn avant la fin du delai");

  await new Promise((resolve) => setTimeout(resolve, 60));

  assert.strictEqual(callCount, 1, "doit appeler fn une seule fois apres le delai, malgre les 3 appels rapproches");
}

function testBuildPaginatedUrlRequestsOnlyOnePageAtATime() {
  const url = DedoublonneurUI.buildPaginatedUrl("/api/jobs/1/blurred", 2, 24);

  assert.strictEqual(url, "/api/jobs/1/blurred?page=2&size=24");
  // La construction d'URL ne fait jamais reference a "toutes les pages" : un seul
  // couple (page, size) est encode, garantissant qu'un seul lot de photos est demande.
  assert.ok(!url.includes("all"), "ne doit jamais demander l'ensemble des photos en une fois");
}

function testBuildPaginatedUrlHandlesExistingQueryString() {
  const url = DedoublonneurUI.buildPaginatedUrl("/api/jobs/1/duplicates?threshold=90", 0, 24);

  assert.strictEqual(url, "/api/jobs/1/duplicates?threshold=90&page=0&size=24");
}

function testFormatBytes() {
  assert.strictEqual(DedoublonneurUI.formatBytes(500), "500 o");
  assert.strictEqual(DedoublonneurUI.formatBytes(2048), "2.0 Ko");
}

async function main() {
  await testDebounceDelaysRecomputation();
  testBuildPaginatedUrlRequestsOnlyOnePageAtATime();
  testBuildPaginatedUrlHandlesExistingQueryString();
  testFormatBytes();
  console.log("shared.test.js: all assertions passed");
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
