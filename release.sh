#!/bin/sh
rm -rf dist/
mkdir dist

mvn clean install -DskipTests -P github.pr
cp target/github-pr-poller*.jar dist/

mvn clean install -DskipTests -P git.fb
cp target/git-fb-poller*.jar dist/

mvn clean install -DskipTests -P stash.pr
cp target/stash-pr-poller*.jar dist/

mvn clean install -DskipTests -P gerrit.cs
cp target/gerrit-cs-poller*.jar dist/

