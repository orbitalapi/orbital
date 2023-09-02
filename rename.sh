#!/bin/bash

# Set the base directory. You can execute the script from the top-level directory
BASE_DIR=$(pwd)

# Rename function for sources
rename_sources() {
    local dir="$1"
    if [ -d "${dir}/src/main/java/io/vyne" ]; then
       rm -rf "${dir}/src/main/java/com/orbitalhq/vyne"
        # Ensure target directory structure exists
        mkdir -p "${dir}/src/main/java/com/orbitalhq/"
        # Move the vyne directory content into com/orbitalhq
        mv "${dir}/src/main/java/io/vyne"/** "${dir}/src/main/java/com/orbitalhq"
    fi
}

# Rename function for tests
rename_tests() {
    local dir="$1"
    if [ -d "${dir}/src/test/java/io/vyne" ]; then
       rm -rf "${dir}/src/test/java/com/orbitalhq/vyne"
        # Ensure target directory structure exists
        mkdir -p "${dir}/src/test/java/com/orbitalhq/"
        # Move the vyne directory content into com/orbitalhq
        mv "${dir}/src/test/java/io/vyne"/** "${dir}/src/test/java/com/orbitalhq"
    fi
}

# Iterate over all subdirectories
for dir in $(find "${BASE_DIR}" -type d -mindepth 1 -maxdepth 1); do
    # Rename sources and tests directories
    rename_sources "${dir}"
    rename_tests "${dir}"
done

echo "Renaming completed!"
