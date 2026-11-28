# NextFam Client 5.0 (Fabric, Minecraft 1.21.11, Java 21)

Build: push ke GitHub -> Actions -> artifact NextFam-Client (NextFam-Client.jar).
Pakai: taruh jar + Fabric API 1.21.11 di folder mods.
Menu: Right Shift   |   Pack Studio: Right Ctrl
Config: config/nextfam/ (hud-layout.json, hud-settings.json, themes.json, animations.json, keybinds.json, cosmetics.json, crosshair.json, profiles/, capes/)
Cape custom: taruh file .png di config/nextfam/capes/ lalu pilih di menu: Cape > OPTIONS.
Font: Poppins (SIL OFL 1.1), lihat assets/nextfam/LICENSE-Poppins.txt

## 5.2
- Logo NextFam untuk pemain NextFam lain: butuh internet. Default memakai topik publik ntfy.sh (tanpa server).
  Server sendiri: jalankan server/presence_server.py dan set config/nextfam/presence.json -> {"mode":"custom","url":"https://..."}
- Cape resmi Mojang (diunduh dari textures.minecraft.net) dan impor cape dari URL.
- Pose blok pedang 1.7, ukuran totem di tangan, Fullbright / Night Vision.
