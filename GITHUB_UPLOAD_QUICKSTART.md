# GitHub upload quick start

Repository: `https://github.com/5ivesaw/b0t`

Current version: `0.5.0-alpha.0`

## Patch workflow

Apply `SawBotV1-0.5.0-alpha.0-PATCH.zip` over the existing `0.4.0-alpha.0` repository using its `apply-patch.ps1`, or use the user's local patch automation.

Then:

```powershell
cd "C:\Users\fivesaw\Desktop\SawBotV1-GitHub-Ready"
git add -A
git commit -m "Implement Phase 4 actuator and model bridge"
git push origin main
```

The `main` push automatically runs verification, builds the Forge JAR, creates tag `v0.5.0-alpha.0`, and publishes the release. Do not create a tag manually.
