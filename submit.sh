#!/usr/bin/env bash
function deployDataflow() {
  mainClass="$1"
  properties="${@:2}"
  echo "running main class ${mainClass}"
  echo "the job properties are ${properties}"
  mvn compile exec:java \
    -Dexec.mainClass="$mainClass" \
    -Dexec.args="$properties"
}

function updateOrCreate() {
  echo "Trying to update the dataflow job"
  deployDataflow "$1" "${@:2}" "--update"
  flag=$?

  if [ "$flag" -ne 0 ]; then
    echo "The job doesn't exist. Create a new one"
    deployDataflow "$1" "${@:2}"
  fi
  
  echo "Deployment is done!"
}

deployDataflow "$@"