#!/usr/bin/env node

import { existsSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { spawnSync } from 'node:child_process';

const projectDirectory = process.argv[2] ? resolve(process.argv[2]) : undefined;
if (!projectDirectory || process.argv.length !== 3) {
  console.error('Usage: node validate.mjs <generated-project>');
  process.exit(1);
}
if (!existsSync(resolve(projectDirectory, 'package.json'))) {
  console.error(`package.json was not found in ${projectDirectory}`);
  process.exit(1);
}

const packageJson = JSON.parse(readFileSync(resolve(projectDirectory, 'package.json'), 'utf8'));
const dependencies = { ...packageJson.dependencies, ...packageJson.devDependencies };
const requiredDependencies = ['tailwindcss', '@tailwindcss/postcss', 'postcss'];
const missingDependencies = requiredDependencies.filter((dependency) => !dependencies[dependency]);
if (missingDependencies.length > 0) {
  console.error(`Missing Tailwind dependencies: ${missingDependencies.join(', ')}`);
  process.exit(1);
}
if (!existsSync(resolve(projectDirectory, '.postcssrc.json'))) {
  console.error('Tailwind PostCSS configuration (.postcssrc.json) was not found.');
  process.exit(1);
}
const styles = readFileSync(resolve(projectDirectory, 'src', 'styles.scss'), 'utf8');
if (!styles.includes("@use 'tailwindcss';")) {
  console.error("src/styles.scss must import Tailwind with @use 'tailwindcss';");
  process.exit(1);
}

const command = process.platform === 'win32' ? 'npm.cmd' : 'npm';
const result = spawnSync(command, ['run', 'build'], {
  cwd: projectDirectory,
  stdio: 'inherit',
  shell: process.platform === 'win32',
});

if (result.status !== 0) {
  console.error(`Build failed with exit code ${result.status ?? 'unknown'}.`);
  process.exit(result.status ?? 1);
}

console.log('Build completed successfully. No tests, server, API, or HTTP request were run.');
