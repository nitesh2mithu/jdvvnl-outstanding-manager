<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Platform"/>
  <img src="https://img.shields.io/badge/Language-Kotlin_2.3-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/UI-Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpack-compose&logoColor=white" alt="Compose"/>
  <img src="https://img.shields.io/badge/License-MIT-F5C518?style=for-the-badge" alt="License"/>
  <img src="https://img.shields.io/badge/Offline-First-E91E63?style=for-the-badge" alt="Offline-First"/>
</p>

# ⚡ Outstanding Manager

**Offline-First Android App for JdVVNL Outstanding Consumer Dues Management**

A purpose-built Android application for Junior Engineers (JEs) of **JdVVNL** (Jodhpur Vidyut Vitran Nigam Ltd.) to import, clean, search, track, and export outstanding consumer dues — entirely offline, directly on their phone while working in the field.

> **Zero cloud. Zero login. Zero internet required.**
> Import your monthly JdVVNL `.xls` report → Clean → Track → Export PDF. Done.

---

## 📸 Screenshots

<!-- Add your screenshots here -->
<!-- <p align="center">
  <img src="screenshots/dashboard.png" width="220"/>
  <img src="screenshots/list.png" width="220"/>
  <img src="screenshots/detail.png" width="220"/>
  <img src="screenshots/pdf.png" width="220"/>
</p> -->

*Screenshots coming soon*

---

## ✨ Features at a Glance

| Feature | Description |
|---|---|
| 📥 **XLS Import** | Native binary `.xls` parser — no Apache POI, no cloud APIs. Pure Kotlin OLE2/BIFF8 decoder. |
| 🧹 **Two-Stage Cleaning** | Column picker + row cleaning with search, filter, feeder chip filtering, and "Select All Filtered" |
| 🔍 **Instant Search** | Real-time search across KNO, Account No, Name, Address, Mobile, Feeder |
| 💰 **Smart Filters** | Preset amount filters (₹5K/10K/25K/50K/1L+), custom range, feeder, status, tariff, phase, govt/smart |
| 📊 **Executive Dashboard** | 4 KPI cards, 5 amount buckets, status distribution — all tappable for filtered navigation |
| 📝 **Status Tracking** | 8 statuses (Visited, Payment Promised, Part Payment, Paid, Disconnected, Dispute, Others, No Status) |
| 📞 **One-Tap Dialer** | Direct phone call action from consumer details |
| 📄 **A4 PDF Export** | Portrait/Landscape auto-selection, column picker, repeating headers, Indian currency format, page numbers |
| 🗑️ **Removed Records** | Soft-delete with search, multi-select restore, and permanent deletion with confirmation |
| 🔒 **Complete Privacy** | SQLite local database — no server, no accounts, no tracking. Data never leaves your device. |

---

## 🏗️ Architecture

```
com.example.outstandingmanager/
├── data/
│   ├── db/             # SQLite database helper, indexed schema
│   ├── parser/         # Pure-Kotlin OLE2/BIFF8 XLS parser
│   └── repository/     # Repository pattern with StateFlow
├── export/             # A4 PDF vector rendering engine
├── model/              # Data models, sort options, filter state
├── theme/              # Material 3 theming
└── ui/
    ├── common/         # Shared composables
    ├── dashboard/      # Executive dashboard with KPI cards
    ├── details/        # Consumer detail + status/remark editing
    ├── empty/          # Empty state for first launch
    ├── export/         # PDF column selection + export flow
    ├── importflow/     # XLS import → preview → column clean → row clean
    ├── list/           # Main outstanding list with search/filter/sort
    ├── removed/        # Removed records management
    └── settings/       # Dataset management, reset, new import
```

**Pattern**: MVI / Clean Architecture with `StateFlow`, `ViewModel`, and SQLite Repository

---

## 📱 Daily Field Workflow

```
Download JdVVNL XLS → Open App → Import XLS → Preview
                                                  ↓
                              Remove unwanted columns → Remove other JE's consumers
                                                  ↓
                              Save cleaned dataset → Ready for field work
                                                  ↓
                              Search / Filter / Sort → Open consumer → Update Status / Remark
                                                  ↓
                              Dashboard overview → Export filtered records to A4 PDF → Share
```

---

## 🔧 XLS Parser — Deep Technical Details

JdVVNL exports reports in **Microsoft Excel 97-2004 (`.xls`)** format — a **BIFF8** stream inside an **OLE2 (CFBF)** container. This app includes a **pure-Kotlin binary parser** with zero external dependencies:

1. **OLE2 Container Unpacking** — Verifies the 8-byte magic header `0xD0CF11E0A1B11AE1`, reads MSAT/SAT, traverses directory entries to locate the `Workbook` stream
2. **BIFF8 Record Decoding** — Handles `SST` (Shared String Table), `LABELSST`, `LABEL`, `NUMBER` (IEEE 754), `RK`/`MULRK` (compressed integers/floats)
3. **Dynamic Header Detection** — Scans first 15 rows, scores each for header keywords (`kno`, `consumer`, `name`, `amount`, `dues`), auto-selects the best match
4. **Fallback Parsers** — Gracefully falls back to HTML/XML table parser or CSV/TSV for non-OLE2 files

---

## 🗄️ Database Schema

**SQLite** with indexed columns, ACID transactions, and `StateFlow` streaming:

<details>
<summary><strong>Click to expand schema</strong></summary>

