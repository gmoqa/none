#!/usr/bin/env node

const fs = require('fs').promises;
const path = require('path');
const { PurgeCSS } = require('purgecss');

async function optimizeCSS() {
  console.log('🎨 CSS Optimization Started\n');

  const cssPath = path.join(__dirname, '..', 'docs', 'styles.css');
  const outputPath = path.join(__dirname, '..', 'docs', 'styles.min.css');

  // Read original CSS
  const originalCSS = await fs.readFile(cssPath, 'utf8');
  const originalSize = Buffer.byteLength(originalCSS);

  console.log(`📄 Original CSS: ${formatBytes(originalSize)}`);

  // Run PurgeCSS
  console.log('🔄 Running PurgeCSS...');
  const purgeCSSResults = await new PurgeCSS().purge({
    content: [
      './docs/**/*.html',
      './docs/**/*.js'
    ],
    css: [{
      raw: originalCSS,
      extension: 'css'
    }],
    safelist: {
      standard: [
        'language-dropdown',
        'language-option',
        'active',
        'show',
        'lottie-player',
        'github-star-button',
        'github-star-button-page',
        'language-selector',
        'language-selector-page',
        'language-button',
        'flag-icon',
        'back-link',
        'skip-link'
      ],
      deep: [
        /^lottie/,
        /^hero/,
        /^fade/,
        /^animate/,
        /hover$/,
        /focus$/,
        /active$/
      ],
      greedy: [
        /data-/,
        /aria-/
      ]
    },
    fontFace: true,
    keyframes: true,
    variables: true
  });

  let purgedCSS = purgeCSSResults[0].css;
  const purgedSize = Buffer.byteLength(purgedCSS);

  console.log(`✂️  After PurgeCSS: ${formatBytes(purgedSize)} (${((originalSize - purgedSize) / originalSize * 100).toFixed(1)}% removed)`);

  // Additional minification: remove comments, extra whitespace
  purgedCSS = purgedCSS
    .replace(/\/\*[\s\S]*?\*\//g, '') // Remove comments
    .replace(/\s+/g, ' ') // Collapse whitespace
    .replace(/\s*([:;{}(),])\s*/g, '$1') // Remove space around punctuation
    .replace(/;}/g, '}') // Remove last semicolon in block
    .replace(/\s*!important/g, '!important')
    .trim();

  const finalSize = Buffer.byteLength(purgedCSS);

  console.log(`🗜️  After minification: ${formatBytes(finalSize)} (${((originalSize - finalSize) / originalSize * 100).toFixed(1)}% total reduction)`);

  // Write output
  await fs.writeFile(outputPath, purgedCSS);

  console.log(`\n✅ Optimized CSS written to: styles.min.css`);
  console.log(`💾 Total savings: ${formatBytes(originalSize - finalSize)}`);
}

function formatBytes(bytes) {
  if (bytes < 1024) return bytes + 'B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + 'KB';
  return (bytes / (1024 * 1024)).toFixed(1) + 'MB';
}

optimizeCSS().catch(console.error);
