# Our UI build

There are three applications in this folder. We keep them together to keep our builds
simplified.

* `./src/taxi-playground-app`: The app now know as "Voyager", deployed at voyager.orbitalhq.com
* `./src/orbital-app`: Our UI container for Orbital. While the Vyne UI isn't dead, active development is focussed on
  this.
* `./src/app`: The original UI, built and deplyoed as part of Vyne. 

### Orbital app feature flags
To enable feature flags, you need to add some switches to  the VM section of the `OrbitalStationApp` run config. 

For example to add the UI feature flag of `onboardingEnabled` add `--vyne.toggles.onboarding-enabled=true` to the VM options.
