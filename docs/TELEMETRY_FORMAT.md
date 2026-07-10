# Telemetry Format Plan v0.1

No high-volume writer is implemented before Phase 3.

The planned file is a chunked little-endian trajectory container, not Java serialization and not JSON-only. File header contains magic `SAWBOTTRJ`, format version, observation/action contract IDs, endianness marker, episode metadata length, and header checksum. Each compressed chunk contains bounded policy-step records and an independent CRC32C. A trailing index/footer is optional; interrupted files remain recoverable by scanning valid chunks.

Each policy step associates observation sequence, action source (`HUMAN`, `MODEL`, `TEACHER`, `SCRIPTED_OPPONENT`), source identifier, action, immediate events, bounded outcome window, reward terms when applicable, terminal reason, timing, and drop/corruption counters.

Small debug exports may use JSON. Screenshots, frames, OCR, arbitrary chat text, Java object graphs, and unbounded logs are prohibited.