```sql
-- Dataset metadata
CREATE TABLE datasets (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    file_name TEXT NOT NULL,
    total_records INTEGER NOT NULL,
    active_records INTEGER NOT NULL,
    removed_records INTEGER NOT NULL,
    created_at INTEGER NOT NULL,
    is_active INTEGER NOT NULL DEFAULT 1
);

-- Consumer records with performance indexes
CREATE TABLE consumers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    dataset_id INTEGER NOT NULL,
    source_row_id INTEGER NOT NULL,
    kno TEXT NOT NULL,
    account_no TEXT NOT NULL,
    consumer_name TEXT NOT NULL,
    address TEXT NOT NULL,
    -- ... 20+ fields including dynamic_fields JSON
    status TEXT NOT NULL DEFAULT '',
    remark TEXT NOT NULL DEFAULT '',
    last_updated INTEGER NOT NULL DEFAULT 0,
    is_removed INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY(dataset_id) REFERENCES datasets(id) ON DELETE CASCADE
);

-- Indexes for fast search & filter
CREATE INDEX idx_consumers_dataset_removed ON consumers(dataset_id, is_removed);
CREATE INDEX idx_consumers_kno ON consumers(kno);
CREATE INDEX idx_consumers_account ON consumers(account_no);
CREATE INDEX idx_consumers_name ON consumers(consumer_name);
CREATE INDEX idx_consumers_mobile ON consumers(mobile_no);
CREATE INDEX idx_consumers_feeder ON consumers(feeder_name);
CREATE INDEX idx_consumers_amount ON consumers(outstanding_amount);
CREATE INDEX idx_consumers_status ON consumers(status);
```

</details>

---

## 📄 A4 PDF Generation

Built-in vector rendering using Android's `PdfDocument` API — **zero cloud dependency**:

- **Dimensions**: A4 Portrait `595×842pt` / Landscape `842×595pt` (72 DPI)
- **Auto-orientation**: Recommends landscape when >5 columns selected
- **Formatting**: Indian currency (`₹17,65,872`), right-aligned numbers, alternating row backgrounds
- **Headers**: Repeating table headers on every page, filter summary on page 1
- **Sharing**: Via Android Share Sheet (`FileProvider`) — WhatsApp, Gmail, Print

---

## 🚀 Getting Started

### Prerequisites

| Requirement | Version |
|---|---|
| JDK | 17+ (tested with OpenJDK 25 LTS) |
| Android SDK | Platform 34 or 36 |
| Build-Tools | 36.0.0 |

### Install Pre-Built APK

```bash
# Via ADB
adb install -r app/build/outputs/apk/release/app-release.apk
```

Or transfer the APK via WhatsApp / Google Drive / USB and tap to install.

### Build from Source

```bash
# Clone the repository
git clone https://github.com/nitesh2mithu/jdvvnl-outstanding-manager.git
cd jdvvnl-outstanding-manager

# Run unit tests
./gradlew test --no-configuration-cache

# Build debug APK
./gradlew assembleDebug --no-configuration-cache

# Build signed release APK
./gradlew assembleRelease --no-configuration-cache
```

---

## 🧪 Test Suite

All tests pass — validates parsing, formatting, filtering, and sorting:

```
> Task :app:testDebugUnitTest
BUILD SUCCESSFUL in 1s
```

| Test Class | What It Validates |
|---|---|
| `XlsParserTest` | OLE2 decoding, header detection (row 3), 32 columns, 1,145 rows parsed |
| `CurrencyFormatterTest` | Indian format `₹17,65,872`, negatives, zero, commas, decimals |
| `FilterAndSortTest` | Amount presets, compound filters, multi-field search, all 8 sort modes |

---

## 🛠️ Tech Stack

| Component | Technology |
|---|---|
| **Language** | Kotlin 2.3.20 |
| **UI** | Jetpack Compose (BOM 2026.03.01) + Material Design 3 |
| **Architecture** | MVI / Clean Architecture |
| **State** | StateFlow + ViewModel |
| **Database** | SQLite with indexed columns |
| **Navigation** | AndroidX Navigation 3 |
| **PDF** | Android PdfDocument (vector rendering) |
| **File I/O** | Storage Access Framework (SAF) |
| **Build** | Android Gradle Plugin 9.0.1, Gradle 9.1.0 |
| **Target** | Android 14/15 (API 34/36) |

---

## 📁 Project Structure

```
jdvvnl-outstanding-manager/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/outstandingmanager/   # Application source
│   │   │   ├── assets/sample.xls                      # Bundled sample dataset
│   │   │   └── res/                                   # Resources, icons, XML configs
│   │   └── test/                                      # Unit tests
│   └── build.gradle.kts                               # App-level build config
├── gradle/
│   └── libs.versions.toml                             # Version catalog
├── build.gradle.kts                                   # Project-level build config
├── settings.gradle.kts                                # Gradle settings
├── CONTRIBUTING.md                                    # Contribution guidelines
├── CODE_OF_CONDUCT.md                                 # Community standards
├── LICENSE                                            # MIT License
└── README.md                                          # This file
```

---

## 🤝 Contributing

Contributions are welcome! Please read the [Contributing Guide](CONTRIBUTING.md) and [Code of Conduct](CODE_OF_CONDUCT.md) before submitting a pull request.

---

## 📄 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

---

<p align="center">
  <strong>Built with ❤️ by <a href="https://github.com/nitesh2mithu">nitesh2mithu</a></strong>
  <br/>
  <sub>A practical field utility for JdVVNL Junior Engineers</sub>
</p>
