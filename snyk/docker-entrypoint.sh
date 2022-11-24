#!/bin/sh

# Note that this script is implemented in sh, *not* bash. This is to aid portability
# for environments which may have the minimal shell

# Command
# In order to deal with edge cases like requiring specific flags or filenames
# Look for an ENV and if present run the specified commands
if ! [ -z "${COMMAND}" ]; then
    exec ${COMMAND}
else

    # Python
    # Snyk requires Python to have downloaded the dependencies before running
    # If pip is present on the path, and we find a requirements.txt file, run pip install -r requirements.txt
    # If pipenv is present on the path, and we find a Pipfile without a Pipfile.lock, run pipenv update
    if [ -x "$(command -v pip)" ]; then
        if [ -f "requirements.txt" ]; then
            out=$(cat requirements.txt | sed -e '/^\s*#.*$/d' -e '/^\s*$/d' | xargs -n 1 pip install 2>&1 || true) # Skipping the dependencies which aren't Installable
        fi
        if [ -f "Pipfile" ]; then
            if ! [ -x "$(command -v pipenv)" ]; then
                pip install pipenv > /dev/null 2>&1
            fi
            if [ -f "Pipfile.lock" ]; then
                out=$(pipenv sync)
            else
                out=$(pipenv update)
            fi
        fi
        if [ -f "pyproject.toml" ] && ! [ -f "poetry.lock" ]; then
            if ! [ -x "$(command -v poetry)" ]; then
                pip install poetry > /dev/null 2>&1
            fi
            out=$(poetry config virtualenvs.create false && poetry install)
        fi
    fi

    # Maven
    # Snyk requires Maven to have downloaded the dependencies before running
    # If mvn is present on the path, and we find a pom.xml, run mvn install
    if [ -x "$(command -v mvn)" ]; then
        if [ -f "pom.xml" ]; then
            out=$(mvn install --no-transfer-progress -DskipTests)
        fi
    fi

    # Go dep
    # Snyk requires dep to be installed
    # If Go is installed and if we find a Gopkg.toml file, ensure dep is installed and then install dependencies
    if [ -x "$(command -v go)" ]; then
        if [ -f "Gopkg.toml" ]; then
            if ! [ -x "$(command -v dep)" ]; then
                curl -s https://raw.githubusercontent.com/golang/dep/master/install.sh | sh > /dev/null 2>&1
            fi
            out=$(dep ensure)
        fi
    fi

fi

exit_code=$?

# If an error occurs in the command run then print the error and
# exit with the same exit code
if [ $exit_code -ne 0 ]; then
    printf '%s\n' "$out"
    exit $exit_code
# By default we don't output any of the commands needed to run before Snyk
# but when debugging it can be useful to trigger that output to be shown.
# To do so simply set the DEBUG environment variable.
elif ! [ -z "${DEBUG}" ]; then
    printf '%s\n' "$out"
fi

# This is in place for GitHub Actions, in order to build a nice
# interface using args (which are converted to ENV) for whether or not
# to output a JSON file
if [ "$INPUT_COMMAND" = "test" -a "$INPUT_JSON" = "true" ]; then
    JSON_OUTPUT="--json-file-output=snyk.json"
fi

if [ "$INPUT_COMMAND" = "test" -a "$INPUT_SARIF" = "true" ]; then
    # SARIF output is only relevant if a file is specified as well
    # The IaC Action uses a property, while will use a flag
    args=$@
    if [ "${args/--file}" != "$args" ] || ! [ -z "$INPUT_FILE" ] ; then
        SARIF_OUTPUT="--sarif-file-output=snyk.sarif"
    fi
fi

# create the command to invocate as an intermediate string and use eval to run exec with a single string
# This supports both arguments with spaces and usages where multiple CLI arguments are given as one argument to the entrypoint script.
cmd_string="$* $JSON_OUTPUT $SARIF_OUTPUT"
eval "exec ${cmd_string}"
