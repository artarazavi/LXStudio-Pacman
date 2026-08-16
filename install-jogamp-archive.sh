#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VERSION="2.4.0-rc-20230123"
ARCHIVE_URL="https://jogamp.org/deployment/archive/rc/v2.4.0/jar"
M2_REPO="${HOME}/.m2/repository"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

artifact_installed() {
  local path="$1"
  [[ -f "${M2_REPO}/${path}" ]]
}

download_if_missing() {
  local url="$1"
  local dest="$2"
  if [[ ! -f "${dest}" ]]; then
    curl -fsSL "$url" -o "$dest"
  fi
}

require_cmd curl
require_cmd mvn

if [[ -z "${JAVA_HOME:-}" ]]; then
  echo "JAVA_HOME must be set before running install-jogamp-archive.sh." >&2
  exit 1
fi

if [[ ! -x "${JAVA_HOME}/bin/jar" ]]; then
  echo "Could not find the JDK jar tool at ${JAVA_HOME}/bin/jar" >&2
  exit 1
fi

if artifact_installed "org/jogamp/gluegen/gluegen-rt-main/${VERSION}/gluegen-rt-main-${VERSION}.pom" \
  && artifact_installed "org/jogamp/jogl/jogl-all-main/${VERSION}/jogl-all-main-${VERSION}.pom"; then
  echo "JogAmp archive artifacts already installed for ${VERSION}."
  exit 0
fi

tmp_dir="$(mktemp -d)"
trap 'rm -rf "${tmp_dir}"' EXIT

mkdir -p "${tmp_dir}/empty"
"${JAVA_HOME}/bin/jar" --create --file "${tmp_dir}/empty.jar" -C "${tmp_dir}/empty" .

download_if_missing "${ARCHIVE_URL}/gluegen-rt.jar" "${tmp_dir}/gluegen-rt.jar"
download_if_missing "${ARCHIVE_URL}/jogl-all.jar" "${tmp_dir}/jogl-all.jar"
download_if_missing "${ARCHIVE_URL}/gluegen-rt-natives-macosx-universal.jar" "${tmp_dir}/gluegen-rt-natives-macosx-universal.jar"
download_if_missing "${ARCHIVE_URL}/jogl-all-natives-macosx-universal.jar" "${tmp_dir}/jogl-all-natives-macosx-universal.jar"

cat > "${tmp_dir}/gluegen-rt-main.pom" <<EOF
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>org.jogamp.gluegen</groupId>
  <artifactId>gluegen-rt-main</artifactId>
  <version>${VERSION}</version>
  <packaging>jar</packaging>
  <name>GlueGen Runtime (macOS bootstrap)</name>
  <dependencies>
    <dependency>
      <groupId>org.jogamp.gluegen</groupId>
      <artifactId>gluegen-rt</artifactId>
      <version>${VERSION}</version>
    </dependency>
    <dependency>
      <groupId>org.jogamp.gluegen</groupId>
      <artifactId>gluegen-rt</artifactId>
      <version>${VERSION}</version>
      <classifier>natives-macosx-universal</classifier>
    </dependency>
  </dependencies>
</project>
EOF

cat > "${tmp_dir}/jogl-all-main.pom" <<EOF
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>org.jogamp.jogl</groupId>
  <artifactId>jogl-all-main</artifactId>
  <version>${VERSION}</version>
  <packaging>jar</packaging>
  <name>JOGL All (macOS bootstrap)</name>
  <dependencies>
    <dependency>
      <groupId>org.jogamp.jogl</groupId>
      <artifactId>jogl-all</artifactId>
      <version>${VERSION}</version>
    </dependency>
    <dependency>
      <groupId>org.jogamp.jogl</groupId>
      <artifactId>jogl-all</artifactId>
      <version>${VERSION}</version>
      <classifier>natives-macosx-universal</classifier>
    </dependency>
  </dependencies>
</project>
EOF

mvn install:install-file -Dfile="${tmp_dir}/gluegen-rt.jar" -DgroupId=org.jogamp.gluegen -DartifactId=gluegen-rt -Dversion="${VERSION}" -Dpackaging=jar
mvn install:install-file -Dfile="${tmp_dir}/gluegen-rt-natives-macosx-universal.jar" -DgroupId=org.jogamp.gluegen -DartifactId=gluegen-rt -Dversion="${VERSION}" -Dpackaging=jar -Dclassifier=natives-macosx-universal
mvn install:install-file -Dfile="${tmp_dir}/jogl-all.jar" -DgroupId=org.jogamp.jogl -DartifactId=jogl-all -Dversion="${VERSION}" -Dpackaging=jar
mvn install:install-file -Dfile="${tmp_dir}/jogl-all-natives-macosx-universal.jar" -DgroupId=org.jogamp.jogl -DartifactId=jogl-all -Dversion="${VERSION}" -Dpackaging=jar -Dclassifier=natives-macosx-universal

mvn install:install-file -Dfile="${tmp_dir}/empty.jar" -DpomFile="${tmp_dir}/gluegen-rt-main.pom"
mvn install:install-file -Dfile="${tmp_dir}/empty.jar" -DpomFile="${tmp_dir}/jogl-all-main.pom"

echo "Installed JogAmp archive artifacts for ${VERSION} into ${M2_REPO}."
