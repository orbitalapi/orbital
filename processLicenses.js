#!/usr/bin/env node

const fs = require('fs').promises;
const path = require('path');

class DependencyWithLicense {
   constructor(licenseNames, dependencyName, dependencyId, dependencyUrl, hasWhitelistedLicense) {
      this.licenseNames = licenseNames;
      this.dependencyName = dependencyName;
      this.dependencyId = dependencyId;
      this.dependencyUrl = dependencyUrl;
      this.hasWhitelistedLicense = hasWhitelistedLicense;
   }
}

function parseMavenDependencies(inputText) {
   const lines = inputText.trim().split('\n');
   const dependencies = [];

   // const regex = /^\((.+?)\)\s+(.+?)\s+\((.+?)\s+-\s+(.+?)\)$/;
   const regex = /^\((.+?)\)\s+([^\(]+?)\s+\(([^ ]+?)\s+-\s+(.+?)\)$/;

   lines.forEach(line => {
      const match = line.trim().match(regex);
      if (match) {
         const [, licenseName, dependencyName, dependencyId, dependencyUrl] = match;
         const licenseNames = licenseName.split(') (')
            .map(d => d.replaceAll(')', ''))
         const whitelistedLicenseNames = whitelist.licenses.map(l => l.licenseName);
         const hasWhitelistedLicense = licenseNames.some(licenseName => whitelistedLicenseNames.includes(licenseName))
         const dependency = new DependencyWithLicense(licenseNames, dependencyName, dependencyId, dependencyUrl, hasWhitelistedLicense);
         dependencies.push(dependency);
      }
   });

   return dependencies;
}

function parseNodeDependencies(inputText) {
   const lines = inputText.split('\n');
   lines.shift() // remove the header line
   const dependencies = [];
   const whitelabeledLicenseIds = whitelist.licenses.map(l => l.spdxCode)

   lines
      .forEach(line => {
         const [dependencyNameWithVersion, licenseName, dependencyUrl] = line.split('","').map(item => item.replace(/"/g, ''));
         // Remove the version number.
         // Special consideration for projects that start @foo/bar
         const dependencyName = (dependencyNameWithVersion.startsWith("@")) ? dependencyNameWithVersion.split('@')[1] : dependencyNameWithVersion.split('@')[0];
         const licenseNames = [licenseName];

         const licenseSpdx = licenseName.startsWith('(') ? licenseName.substring(1, licenseName.length - 1) : licenseName;
         let publishedLicenses = [];
         const parts = licenseSpdx
            .replace(' AND ', ' OR ')
            .split(' OR ')
         parts.forEach(part => publishedLicenses.push(part));

         const isWhitelisted = whitelabeledLicenseIds.some(whitelabeledLicenseId => publishedLicenses.includes(whitelabeledLicenseId))
         const dependency = new DependencyWithLicense(licenseNames, dependencyName, dependencyName, dependencyUrl, isWhitelisted);
         dependencies.push(dependency);
      });

   return dependencies;
}

const whitelist = {
   licenses: [
      {
         licenseName: 'Orbital Enterprise License',
         spdxCode: '',
         url: ''
      },
      {
         licenseName: 'Apache License, Version 2.0',
         spdxCode: 'Apache-2.0',
         url: 'https://www.apache.org/licenses/LICENSE-2.0'
      },
      {
         licenseName: 'MIT License',
         spdxCode: 'MIT',
         url: 'https://opensource.org/licenses/MIT'
      },
      {
         licenseName: 'MIT License',
         spdxCode: 'MIT*',
         url: 'https://opensource.org/licenses/MIT'
      },
      {
         licenseName: 'BSD Zero Clause',
         spdxCode: '0BSD',
         url: 'https://opensource.org/licenses/BSD-3-Clause'
      },
      {
         licenseName: 'BSD-3-Clause',
         spdxCode: 'BSD-3-Clause',
         url: 'https://opensource.org/licenses/BSD-3-Clause'
      },
      {
         licenseName: 'BSD-3-Clause',
         spdxCode: 'BSD',
         url: 'https://opensource.org/licenses/BSD-3-Clause'
      },
      {
         licenseName: 'BSD-2-Clause',
         spdxCode: 'BSD-2-Clause',
         url: 'https://opensource.org/licenses/BSD-2-Clause'
      },
      {
         licenseName: 'Eclipse Public License - v 1.0',
         spdxCode: 'EPL-1.0',
         url: 'https://www.eclipse.org/legal/epl-v10.html'
      },
      {
         licenseName: 'Eclipse Distribution License - v 1.0',
         spdxCode: 'EDL-1.0',
         url: 'https://www.eclipse.org/org/documents/edl-v10.php'
      },
      {
         licenseName: 'Eclipse Public License 2.0',
         spdxCode: 'EPL-2.0',
         url: 'https://www.eclipse.org/legal/epl-2.0/'
      },
      {
         licenseName: 'Bouncy Castle Licence',
         spdxCode: 'Bouncy-Castle',
         url: 'https://www.bouncycastle.org/licence.html'
      },
      {
         licenseName: 'Go License',
         spdxCode: '',
         url: 'http://golang.org/LICENSE'
      },
      /*{
         licenseName: 'GNU Library General Public License v2 only',
         spdxCode: 'LGPL-2.0-only',
         url: 'https://www.gnu.org/licenses/old-licenses/lgpl-2.0.html'
      },
      {
         licenseName: 'GNU Library General Public License v2 or later',
         spdxCode: 'LGPL-2.0-or-later',
         url: 'https://www.gnu.org/licenses/old-licenses/lgpl-2.0.html'
      },*/

      /*
      {
         licenseName: 'GNU Lesser General Public License v2.1 only',
         spdxCode: 'LGPL-2.1-only',
         url: 'https://www.gnu.org/licenses/old-licenses/lgpl-2.1.html'
      },
      {
         licenseName: 'GNU Lesser General Public License v2.1 or later',
         spdxCode: 'LGPL-2.1-or-later',
         url: 'https://www.gnu.org/licenses/old-licenses/lgpl-2.1.html'
      },
      {
         licenseName: 'GNU Lesser General Public License v3.0 only',
         spdxCode: 'LGPL-3.0-only',
         url: 'https://www.gnu.org/licenses/lgpl-3.0.html'
      },
      {
         licenseName: 'GNU Lesser General Public License v3.0 or later',
         spdxCode: 'LGPL-3.0-or-later',
         url: 'https://www.gnu.org/licenses/lgpl-3.0.html'
      },
      */
    /*  {
         licenseName: 'Hazelcast Community License',
         spdxCode: '',
         url: 'https://hazelcast.com/hazelcast-community-license/'
      },*/
      {
         licenseName: 'Common Public License 1.0',
         spdxCode: 'CPL-1.0',
         url: 'https://spdx.org/licenses/CPL-1.0.html'
      },
      {
         licenseName: 'Creative Commons Zero v1.0 Universal',
         spdxCode: 'CC0-1.0',
         url: 'https://spdx.org/licenses/CC0-1.0.html'
      },
      {
         "licenseName": "Creative Commons Attribution 4.0 International",
         "spdxCode": "CC-BY-4.0",
         "url": "https://spdx.org/licenses/CC-BY-4.0.html"
      },
      {
         "licenseName": "Creative Commons Attribution 3.0 Unported",
         "spdxCode": "CC-BY-3.0",
         "url": "https://spdx.org/licenses/CC-BY-3.0.html"
      },
      {
         "licenseName": "ISC License",
         "spdxCode": "ISC",
         "url": "https://spdx.org/licenses/ISC.html"
      },
      {
         "licenseName": "Unlicense",
         "spdxCode": "Unlicense",
         "url": "https://spdx.org/licenses/Unlicense.html"
      },
      {
         "licenseName": "Blue Oak Model License",
         "spdxCode": "BlueOak-1.0.0",
         "url": "https://spdx.org/licenses/BlueOak-1.0.0.html"
      },
      {
         "licenseName": "Python-2.0",
         "spdxCode": "Python-2.0",
         "url": "https://spdx.org/licenses/Python-2.0.html"
      },

   ],
   projects: [
      {
         project: 'com.google.code.findbugs:findbugs-annotations',
         license: 'LGPL-3.0-only',
         rationale: 'Non-standard license id used in source- is actually LGPL-3.0'
      },
      {
         project: 'org.beryx:awt-color-factory',
         license: 'GPL 2 with classpath exception',
         rationale: 'https://github.com/beryx/awt-color-factory grants permission for use'
      },
      {
         project: 'javax.annotation:javax.annotation-api',
         license: 'CDDL',
         rationale: 'Java API is made available under CDDL, which does not require source distribution, provided the CDDL contents are not changed.',
         relatedLinks: [
            'https://fossa.com/blog/open-source-licenses-101-cddl-common-development-distribution-license/',
            'https://github.com/aws/aws-sdk-java/issues/2643#issuecomment-944579030'
         ]
      },
      {
         project: 'javax.websocket:javax.websocket-api',
         license: 'CDDL',
         rationale: 'Java API is made available under CDDL, which does not require source distribution, provided the CDDL contents are not changed.',
         relatedLinks: [
            'https://fossa.com/blog/open-source-licenses-101-cddl-common-development-distribution-license/',
            'https://github.com/aws/aws-sdk-java/issues/2643#issuecomment-944579030'
         ]
      },
      {
         project: 'com.microsoft.sqlserver:mssql-jdbc_auth',
         license: 'Microsoft Proprietary',
         rationale: 'Proprietary license, but does not add material restrictions to use, or add non-commercial obligations',
         relatedLinks: [
            'https://raw.githubusercontent.com/microsoft/mssql-jdbc/v12.7.1/mssql-jdbc_auth_LICENSE',
         ]
      },
      {
         project: 'org.opensaml:opensaml-messaging-api',
         license: 'Apache License, Version 2.0',
         rationale: 'License is declared in parent pom - opensaml-messaging-api inherits from opensaml-parent which inherits from net.shibboleth:parent which declares license of Apache 2',
         relatedLinks: [
            'https://build.shibboleth.net/maven/releases/org/opensaml/opensaml-messaging-api/5.0.0/opensaml-messaging-api-5.0.0.pom',
            'https://build.shibboleth.net/maven/releases/org/opensaml/opensaml-parent/5.0.0/opensaml-parent-5.0.0.pom',
            'https://repo1.maven.org/maven2/net/shibboleth/parent/11.0.1/parent-11.0.1.pom'
         ]
      },
      {
         project: 'memfs',
         license: 'Apache 2',
         rationale: 'package.json shows Apache 2 on Github',
         relatedLinks: [
            'https://github.com/streamich/memfs/blob/master/package.json'
         ]
      },
      {
         project: 'pause-stream',
         license: 'Apache 2',
         rationale: 'License is incorrectly declared in package.json, so parsing fails',
         relatedLinks: [
            'https://github.com/dominictarr/pause-stream/blob/master/package.json'
         ]
      },
      {
         project: 'vyne-app',
         license: 'Orbital License',
         rationale: 'This is our project',
         relatedLinks: []
      },
      {
         project: 'taiga-ui',
         license: 'Apache 2.0',
         rationale: 'Malformed package.json - part of the Apache 2.0 taiga-ui package',
         relatedLinks: []
      },
      {
         project: 'org.hibernate.common:hibernate-commons-annotations',
         license: 'LGPL 2.1',
         rationale: 'LGPL only requires republication of modified LGPL source code. Hibernate and JBoss make this very clear specifically with relation to use of Hibernate in commerical applications',
         relatedLinks: [
            'https://developer.jboss.org/docs/DOC-15788#:~:text=be%20free%20software.-,Can%20I%20embed%20Hibernate%20in%20my%20commercial%20application%3F,Hibernate%20binary%20has%20no%20restrictions.',
            'https://hibernate.org/community/license/'
         ]
      },
      {
         project: 'org.hibernate.orm:hibernate-core',
         license: 'LGPL 2.1',
         rationale: 'LGPL only requires republication of modified LGPL source code. Hibernate and JBoss make this very clear specifically with relation to use of Hibernate in commerical applications',
         relatedLinks: [
            'https://developer.jboss.org/docs/DOC-15788#:~:text=be%20free%20software.-,Can%20I%20embed%20Hibernate%20in%20my%20commercial%20application%3F,Hibernate%20binary%20has%20no%20restrictions.',
            'https://hibernate.org/community/license/'
         ]
      }
   ]
}

function getDependenciesNeedingAttention(dependencies) {
   const whitelistedProjects = whitelist.projects.map(l => l.project);
   const depsNeedingAttention = dependencies.filter(d => !d.hasWhitelistedLicense).filter(d => {
         const isProjectWhitelisted = whitelistedProjects.some(whitelistedProjectId => d.dependencyId.startsWith(whitelistedProjectId))
         return !isProjectWhitelisted;
      })
   ;
   depsNeedingAttention.sort((a, b) => a.dependencyName.localeCompare(b.dependencyName));
   return depsNeedingAttention;
}

async function processMavenDependencies() {
   const filePath = path.resolve('target/generated-sources/license/THIRD-PARTY.txt')
   // Read the file
   const data = await fs.readFile(filePath, 'utf8')
   const dependencies = parseMavenDependencies(data);
   return getDependenciesNeedingAttention(dependencies)
}

async function processNodeDependencies() {
   const filePath = path.resolve('licenses.csv')
   // Read the file
   const data = await fs.readFile(filePath, 'utf8');
   // Parse the dependencies
   const dependencies = parseNodeDependencies(data);
   return getDependenciesNeedingAttention(dependencies)
}

function logDependenciesNeedingAttention(dependencies, kind) {
   console.log(`${dependencies.length} ${kind} dependencies need attention:`)
   dependencies.forEach(dep => {
      console.log([dep.licenseNames.join(' | '), dep.dependencyName, dep.dependencyId, dep.dependencyUrl].join(','))
   })
}

function generateMarkdownTable(data, keys) {
   if (!data.length || !keys.length) {
      return '';
   }

   // Determine the length of the longest value for each key
   const columnWidths = keys.map(key =>
      Math.max(key.length, ...data.map(item => item[key]?.toString().length || 0))
   );

   // Create the header row
   const header = keys.map((key, index) => key.padEnd(columnWidths[index])).join(' | ');

   // Create the separator row
   const separator = keys.map((key, index) => '-'.repeat(columnWidths[index])).join('-|-');

   // Create the data rows
   const rows = data.map(item =>
      keys.map((key, index) => (item[key]?.toString() || '').padEnd(columnWidths[index])).join(' | ')
   );

   // Combine header, separator, and rows into a markdown table
   return [header, separator, ...rows].join('\n');
}

function logWhitelistAsMarkdown() {
   // {
   //          licenseName: 'GNU Lesser General Public License v2.1 or later',
   //          spdxCode: 'LGPL-2.1-or-later',
   //          url: 'https://www.gnu.org/licenses/old-licenses/lgpl-2.1.html'
   //       },
   const tableMarkdown = generateMarkdownTable(whitelist.licenses, ['licenseName', 'spdxCode', 'url'])
   const projects = whitelist.projects.map(project => {
      const relatedLinks = (project.relatedLinks) ? `\nSee also:\n${(project.relatedLinks || []).map(link => ` * ${link}`).join('\n')}` : ''
      return `### ${project.project}
${project.rationale}${relatedLinks}
`
   })
   const markdown = `## Whitelist
The following licenses are currently whitelisted:

${tableMarkdown}

## Exceptions
The following projects have exceptions:

${projects.join("\n")}`

   console.log(markdown)
}

async function main() {
   // Check if a file path is provided
   // if (process.argv.length < 3) {
   //    console.error('Please provide the path to the input file as an argument.');
   //    console.error('Usage: node script.js <path_to_input_file>');
   //    process.exit(1);
   // }
   logWhitelistAsMarkdown()

   const mavenDependenciesNeedingAttention = await processMavenDependencies() || []
   logDependenciesNeedingAttention(mavenDependenciesNeedingAttention, 'maven')

   const nodeDependenciesNeedingAttention = await processNodeDependencies() || []
   logDependenciesNeedingAttention(nodeDependenciesNeedingAttention, 'node')


   if (mavenDependenciesNeedingAttention.length > 0 || nodeDependenciesNeedingAttention.length > 0) {
      console.warn('There are license violations, exiting with error')
      process.exit(1)
   } else {
      console.info('There are no license violations')
      process.exit(0)
   }
}

main();
