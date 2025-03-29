# NONE Landing Page

Landing page multiidioma para NONE - Aplicación de comunicación aumentativa para personas autistas.

## 🌍 Idiomas disponibles

- 🇪🇸 **Español**: [index.html](index.html) - Idioma principal
- 🇬🇧 **Inglés**: [en/index.html](en/index.html)
- 🇧🇷 **Portugués**: [pt/index.html](pt/index.html)
- 🇫🇷 **Francés**: [fr/index.html](fr/index.html)
- 🇩🇪 **Alemán**: [de/index.html](de/index.html)

## 📁 Estructura

```
docs/
├── index.html              # Página principal (español)
├── styles.css              # Estilos compartidos por todos los idiomas
├── script.js               # JavaScript compartido
├── CNAME                   # Configuración dominio noneapp.org
│
├── en/                     # Versión en inglés
│   └── index.html
├── pt/                     # Versión en portugués
│   └── index.html
├── fr/                     # Versión en francés
│   └── index.html
├── de/                     # Versión en alemán
│   └── index.html
│
└── assets/                 # Recursos compartidos
    ├── favicon.png         # Icono del sitio (32x32px)
    ├── og-image.png        # Imagen para redes sociales (1200x630px)
    ├── app-screenshot.png  # Screenshot principal (400x800px)
    ├── none.jpg            # Foto personal de Salvador
    └── screenshot-*.png    # Screenshots para galería
```

## 🎨 Características

- ✅ **5 idiomas completos** con selector en navbar
- ✅ **Diseño accesible**: Alto contraste, botones grandes, WCAG AAA
- ✅ **SEO optimizado**: Hreflang tags, Open Graph, Twitter Cards
- ✅ **Responsive**: Mobile-first, funciona en cualquier dispositivo
- ✅ **Galería interactiva** de screenshots
- ✅ **Formulario de contacto** (opcional con Formspree)
- ✅ **Sin dependencias**: HTML, CSS, JS vanilla

## 🔧 Recursos compartidos

### CSS (styles.css)
Todos los idiomas comparten el mismo archivo CSS. Incluye:
- Variables CSS para fácil personalización
- Diseño responsive con breakpoints
- Estilos para selector de idioma
- Componentes: navbar, hero, cards, formularios, modal
- Modo alto contraste y reduced motion

### JavaScript (script.js)
Funcionalidad compartida:
- Navegación móvil responsive
- Galería de screenshots con navegación por teclado
- Formulario de contacto con Formspree
- Smooth scroll
- Animaciones on-scroll
- Detección de idioma del navegador (opcional)

### Assets (assets/)
Todos los idiomas usan las mismas imágenes:
- Favicon
- Open Graph image (para compartir en redes)
- Screenshots de la app
- Foto personal

## 📝 Agregar nuevo idioma

Para agregar un nuevo idioma:

1. **Crear carpeta**: `mkdir docs/it` (ejemplo para italiano)
2. **Copiar HTML**: `cp docs/en/index.html docs/it/`
3. **Traducir contenido** en el nuevo HTML
4. **Actualizar meta tags**: `og:url`, `hreflang`, etc.
5. **Actualizar selector**: Agregar opción IT en todos los archivos HTML
6. **Actualizar hreflang**: Agregar tag `<link rel="alternate" hreflang="it">` en todos

## 🌐 URLs

### En producción (noneapp.org)
- Español: https://noneapp.org/
- Inglés: https://noneapp.org/en/
- Portugués: https://noneapp.org/pt/
- Francés: https://noneapp.org/fr/
- Alemán: https://noneapp.org/de/

### En desarrollo local
Abrir `index.html` directamente en el navegador o usar un servidor local:
```bash
# Python 3
python3 -m http.server 8000

# Node.js (si tienes http-server)
npx http-server docs -p 8000
```

## 📚 Documentación

Para más información consulta:
- [README_LANDING.md](../documentation/README_LANDING.md) - Guía rápida
- [LANDING_PAGE_SETUP.md](../documentation/LANDING_PAGE_SETUP.md) - Configuración GitHub Pages
- [MULTILANGUAGE_SETUP.md](../documentation/MULTILANGUAGE_SETUP.md) - Sistema multiidioma
- [DOMINIO_NONEAPP_SETUP.md](../documentation/DOMINIO_NONEAPP_SETUP.md) - Configuración DNS

## ⚡ GitHub Pages

Esta carpeta está configurada para servirse desde GitHub Pages:
- Settings > Pages > Source: main branch, `/docs` folder
- Dominio personalizado: noneapp.org (configurado en CNAME)
- SSL/HTTPS automático

## 🛠️ Mantenimiento

### Actualizar contenido
Edita los archivos HTML correspondientes y haz push:
```bash
git add docs/
git commit -m "Update landing page content"
git push
```

GitHub Pages se actualiza automáticamente en 1-2 minutos.

### Actualizar estilos o funcionalidad
Edita `styles.css` o `script.js` - afecta todos los idiomas:
```bash
git add docs/styles.css docs/script.js
git commit -m "Update styles/functionality"
git push
```

### Agregar imágenes
Coloca nuevas imágenes en `assets/` y actualiza las referencias HTML:
```bash
git add docs/assets/
git commit -m "Add new images"
git push
```

---

**Sitio web**: [noneapp.org](https://noneapp.org)
**Repositorio**: [github.com/gmoqa/none](https://github.com/gmoqa/none)
**Email**: gu.quinteros@gmail.com
