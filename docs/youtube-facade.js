/**
 * Lite YouTube Embed
 * Loads YouTube iframe only when user clicks play
 * Saves ~1MB of data on initial page load
 */
class LiteYouTubeEmbed extends HTMLElement {
  connectedCallback() {
    this.videoId = this.getAttribute('videoid');

    // Create thumbnail
    const posterUrl = `https://i.ytimg.com/vi/${this.videoId}/maxresdefault.jpg`;

    this.style.backgroundImage = `url("${posterUrl}")`;

    // Create play button
    const playBtn = document.createElement('button');
    playBtn.type = 'button';
    playBtn.classList.add('lty-playbtn');
    playBtn.setAttribute('aria-label', 'Play video');
    this.append(playBtn);

    // Load iframe on click
    this.addEventListener('click', () => this.addIframe());
  }

  addIframe() {
    const iframe = document.createElement('iframe');
    iframe.width = 560;
    iframe.height = 315;
    iframe.title = 'YouTube video player';
    iframe.allow = 'accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture';
    iframe.allowFullscreen = true;
    iframe.src = `https://www.youtube-nocookie.com/embed/${this.videoId}?autoplay=1`;

    this.classList.add('lyt-activated');
    this.innerHTML = '';
    this.append(iframe);
  }
}

// Register custom element
customElements.define('lite-youtube', LiteYouTubeEmbed);
