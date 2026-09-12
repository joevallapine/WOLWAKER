# Despertar PC (WolWaker)

App Android muy simple para encender tu PC por WiFi mediante **Wake-on-LAN**
(manda el "magic packet" a la dirección MAC de tu PC) cuando está en
hibernación, suspensión o apagada.

Funciona **solo cuando el celular está conectado a la misma red WiFi que la PC**.

## Requisitos en la PC (una sola vez)

1. **BIOS/UEFI**: entra a la BIOS al encender la PC y activa una opción
   parecida a "Wake on LAN", "Power On by PCI-E/PCIE" o "Resume by PCI-E
   device" (el nombre varía según la placa madre).
2. **Windows – Administrador de dispositivos**:
   - Abre el Administrador de dispositivos → Adaptadores de red → tu
     tarjeta de red (la que usas por cable o WiFi, según cuál quieras
     usar para despertar).
   - Click derecho → Propiedades → pestaña **Opciones avanzadas**:
     activa "Wake on Magic Packet" (y "Wake on Pattern Match" si existe).
   - Pestaña **Administración de energía**: marca "Permitir que este
     dispositivo reactive el equipo" y "Solo permitir que un paquete
     mágico reactive el equipo".
3. Ten en cuenta que **Wake-on-LAN funciona mejor por cable Ethernet**
   que por WiFi; muchas tarjetas WiFi no soportan mantener el radio
   activo en hibernación/apagado. Si tu PC solo tiene WiFi, revisa si tu
   adaptador WiFi específico soporta WoWLAN.
4. Anota la **dirección MAC** de esa tarjeta de red (en Windows:
   `ipconfig /all`, busca "Dirección física").

## Compilar la app

### Opción A: Android Studio (recomendado si ya lo tienes instalado)

1. Abre Android Studio → "Open" → selecciona esta carpeta (`WolWaker`).
2. Deja que sincronice Gradle (la primera vez descarga dependencias, así
   que necesita internet).
3. Conecta tu celular por USB con la depuración USB activada, o usa un
   emulador, y dale a **Run ▶**. También puedes generar el APK con
   **Build → Build Bundle(s) / APK(s) → Build APK(s)**; el archivo queda
   en `app/build/outputs/apk/debug/app-debug.apk`.

### Opción B: GitHub Actions (sin instalar nada, compila en la nube)

Este proyecto ya incluye un workflow (`.github/workflows/build-apk.yml`)
que compila el APK automáticamente:

1. Crea un repositorio nuevo en GitHub y sube esta carpeta (por
   ejemplo arrastrando los archivos en la web de GitHub, o con
   `git init && git add . && git commit -m "init" && git push`).
2. Ve a la pestaña **Actions** del repositorio: el workflow
   "Build debug APK" se ejecuta solo. Espera a que termine (unos 2-4
   minutos).
3. Entra a esa ejecución y descarga el artefacto
   **DespertarPC-debug-apk** (es un .zip que contiene el `app-debug.apk`).
4. Pasa el APK a tu celular (por Drive, correo, cable USB, etc.) y
   ábrelo para instalarlo. Android te pedirá permitir "instalar apps de
   orígenes desconocidos" la primera vez.

## Usar la app

1. Ábrela, escribe la **MAC** de tu PC (formato `AA:BB:CC:DD:EE:FF`).
2. Normalmente no hace falta tocar el campo de IP de broadcast: se
   autocompleta según tu WiFi actual (si falla, deja `255.255.255.255`).
3. Toca **Guardar datos** una vez.
4. Cuando quieras encender la PC, abre la app (conectado a la misma
   WiFi) y toca **⚡ Encender PC**. Espera unos 15-30 segundos.

## Widget de pantalla de inicio

La app incluye un widget con un solo botón, para encender la PC sin
tener que abrir la app:

1. Primero abre la app al menos una vez y guarda la MAC (paso anterior).
2. Mantén presionada la pantalla de inicio de tu celular → **Widgets**.
3. Busca **Despertar PC** en la lista y arrastra el widget "⚡ Encender PC"
   a tu pantalla.
4. Tócalo para mandar el magic packet directamente. El botón muestra
   "Enviando…" y luego "Enviado ✓" o el error, y vuelve a su texto normal
   a los pocos segundos.

Si cambias la MAC dentro de la app, el widget se actualiza solo.

## Nota sobre alcance

Esta versión solo funciona dentro de la misma red WiFi local. Para
encenderla desde fuera de casa (datos móviles) se necesita configurar
además el router (port forwarding del puerto UDP 9, o una VPN hacia tu
red doméstica) — es un paso aparte, avísame si lo quieres agregar.
