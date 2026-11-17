#!/usr/bin/env bash
# Roda o Maven do wrapper cacheado sem passar pelo script mvn (que falha no
# MSYS: o glob de CLASSWORLDS_JAR nao resolve para caminho Windows).
# Uso: ./mvnw-local.sh -o -q -Dtest=Xxx test
set -euo pipefail

# JAVA_HOME: usa o do ambiente; senao deriva do `java` do PATH se ele for
# executavel; por fim procura um JDK instalado (IntelliJ, JDKs, Corretto) —
# assim o script roda em qualquer maquina Windows sem caminho fixo.
if [ -z "${JAVA_HOME:-}" ] || [ ! -x "${JAVA_HOME}/bin/java" ]; then
  JAVA_HOME=""
  CANDIDATE="$(dirname "$(dirname "$(command -v java)")")"
  if [ -n "$CANDIDATE" ] && [ -x "$CANDIDATE/bin/java" ]; then
    JAVA_HOME="$CANDIDATE"
  else
    for base in "$HOME/.jdks" "/c/Program Files/JetBrains" "/c/Program Files/Java" \
                "/c/Program Files/Eclipse Adoptium" "/c/Program Files/Amazon Corretto"; do
      [ -d "$base" ] || continue
      JAVA_BIN="$(find "$base" -maxdepth 4 -type f \( -name java -o -name java.exe \) 2>/dev/null | head -1)"
      if [ -n "$JAVA_BIN" ]; then
        JAVA_HOME="$(dirname "$(dirname "$JAVA_BIN")")"
        break
      fi
    done
  fi
fi
if [ -z "${JAVA_HOME:-}" ] || [ ! -x "${JAVA_HOME}/bin/java" ]; then
  echo "JAVA_HOME nao definido e nenhum JDK encontrado — defina JAVA_HOME." >&2
  exit 1
fi

# Distribuicao do wrapper: aceita qualquer versao cacheada em ~/.m2, sem hash fixo.
# Localiza pelo bin/m2.conf (a estrutura real do wrapper), nao pelo nome do diretorio.
DIST_ROOT="${MAVEN_WRAPPER_DIST:-$HOME/.m2/wrapper/dists}"
M2_CONF="$(find "$DIST_ROOT" -maxdepth 5 -type f -path '*/bin/m2.conf' 2>/dev/null | head -1)"
if [ -z "$M2_CONF" ]; then
  echo "Maven do wrapper nao encontrado em $DIST_ROOT — rode ./mvnw uma vez para baixar." >&2
  exit 1
fi
DIST="$(dirname "$(dirname "$M2_CONF")")"

CLASSWORLDS="$(find "$DIST" -name 'plexus-classworlds-*.jar' 2>/dev/null | head -1)"
if [ -z "$CLASSWORLDS" ]; then
  echo "plexus-classworlds-*.jar nao encontrado em $DIST" >&2
  exit 1
fi

DW="$(cygpath -w "$DIST")"
CW="$(cygpath -w "$CLASSWORLDS")"
PW="$(cygpath -w "$PWD")"

"$JAVA_HOME/bin/java" \
  -classpath "$CW" \
  "-Dclassworlds.conf=$DW\\bin\\m2.conf" \
  "-Dmaven.home=$DW" \
  "-Dmaven.multiModuleProjectDirectory=$PW" \
  org.codehaus.plexus.classworlds.launcher.Launcher "$@"
