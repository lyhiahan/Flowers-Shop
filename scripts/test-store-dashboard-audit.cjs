// Executes the actual admin chart initializer with a minimal DOM/Chart stub.
// This does not verify browser rendering, Chart.js, Bootstrap or CDN loading.
const fs = require('node:fs');
const vm = require('node:vm');
const source = fs.readFileSync('src/main/resources/templates/admin/dashboard.html', 'utf8');
const markup = source.replace(/<script\b[\s\S]*?<\/script>/g, '').replace(/<!--[\s\S]*?-->/g, '');
const ids = new Set([...markup.matchAll(/(?<![\w:-])id="([^"]+)"/g)].map(m => m[1]));
const inline = [...source.matchAll(/<script\b[^>]*>([\s\S]*?)<\/script>/g)]
  .map(m => m[1]).find(code => code.includes('var revData'));
const lookedUp = [];
function Chart(context, config) { this.config = config; this.options = config.options; this.update = () => {}; }
const document = {
  getElementById(id) {
    lookedUp.push(id);
    return ids.has(id) ? { getContext: () => ({}), classList: { toggle() {} }, addEventListener() {} } : null;
  }
};
try {
  vm.runInNewContext(inline, { document, Chart }, { filename: 'admin/dashboard.html', timeout: 1000 });
  console.log('Admin chart initializer completed.');
} catch (error) {
  console.error(`Admin chart initializer failed at #${lookedUp.at(-1)}: ${error.message}`);
  process.exitCode = 1;
}
