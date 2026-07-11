# GitHub update quick start

The patch automation reads `AUTO_UPDATE.json`, applies the patch to the repository, commits `Implement Phase 5 learned waypoint navigation`, and pushes `main`. GitHub Actions then verifies the Phase 5 model assets, builds the Forge JAR, creates tag `v0.6.0-alpha.0`, and publishes the release.

Manual fallback:

```powershell
cd "C:\Users\fivesaw\Desktop\SawBotV1-GitHub-Ready"
git add -A
git commit -m "Implement Phase 5 learned waypoint navigation"
git push origin main
```
