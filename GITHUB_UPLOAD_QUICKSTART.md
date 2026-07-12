# GitHub update quick start

The patch automation reads `AUTO_UPDATE.json`, applies the patch, commits `Implement Phase 6 hybrid navigation body`, and pushes `main`. GitHub Actions then runs the hybrid-navigation verifier, builds the Forge JAR, creates tag `v0.7.0-alpha.0`, and publishes the release.

Manual fallback:

```powershell
cd "C:\Users\fivesaw\Desktop\SawBotV1-GitHub-Ready"
git add -A
git commit -m "Implement Phase 6 hybrid navigation body"
git push origin main
```
