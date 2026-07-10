# Source Audit v0.1

This audit records architecture study only. No donor source has been copied into SawBotV1. Exact source commits must be pinned before any future line-level study or reuse.

| Repository/reference | Version/branch | MC version | Language/build | Licence | Build status | Relevant concepts | Unwanted areas/security concerns | Direct reuse decision |
|---|---|---|---|---|---|---|---|---|
| MinecraftForge/MinecraftForge + official Forge distribution | Forge `1.8.9-11.15.1.2318-1.8.9` | 1.8.9 | Java / Gradle | Forge/FML licences; review bundled notices | Official legacy recommended/latest artifact exists; clean build pending JDK 8 network lane | lifecycle, events, rendering, key bindings, config, mappings | legacy dependency availability; Minecraft/MCP redistribution constraints | Required platform dependency only; preserve notices |
| EssentialGG/Architectury-Loom | `0.10.0.5` | supports legacy Forge 1.8.9 | Java/Kotlin / Gradle | LGPL-3.0; dependency use only | Selected CI build plugin; online build pending | remapping, run configuration, legacy Forge resolution | plugin/repository availability and Gradle compatibility | Build dependency only; no source copied |
| lineargraph/Forge1.8.9Template | `master`, commit pin required before future copying | 1.8.9 | Java/Kotlin DSL / Gradle 8.8 | Unlicense/CC0 | Public maintained template; architecture studied | dual JDK lane, Loom/Pack200 configuration, remapped-JAR task | template includes optional mixin/DevAuth pieces that SawBot does not use | Build concepts independently adapted; no Java runtime source copied |
| cabaletta/baritone | release `v1.15.0` for current architecture study; historical 1.8 fork requires separate audit | modern versions | Java / Gradle | LGPL-3.0 | Active upstream, not a 1.8.9 runtime dependency | path cost decomposition, goal APIs, deterministic search tests, cancellation | public automation capability; version mismatch; copyleft obligations | Concepts only; no code copied; teachers clean-room implemented later |
| cl0ckworks/1.8baritone | `master`, exact commit **PIN BEFORE STUDY** | 1.8 | Java / Gradle | LGPL-3.0 | Unverified fork | historical naming/mapping clues, regression scenarios | tiny/unmaintained fork; supply-chain and correctness risk | Do not execute binaries; concepts/test cases only after commit/security review |
| fr1kin/ForgeHax | release `3.3.1-eca8932`; older tags separately pin | 1.12.2–1.16.5 across releases | Java / Gradle | MIT | Historical releases; not selected dependency | event organization, render-state isolation, command/config patterns | cheat modules, packet manipulation, unsafe scope contamination | No wholesale dependency; independently implement benign concepts only |
| CCBlueX/LiquidBounce | `nextgen` branch; exact commit **PIN BEFORE STUDY** | modern | Kotlin/Java / Gradle | GPL-3.0 | Active | module lifecycle, HUD diagnostics, entity selection test ideas | anti-cheat bypass and packet-manipulation modules; strong copyleft | No direct code reuse; concepts only |
| Wurst-Imperium/Wurst7 | `master`; exact commit **PIN BEFORE STUDY** | modern | Java / Gradle | GPL-3.0 | Active | settings UX, ESP rendering concepts, SafeWalk test-case discovery | cheat features, public-server use, strong copyleft | No direct code reuse; concepts only |
| dxxxxy/1.8.9ForgeTemplate | archived `master`; exact commit **PIN BEFORE ANY REUSE** | 1.8.9 | Java / Gradle 3.1 | repository licence not established in initial review | Repository warns its toolchain is deprecated/broken | historical wrapper/build troubleshooting only | archived, Forge artifacts removed/deprecated, unclear reuse licence | No code reuse; not a foundation donor |

## Donor review procedure

1. Record repository URL, exact commit, tag, subpath, and retrieval date.
2. Read root and file-level licences and dependency notices.
3. Build source in an isolated environment; never execute downloaded compiled clients.
4. Identify and exclude packet manipulation, bypass, authentication, obfuscated, native, and unexplained binary modules.
5. Write a concept description without copying implementation.
6. Implement from Minecraft/geometry fundamentals and SawBot contracts.
7. Add attribution only when legally required or direct reuse is explicitly approved.
8. Run forbidden-reference and licence scans before release.

## Initial references

- Official Forge 1.8.9 downloads: `https://files.minecraftforge.net/net/minecraftforge/forge/index_1.8.9.html`
- Architectury Loom: `https://github.com/architectury/architectury-loom`
- Legacy Forge template studied for build configuration: `https://github.com/lineargraph/Forge1.8.9Template`
- Baritone: `https://github.com/cabaletta/baritone`
- ForgeHax: `https://github.com/fr1kin/ForgeHax`
- LiquidBounce: `https://github.com/CCBlueX/LiquidBounce`
- Wurst: `https://github.com/Wurst-Imperium/Wurst7`
