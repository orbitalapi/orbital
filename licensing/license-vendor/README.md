# License Vendor

This project contains the code to generate licenses for Vyne.

## Generating a license
Licenses are created using the command-line tool `licensor`.

## Getting the private key
In order to generate a license, you need the private key, which is not included in this source.

Clone the below repo - ask for access if you don't have it.

```shell
git@gitlab.com:vyne/vyne-license-key.git
```

### Getting the cli
Either

 * build this project `mvn install`, and `cd target/appassembler/bin`
 * Grab from gitlab repo

### Running
To launch, you must pass the path to the private key, checked out earlier.

```shell
./licensor -k /path/to/vyne-license.der
```

The cli will prompt you for details to create the license.
Alternatively, args can be passed from the command line.

```shell
$  ./licensor --help
Usage: <main class> [-e] [-n] [-o] [-x]
  -e, --edition   Licensed Edition. Valid values: STARTER, PLATFORM, ENTERPRISE.
  -n, --name      Licensee name
  -o, --output    Output filename. Default: ./license.json
  -x, --expires   When the license expires
```

The output file - `license.json` contains a signed json file for the license, which can be provided to a user. 
