export const environment = {
  production: false,
  // Convention for relative urls:  Start with an /, but don't end with one
  queryServiceUrl: `//localhost:9022`,
  websocketUrl: `ws://localhost:9022/stomp`,
  caskServiceUrl: `//localhost:8800`,
  showPolicyManager: true,
  // TODO: hide changes made on data-explorer related to add type stuff
  showGenerateSchema: true,
};
