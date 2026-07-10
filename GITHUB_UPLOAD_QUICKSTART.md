# Upload SawBotV1 to GitHub

The ZIP is arranged so its extracted contents are the repository root. The `.github` directory must be uploaded because it contains the build and release workflows.

## Recommended: GitHub Desktop

1. Extract the ZIP into a folder named `SawBotV1`.
2. In GitHub Desktop, choose **File → Add local repository**.
3. Select the extracted `SawBotV1` folder.
4. If prompted, choose **create a repository here**.
5. Commit all files with the message `Initial SawBotV1 Phase 0 foundation`.
6. Select **Publish repository**.
7. Open the repository on GitHub and select **Actions**.
8. Confirm the **CI** workflow runs.

## Command-line alternative

Create an empty GitHub repository first, then run these commands inside the extracted folder:

```bash
git init
git add .
git commit -m "Initial SawBotV1 Phase 0 foundation"
git branch -M main
git remote add origin https://github.com/YOUR-NAME/SawBotV1.git
git push -u origin main
```

Replace `YOUR-NAME` with the GitHub account or organization.

## Publish the first release

After CI succeeds:

1. Open **Actions** on GitHub.
2. Select **Release**.
3. Select **Run workflow**.
4. Enter `0.1.0-alpha.0`.
5. Keep **Publish as a prerelease** enabled.
6. Run the workflow.

The workflow creates tag `v0.1.0-alpha.0` and attaches the installable Minecraft Forge JAR, source JAR, checksums, and reports to the GitHub Release.
