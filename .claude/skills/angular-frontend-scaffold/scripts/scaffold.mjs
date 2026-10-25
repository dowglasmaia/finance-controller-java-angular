#!/usr/bin/env node

import { existsSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { mkdir, writeFile } from 'node:fs/promises';
import { basename, join, resolve } from 'node:path';
import { spawnSync } from 'node:child_process';

const usage = `Usage: node scaffold.mjs --output <parent-directory> --project-name <name> [--angular-cli @angular/cli@21]`;

function parseArguments(argv) {
  const options = { angularCli: '@angular/cli@21' };
  for (let index = 0; index < argv.length; index += 2) {
    const flag = argv[index];
    const value = argv[index + 1];
    if (!flag?.startsWith('--') || !value || value.startsWith('--')) throw new Error(usage);
    if (flag === '--output') options.output = value;
    else if (flag === '--project-name') options.projectName = value;
    else if (flag === '--angular-cli') options.angularCli = value;
    else throw new Error(usage);
  }
  if (!options.output || !options.projectName) throw new Error(usage);
  if (!/^[a-z][a-z0-9-]*$/.test(options.projectName)) {
    throw new Error('The project name must use lowercase letters, numbers, and hyphens, and start with a letter.');
  }
  if (!/^@angular\/cli@(2[1-9]|[3-9][0-9])(?:$|[./-])/.test(options.angularCli)) {
    throw new Error('The Angular CLI must be version 21 or later, for example @angular/cli@21.');
  }
  return options;
}

function run(command, args, cwd) {
  const result = spawnSync(command, args, { cwd, stdio: 'inherit', shell: process.platform === 'win32' });
  if (result.status !== 0) throw new Error(`${command} failed with exit code ${result.status ?? 'unknown'}.`);
}

function installTailwind(projectDirectory) {
  const npmCommand = process.platform === 'win32' ? 'npm.cmd' : 'npm';
  run(npmCommand, ['install', 'tailwindcss', '@tailwindcss/postcss', 'postcss'], projectDirectory);
}

async function replaceShell(projectDirectory) {
  const appDirectory = join(projectDirectory, 'src', 'app');
  rmSync(appDirectory, { recursive: true, force: true });
  await mkdir(join(appDirectory, 'core', 'http'), { recursive: true });
  await mkdir(join(appDirectory, 'features', 'home', 'pages'), { recursive: true });

  const files = {
    'src/main.ts': `import { bootstrapApplication } from '@angular/platform-browser';\nimport { appConfig } from './app/app.config';\nimport { App } from './app/app';\n\nbootstrapApplication(App, appConfig).catch((error: unknown) => console.error(error));\n`,
    'src/app/app.ts': `import { ChangeDetectionStrategy, Component } from '@angular/core';\nimport { RouterOutlet } from '@angular/router';\n\n@Component({\n  selector: 'app-root',\n  imports: [RouterOutlet],\n  templateUrl: './app.html',\n  styleUrl: './app.scss',\n  changeDetection: ChangeDetectionStrategy.OnPush,\n})\nexport class App {}\n`,
    'src/app/app.html': `<main class="app-shell"><router-outlet /></main>\n`,
    'src/app/app.scss': `:host { display: block; min-height: 100vh; }\n\n.app-shell { min-height: 100vh; }\n`,
    'src/app/app.config.ts': `import { ApplicationConfig, provideZonelessChangeDetection } from '@angular/core';\nimport { provideHttpClient, withInterceptors } from '@angular/common/http';\nimport { provideRouter } from '@angular/router';\nimport { apiErrorInterceptor } from './core/http/api-error.interceptor';\nimport { routes } from './app.routes';\n\nexport const appConfig: ApplicationConfig = {\n  providers: [\n    provideZonelessChangeDetection(),\n    provideRouter(routes),\n    provideHttpClient(withInterceptors([apiErrorInterceptor])),\n  ],\n};\n`,
    'src/app/app.routes.ts': `import { Routes } from '@angular/router';\n\nexport const routes: Routes = [\n  {\n    path: '',\n    loadChildren: () => import('./features/home/home.routes').then((module) => module.HOME_ROUTES),\n  },\n  { path: '**', redirectTo: '' },\n];\n`,
    'src/app/core/http/api-base-url.token.ts': `import { InjectionToken } from '@angular/core';\n\nexport const API_BASE_URL = new InjectionToken<string>('API_BASE_URL', {\n  factory: () => '/api',\n});\n`,
    'src/app/core/http/api-error.ts': `export interface ApiError {\n  readonly status: number;\n  readonly message: string;\n}\n\nexport function toApiError(status: number, message: string): ApiError {\n  return { status, message };\n}\n`,
    'src/app/core/http/api-error.interceptor.ts': `import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';\nimport { catchError, throwError } from 'rxjs';\nimport { toApiError } from './api-error';\n\nexport const apiErrorInterceptor: HttpInterceptorFn = (request, next) =>\n  next(request).pipe(\n    catchError((error: unknown) => {\n      if (error instanceof HttpErrorResponse) {\n        return throwError(() => toApiError(error.status, error.message));\n      }\n      return throwError(() => error);\n    }),\n  );\n`,
    'src/app/features/home/home.routes.ts': `import { Routes } from '@angular/router';\nimport { HomePage } from './pages/home-page';\n\nexport const HOME_ROUTES: Routes = [\n  { path: '', component: HomePage, title: 'Finance Controller' },\n];\n`,
    'src/app/features/home/pages/home-page.ts': `import { ChangeDetectionStrategy, Component } from '@angular/core';\n\n@Component({\n  selector: 'app-home-page',\n  templateUrl: './home-page.html',\n  styleUrl: './home-page.scss',\n  changeDetection: ChangeDetectionStrategy.OnPush,\n})\nexport class HomePage {}\n`,
    'src/app/features/home/pages/home-page.html': `<section class="mx-auto grid min-h-screen max-w-3xl place-items-center px-6 py-12" aria-labelledby="home-title">\n  <div class="space-y-4">\n    <p class="text-sm font-bold uppercase tracking-[0.08em] text-teal-700">Finance Controller</p>\n    <h1 id="home-title" class="text-3xl font-bold text-slate-900 sm:text-4xl">Frontend pronto para evoluir</h1>\n    <p class="text-lg leading-8 text-slate-700">O shell Angular foi criado e aguarda contratos OpenAPI aprovados para os próximos módulos.</p>\n  </div>\n</section>\n`,
    'src/app/features/home/pages/home-page.scss': `:host { display: block; }\n`,
    'src/app/features/home/doc.md': `# Home\n\n## Responsabilidade\n\nFornecer o shell inicial da aplicação e confirmar que a navegação lazy funciona sem depender de uma API.\n\n## Contratos\n\nAinda não há operações OpenAPI neste módulo.\n\n## Próxima evolução\n\nSubstituir este exemplo somente por uma mudança especificada e com contrato aprovado.\n`,
    '.postcssrc.json': `${JSON.stringify({ plugins: { '@tailwindcss/postcss': {} } }, null, 2)}\n`,
    'src/styles.scss': `@use 'tailwindcss';\n\n:root {\n  font-family: Inter, system-ui, sans-serif;\n}\n\nbody {\n  margin: 0;\n}\n`,
  };

  await Promise.all(Object.entries(files).map(([relativePath, content]) => writeFile(join(projectDirectory, relativePath), content, 'utf8')));
}

async function updatePackageJson(projectDirectory) {
  const packagePath = join(projectDirectory, 'package.json');
  const packageJson = JSON.parse(readFileSync(packagePath, 'utf8'));
  packageJson.scripts = {
    start: 'ng serve --port 4200',
    build: 'ng build',
  };
  writeFileSync(packagePath, `${JSON.stringify(packageJson, null, 2)}\n`, 'utf8');
}

async function main() {
  const options = parseArguments(process.argv.slice(2));
  const outputDirectory = resolve(options.output);
  const projectDirectory = resolve(outputDirectory, options.projectName);
  if (basename(projectDirectory) !== options.projectName) throw new Error('The project name cannot escape the output directory.');
  if (existsSync(projectDirectory)) throw new Error(`Target directory already exists: ${projectDirectory}`);
  await mkdir(outputDirectory, { recursive: true });

  run('npx', [
    '--yes', options.angularCli, 'new', options.projectName,
    '--directory', options.projectName,
    '--routing', '--style=scss', '--standalone', '--strict', '--zoneless', '--ssr=false',
    '--skip-tests', '--skip-git', '--package-manager=npm', '--defaults', '--interactive=false', '--ai-config=none',
  ], outputDirectory);

  installTailwind(projectDirectory);
  await replaceShell(projectDirectory);
  await updatePackageJson(projectDirectory);
  console.log(`Angular frontend created at ${projectDirectory}`);
}

main().catch((error) => {
  console.error(error instanceof Error ? error.message : error);
  process.exitCode = 1;
});
