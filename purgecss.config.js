module.exports = {
  content: [
    './docs/**/*.html',
    './docs/**/*.js'
  ],
  css: ['./docs/styles.css'],
  output: './docs/styles.min.css',
  safelist: {
    standard: [
      // Language selector
      'language-dropdown',
      'language-option',
      'active',
      // Dynamic classes
      /^lottie/,
      /^hero/,
      /^animate/,
      // YouTube embed
      /^yt/,
      /^ytp/
    ],
    deep: [
      /lottie/,
      /hero/
    ],
    greedy: []
  },
  defaultExtractor: content => content.match(/[\w-/:]+(?<!:)/g) || [],
  fontFace: true,
  keyframes: true,
  variables: true
}
