export const environment = {
  production: true,
  // Convention for relative urls:  Start with an /, but don't end with one
  queryServiceUrl: `//${window.location.host}`,
  caskServiceUrl: `//${window.location.host}:8800`,
  showPolicyManager: false,
  showGenerateSchema: false
};
