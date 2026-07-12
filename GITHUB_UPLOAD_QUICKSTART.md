# GitHub upload quickstart

The patch automation reads `AUTO_UPDATE.json`, applies the patch, commits
`Implement Phase 9 segmented navigation core`, and pushes `main`. GitHub Actions then
runs the complete foundation/navigation/bridging verifier, builds the Forge JAR,
creates tag `v1.0.0-alpha.0`, and publishes the release.

Manual fallback:

```powershell
cd "C:\Users\fivesaw\Desktop\SawBotV1-GitHub-Ready"
git add -A
git commit -m "Implement Phase 9 segmented navigation core"
git push origin main
```

Do not create the tag manually. The workflow publishes the exact verified commit
automatically.
