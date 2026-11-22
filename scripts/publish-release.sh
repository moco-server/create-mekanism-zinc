#!/usr/bin/env bash
set -euo pipefail

version=$1
notes=$2
tag="v$version"
commit=$(git rev-parse HEAD)
artifact=create-mekanism-zinc
jar_name="$artifact-$version.jar"
jar=".local/artifact/$jar_name"
pom=neoforge/build/publications/mod/pom-default.xml
group=$(sed -n 's/^mod_group_id=//p' gradle.properties)
package_url="https://maven.pkg.github.com/${GITHUB_REPOSITORY,,}/${group//.//}/$artifact/$version"

test -s "$jar" && test -s "$pom" && test -s "$notes" || {
  echo 'Release JAR, POM and notes are required.' >&2
  exit 1
}

scratch=$(mktemp -d)
trap 'rm -rf "$scratch"' EXIT
existing_tag=$(git rev-parse --verify --quiet "refs/tags/$tag^{commit}" || true)
if [[ -n "$existing_tag" && "$existing_tag" != "$commit" ]]; then
  echo "$tag already points to $existing_tag, expected $commit." >&2
  exit 1
fi

package_file_present() {
  local local_file=$1 remote_name=$2 status
  status=$(curl --silent --show-error --location --retry 3 \
    --user "$GITHUB_ACTOR:$GH_TOKEN" --output "$scratch/package" \
    --write-out '%{http_code}' "$package_url/$remote_name") || exit 1
  case "$status" in
    200)
      if ! cmp -s "$local_file" "$scratch/package"; then
        echo "GitHub Packages already has different contents for $remote_name." >&2
        exit 1
      fi
      ;;
    404) return 1 ;;
    *) echo "GitHub Packages returned HTTP $status for $remote_name." >&2; exit 1 ;;
  esac
}

# Packages 登録後に Release 作成が失敗した場合も、同じ版の JAR/POM を再利用する。
jar_present=false
pom_present=false
if package_file_present "$jar" "$jar_name"; then jar_present=true; fi
if package_file_present "$pom" "$artifact-$version.pom"; then pom_present=true; fi
if [[ "$jar_present" != true || "$pom_present" != true ]]; then
  ORG_GRADLE_PROJECT_githubPackagesUsername="$GITHUB_ACTOR" \
    ORG_GRADLE_PROJECT_githubPackagesPassword="$GH_TOKEN" \
    ./gradlew :neoforge:publishModPublicationToGithubPackagesRepository
  package_file_present "$jar" "$jar_name"
  package_file_present "$pom" "$artifact-$version.pom"
fi

if release=$(gh release view "$tag" --json isDraft,targetCommitish,assets); then
  if [[ -z "$existing_tag" && "$(jq -r '.targetCommitish' <<< "$release")" != "$commit" ]]; then
    echo "Existing release $tag targets a different commit." >&2
    exit 1
  fi
  if jq -e --arg name "$jar_name" 'any(.assets[]; .name == $name)' <<< "$release" >/dev/null; then
    gh release download "$tag" --pattern "$jar_name" --output "$scratch/release.jar"
    cmp "$jar" "$scratch/release.jar"
  elif [[ "$(jq -r '.isDraft' <<< "$release")" == true ]]; then
    gh release upload "$tag" "$jar"
  else
    echo "Existing release $tag is missing $jar_name." >&2
    exit 1
  fi
  if [[ "$(jq -r '.isDraft' <<< "$release")" == true ]]; then
    gh release edit "$tag" --draft=false
  fi
else
  release_flags=()
  if [[ "$version" == *-* ]]; then release_flags+=(--prerelease); fi
  gh release create "$tag" "$jar" --target "$commit" --title "$tag" --notes-file "$notes" "${release_flags[@]}"
fi
