#!/usr/bin/env node

const sharp = require('sharp');
const fs = require('fs').promises;
const path = require('path');

const IMAGE_CONFIGS = {
  flags: { width: 20, height: 20, quality: 90 },
  logos: { width: 40, height: 40, quality: 90 },
  screenshots: { width: 800, quality: 85 }
};

async function optimizeImage(filePath, config) {
  try {
    const buffer = await fs.readFile(filePath);
    const image = sharp(buffer);
    const metadata = await image.metadata();

    let pipeline = image;

    if (config.width && config.height) {
      pipeline = pipeline.resize(config.width, config.height, {
        fit: 'cover',
        position: 'center'
      });
    } else if (config.width) {
      pipeline = pipeline.resize(config.width, null, {
        fit: 'inside',
        withoutEnlargement: true
      });
    }

    pipeline = pipeline.png({
      quality: config.quality || 85,
      compressionLevel: 9,
      adaptiveFiltering: true,
      palette: true
    });

    const optimized = await pipeline.toBuffer();

    const originalSize = buffer.length;
    const optimizedSize = optimized.length;
    const saved = originalSize - optimizedSize;
    const percent = ((saved / originalSize) * 100).toFixed(1);

    if (optimizedSize < originalSize) {
      await fs.writeFile(filePath, optimized);
      console.log(`✅ ${path.basename(filePath)}: ${formatBytes(originalSize)} → ${formatBytes(optimizedSize)} (${percent}% saved)`);
      return { saved, percent };
    } else {
      console.log(`⏭️  ${path.basename(filePath)}: already optimized`);
      return { saved: 0, percent: 0 };
    }
  } catch (error) {
    console.error(`❌ Error optimizing ${filePath}:`, error.message);
    return { saved: 0, percent: 0 };
  }
}

async function findImages(dir, pattern) {
  const entries = await fs.readdir(dir, { withFileTypes: true });
  const images = [];

  for (const entry of entries) {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      images.push(...await findImages(fullPath, pattern));
    } else if (entry.name.match(pattern)) {
      images.push(fullPath);
    }
  }

  return images;
}

function formatBytes(bytes) {
  if (bytes < 1024) return bytes + 'B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + 'KB';
  return (bytes / (1024 * 1024)).toFixed(1) + 'MB';
}

async function main() {
  console.log('🖼️  Image Optimization Started\n');

  const docsDir = path.join(__dirname, '..', 'docs');
  let totalSaved = 0;
  let filesProcessed = 0;

  // Optimize flag images
  console.log('📁 Optimizing flag images...');
  const flags = await findImages(path.join(docsDir, 'assets', 'flags'), /\.png$/);
  for (const flag of flags) {
    const result = await optimizeImage(flag, IMAGE_CONFIGS.flags);
    totalSaved += result.saved;
    filesProcessed++;
  }

  // Optimize logos
  console.log('\n📁 Optimizing logos...');
  const logoFiles = ['none_white.png', 'logo.png'];
  for (const logoFile of logoFiles) {
    const logoPath = path.join(docsDir, 'assets', logoFile);
    try {
      await fs.access(logoPath);
      const result = await optimizeImage(logoPath, IMAGE_CONFIGS.logos);
      totalSaved += result.saved;
      filesProcessed++;
    } catch {
      // File doesn't exist, skip
    }
  }

  console.log(`\n✨ Optimization Complete!`);
  console.log(`📊 Files processed: ${filesProcessed}`);
  console.log(`💾 Total saved: ${formatBytes(totalSaved)}`);
}

main().catch(console.error);
