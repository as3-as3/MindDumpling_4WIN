# MindDumpling Desktop (Windows 4WIN) 🧠💎

Professional Mind Mapping application for Windows, built with **Compose Multiplatform** and **Kotlin**. This is the desktop counterpart to the MindDumpling Android ecosystem.

## 🚀 Quick Start
- **Download**: Download the [MindDumpling-1.0.0.msi](MindDumpling-1.0.0.msi) directly from this repository.
- **Portable Version**: Extract the ZIP and run `MindDumplingPortable.exe`. No installation required.
- **MSI Installer**: Use the `.msi` file inside the zip for a standard Windows installation with Start Menu integration.

## 🚀 Release Status: v1.0.0 (Stable Alpha)
This version is production-ready for desktop use. 
- **Platform**: Windows 10/11
- **Compatibility**: Fixed JVM toolchain (Java 17) and Fat-Dependency bundling for standalone execution.

## ✨ Key Features
- **High-Fidelity Export**: Support for PNG, PDF, and SVG with professional PESR-Legends and metadata.
- **Sanity Bounds**: Smart coordinate capping to prevent resource exhaustion during massive exports.
- **Print Parity**: Automatic light-background enforcement for high-quality printing.
- **Deep Integration**: Seamlessly manage nodes, relationships, and cognitive maps.

## 📸 Visual Previews
<p align="center">
  <img src="Screenshots%20Previews/dark2.png" width="45%" />
  <img src="Screenshots%20Previews/light-mode-ui.png" width="45%" />
</p>
<p align="center">
  <img src="Screenshots%20Previews/dark5.png" width="30%" />
  <img src="Screenshots%20Previews/dark1.png" width="30%" />
  <img src="Screenshots%20Previews/Export%20Sample.png" width="30%" />
</p>

## 📱 Mobile Companion
This project is part of the MindDumpling ecosystem. You can find the Android version here:
👉 **[MindDumpling Android Repo](https://github.com/as3-as3/MindDumpling_Android)**

## 🛠️ Build System (Gold-Standard)
The project uses a custom, decoupled build strategy to ensure stability on Windows:
1. Fat-JAR compilation via Gradle.
2. Manual MSI/Portable packaging via `jlink` and `jpackage`.

## 📄 License
Licensed under the **Apache License 2.0**. See `LICENSE` and `NOTICE` for details.

---
*Developed by NeuroDumpling 2026*
