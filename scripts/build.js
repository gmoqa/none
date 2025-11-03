#!/usr/bin/env node

const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

console.log(`
╔══════════════════════════════════════════════════════════╗
║          🚀 None Website Build Process                   ║
╚══════════════════════════════════════════════════════════╝
`);

function run(command, description) {
  console.log(`\n▶️  ${description}...`);
  try {
    execSync(command, { stdio: 'inherit' });
    console.log(`✅ ${description} completed`);
  } catch (error) {
    console.error(`❌ ${description} failed:`, error.message);
    process.exit(1);
  }
}

function checkDependencies() {
  console.log('\n📦 Checking dependencies...');
  const packageJson = require('../package.json');
  const deps = Object.keys(packageJson.devDependencies || {});

  try {
    require.resolve('sharp');
    console.log('✅ All dependencies installed');
  } catch {
    console.log('📥 Installing dependencies...');
    run('npm install', 'Install dependencies');
  }
}

async function main() {
  // Check dependencies
  checkDependencies();

  // Run optimization steps
  run('node scripts/optimize-images.js', 'Optimize images');
  run('npm run optimize:css', 'Optimize CSS');

  // Update HTML files to use minified CSS
  console.log('\n🔄 Updating HTML files to use optimized CSS...');
  const docsDir = path.join(__dirname, '..', 'docs');

  function updateHtmlFiles(dir) {
    const entries = fs.readdirSync(dir, { withFileTypes: true });

    for (const entry of entries) {
      const fullPath = path.join(dir, entry.name);

      if (entry.isDirectory() && entry.name !== 'assets') {
        updateHtmlFiles(fullPath);
      } else if (entry.name.endsWith('.html')) {
        let content = fs.readFileSync(fullPath, 'utf8');
        const updated = content.replace(
          /href="(\.\.\/)?styles\.css"/g,
          'href="$1styles.min.css"'
        );
        if (content !== updated) {
          fs.writeFileSync(fullPath, updated);
          console.log(`  ✓ ${path.relative(docsDir, fullPath)}`);
        }
      }
    }
  }

  updateHtmlFiles(docsDir);

  console.log(`
╔══════════════════════════════════════════════════════════╗
║          ✨ Build Complete!                              ║
╚══════════════════════════════════════════════════════════╝

Next steps:
  1. Test locally: npm run serve
  2. Run Lighthouse: npm run lighthouse
  3. Commit changes: git add . && git commit -m "chore: optimize assets"
  4. Deploy: git push origin main
`);
}

main().catch(console.error);
