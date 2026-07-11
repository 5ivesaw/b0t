# Update `5ivesaw/b0t` and publish automatically

This update is a **patch-only ZIP**. Apply it over the existing repository with `apply-patch.ps1`.

After applying the patch, the normal release process is only:

```powershell
git status
git add -A
git commit -m "Automate verified GitHub releases"
git push origin main
```

That is all. Do not create or push a tag manually.

The `CI and automatic release` workflow will:

1. Read `sawbotVersion` from `gradle.properties`.
2. Run offline verification.
3. Build the real Forge 1.8.9 JAR.
4. Validate the exact JAR and release payload.
5. Create tag `v<sawbotVersion>`.
6. Publish the GitHub Release and all assets.

For this patch the expected automatic tag is:

```text
v0.3.0-alpha.6
```

A version is immutable. If the tag or release already exists, the workflow deliberately fails instead of silently overwriting it. Every future patch supplied for SawBotV1 will include a version bump.

## One-time GitHub setting

If the release job reports a permission error, open:

```text
Repository Settings → Actions → General → Workflow permissions
```

Select **Read and write permissions**, then save and rerun the failed job.

`Manual Release Recovery` remains available under Actions only as a fallback for a broken/missing release. It is no longer part of the normal process.
