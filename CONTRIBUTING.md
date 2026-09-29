# Contributing to Outstanding Manager

Thank you for your interest in contributing! This guide will help you get started.

## 🚀 Getting Started

### Prerequisites

| Requirement | Version |
|---|---|
| **JDK** | 17 or higher |
| **Android SDK** | Platform 34 or 36 |
| **Build-Tools** | 36.0.0 |
| **Gradle** | 9.1.0 (bundled via wrapper) |

### Setting Up the Development Environment

1. **Clone the repository**
   ```bash
   git clone https://github.com/nitesh2mithu/jdvvnl-outstanding-manager.git
   cd jdvvnl-outstanding-manager
   ```

2. **Open in Android Studio**
   - File → Open → select the cloned folder
   - Let Gradle sync complete

3. **Build and run**
   ```bash
   ./gradlew assembleDebug --no-configuration-cache
   ```

## 📋 How to Contribute

### Reporting Bugs

- Use the [Issue Tracker](https://github.com/nitesh2mithu/jdvvnl-outstanding-manager/issues)
- Include steps to reproduce, expected behavior, and actual behavior
- Attach screenshots if applicable
- Mention your Android version and device model

### Suggesting Features

- Open an issue with the **`enhancement`** label
- Describe the use case and how it benefits field operations
- Keep suggestions practical — this is a field utility, not an enterprise app

### Submitting Code Changes

1. **Fork** the repository
2. **Create a feature branch** from `main`:
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. **Make your changes** following the code style below
4. **Run tests** to ensure nothing breaks:
   ```bash
   ./gradlew test --no-configuration-cache
   ```
5. **Commit** with a clear message:
   ```bash
   git commit -m "feat: add feeder-wise summary to dashboard"
   ```
6. **Push** and open a **Pull Request** against `main`

## 🎨 Code Style

- **Language**: Kotlin (follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html))
- **UI**: Jetpack Compose with Material 3
- **Architecture**: MVI / Clean Architecture with `StateFlow` + Repository pattern
- **Formatting**: Use Android Studio's default Kotlin formatter (`Ctrl+Alt+L` / `Cmd+Option+L`)
- **Naming**: Descriptive names — avoid abbreviations except well-known ones (e.g., `KNO`, `SAF`)

## 🧪 Testing

- All new features should include unit tests where practical
- Run the full test suite before submitting:
  ```bash
  ./gradlew test --no-configuration-cache
  ```

## 📝 Commit Message Convention

Use [Conventional Commits](https://www.conventionalcommits.org/):

| Prefix | Usage |
|---|---|
| `feat:` | New feature |
| `fix:` | Bug fix |
| `docs:` | Documentation only |
| `style:` | Formatting, no logic change |
| `refactor:` | Code restructuring |
| `test:` | Adding or fixing tests |
| `chore:` | Build process, tooling |

## 📄 License

By contributing, you agree that your contributions will be licensed under the [MIT License](LICENSE).
