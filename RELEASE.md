# Releasing binaries to GitHub

This repository provides helper scripts which aid releasing to GitHub.  This
document outlines how to release in a consistent manner.

Before you begin you must generate a [GitHub personal access token][pat].  It
must have at least `repo` scope if releasing to a private repository or
`public_repo` scope to release to a public repository.  Learn more [about GitHub
OAuth scopes][os].  Then set up your environment with the following settings.

    export GITHUB_USER=<github username or organization>
    export GITHUB_TOKEN=<personal access token>
    export GITHUB_REPO=<repository name>

The following is a recommended release process.

```bash
git tag <your tag>
git push origin --tags

# build binaries, copy manifest, and checksum files
./jenkins-bootstrap-shared/scripts/buildRelease.sh

# upload binaries to GitHub (parallelized)
./jenkins-bootstrap-shared/scripts/uploadRelease.sh
```

# Troubleshooting

If you get a 404 not found error, then you might not be using `repo` scope on a
private repository.  It's also possible you incorrectly set the `GITHUB_REPO`
environment variable.

If using GitHub Enterprise, then you'll need to set the following additional
environment variable.

    export GITHUB_API=http://github.example.com/api/v3

[os]: https://developer.github.com/apps/building-oauth-apps/scopes-for-oauth-apps/
[pat]: https://help.github.com/articles/creating-a-personal-access-token-for-the-command-line/
