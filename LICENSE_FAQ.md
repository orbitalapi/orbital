## Licensing

We are progressively making the source of Orbital available, moving from a closed source model 
to a mix of open-source and [fair code](https://faircode.io/) licenses.

We're doing this iteratively.  It's likely that we may move code between licenses as we continue to find the right balance.

While we work through this, it's responsible to outline our intentions, and how we think about licensing.

We want Orbital users to trust that the platform will continue even if the company behind it changes. 
At the same time, we want to ensure the company's business interests are protected, and ensure Orbital remains a successful commercial product.

Our guiding approach is:
 * Taxi, and all the language tooling is Open Sourced under an Apache 2 license
 * The tooling in Orbital for executing queries and delivering integration is published under a Fair Code license - we're using the Business Source License (BSL)
 * Tooling which focuses on improving productivity, collaboration, performance, or authoring require a paid license.

We also want to protect our commercial interests, ensuring that customers don't use Orbital (or it's code) to compete with Orbital.

To further clarify our intentions of what we consider allowed / prohibited within the BSL section of our code:
 * Shipping commercial products (including SaaS) that use Orbital to integrate data sources is permitted, where those integration capabilities are not configurable by end users
 * Using Orbital to build public facing APIs that you expose to paying customers in your commercial product (including SaaS) is permitted
 * Using Orbital internally in your organisation is permitted
 * Using Orbital to provide configurable integration capabilities to end users requires a license
 * Using Orbital to allow end-users to build their own APIs requires a license
 * Using Orbital to allow customers to create data pipelines requires a license
 * Using Orbital to provide services which compete with Orbital requires a license


