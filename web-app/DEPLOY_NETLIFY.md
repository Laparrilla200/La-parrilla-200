# Despliegue en Netlify — La Parrilla 200 Web (React + Vite + Firebase)

Esta carpeta (`/web-app`) contiene la versión web completa de **La Parrilla 200**, conectada en tiempo real a la misma base de datos Firestore del proyecto Firebase (`gen-lang-client-0261940484`), lista para compilar y subir a **Netlify**.

## 1. Compilar localmente (para verificar o subir carpeta `dist/`)

```bash
cd web-app
npm install
npm run build
```

Esto genera la carpeta lista para producción en `web-app/dist/`.

---

## 2. Subir a Netlify (3 formas)

### Opción A: Drag & Drop rápido en Netlify
1. Ejecuta `npm install && npm run build` dentro de `web-app/`.
2. Entra a [app.netlify.com](https://app.netlify.com/) → **Add new site** → **Deploy manually**.
3. Arrastra la carpeta `web-app/dist/` al recuadro de Netlify. ¡Listo!

### Opción B: Conectar repositorio GitHub en Netlify
1. En Netlify → **Add new site** → **Import an existing project**.
2. Configura:
   - **Base directory**: `web-app`
   - **Build command**: `npm run build`
   - **Publish directory**: `web-app/dist` (o `dist` si Netlify ya aplica el Base directory).
3. El archivo `netlify.toml` y `public/_redirects` ya están configurados para enrutamiento SPA (`/* -> /index.html 200`).

### Opción C: Netlify CLI
```bash
cd web-app
npm install
npm run build
npx netlify-cli deploy --prod --dir=dist
```

---

## 3. Nota importante sobre Asistente Gemini AI en la Web

- La sincronización de pedidos, cocina (KDS), inventario, menú, cierres y permisos de usuarios funciona inmediatamente con **Firebase Firestore** (base `ai-studio-android-laparril-1b643e62-cfb5-4b1c-96c5-d20e626d77dd`).
- Para usar **Firebase AI Logic (Gemini)** desde el dominio de Netlify:
  1. En **Firebase Console** (`gen-lang-client-0261940484`), registra una **Web App** si aún no existe y copia su `appId`.
  2. En Netlify → **Site configuration → Environment variables**, agrega `VITE_FIREBASE_APP_ID` con ese valor (ver `.env.example`) y vuelve a desplegar.
  3. Si tu proyecto tiene App Check obligatorio en Web, habilita **reCAPTCHA v3** en Firebase App Check para tu dominio `*.netlify.app`.
