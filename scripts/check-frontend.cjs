// Run with `node scripts/check-frontend.cjs`; uses only Node's built-in modules.
const fs = require('fs');
const path = require('path');
const vm = require('vm');
const root = 'src/main/resources/templates';
let errors = 0, count = 0;
const fail = (file, message) => { errors++; console.error(file + ': ' + message); };
for (const file of fs.readdirSync(root, { recursive: true }).filter(f => f.endsWith('.html'))) {
    count++;
    let source = fs.readFileSync(path.join(root, file), 'utf8');
    for (const match of source.matchAll(/<script\b[^>]*>([\s\S]*?)<\/script>/g)) {
        try { new vm.Script(match[1], { filename: file }); }
        catch (error) { fail(file, 'JavaScript syntax: ' + error.message); }
    }
    source = source.replace(/<!--[\s\S]*?-->|<script\b[\s\S]*?<\/script>|<style\b[\s\S]*?<\/style>/g, '');
    const ids = new Set();
    for (const match of source.matchAll(/(?<![\w:-])id="([^"]+)"/g)) {
        if (ids.has(match[1])) fail(file, 'Duplicate id: ' + match[1]);
        ids.add(match[1]);
    }
    for (const tag of ['head', 'body', 'div', 'form', 'footer']) {
        let depth = 0;
        for (const match of source.matchAll(new RegExp('<(/?)' + tag + '\\b[^>]*>', 'g'))) {
            depth += match[1] ? -1 : 1;
            if (depth < 0) { fail(file, 'Unexpected closing ' + tag); break; }
        }
        if (depth > 0) fail(file, 'Unclosed ' + tag + ': ' + depth);
    }
    if (/<head\b/.test(source) && !source.includes('fragments/floral :: head') && !/name="viewport"/.test(source)) fail(file, 'Missing viewport');
}
for (const file of fs.readdirSync('src/main/resources/static/js').filter(f => f.endsWith('.js'))) {
    try { new vm.Script(fs.readFileSync('src/main/resources/static/js/' + file, 'utf8'), { filename: file }); }
    catch (error) { fail(file, 'JavaScript syntax: ' + error.message); }
}
const css = fs.readFileSync('src/main/resources/static/css/floral-theme.css', 'utf8').replace(/\/\*[\s\S]*?\*\//g, '').replace(/"[^"\n]*"|'[^'\n]*'/g, '');
let braces = 0;
for (const ch of css) { if (ch === '{') braces++; else if (ch === '}') braces--; if (braces < 0) break; }
if (braces !== 0) fail('floral-theme.css', 'Unbalanced braces: ' + braces);
console.log(`Checked ${count} templates and all local JavaScript; ${errors} errors.`);
process.exitCode = errors ? 1 : 0;
