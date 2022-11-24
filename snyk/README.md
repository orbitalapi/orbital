The Snyk integration with GitLab CI isn't feasible for us since that doesn't scan the JARs used by our app which is a
requirement for us. The integration scans just the Docker image itself.

There are a few reasons for the needing this custom Docker build for Snyk:

1. Not sure what exactly is the problem why we need to build the Snyk Docker image by ourselves but seems to be related
   to [this issue](https://github.com/snyk/snyk-images/issues/48).
   The [docker-entrypoint.sh](https://github.com/snyk/snyk-images/blob/master/docker-entrypoint.sh) used by Snyk images
   seems to use `eval` over `exec`. Yet, I'm not sure why specifying a custom entrypoint is needed anyways, so that's
   been removed.
2. We need to install Docker into the Snyk image to be able to auth properly in the `before_script`. Though, Snyk has
   options to maybe do the auth on its own if the username and password have been provided. Tried this quickly and
   didn't work. Probably due to point 3. Worth a shot if touching any of this, though.
3. The image is doing something odd with bash/sh setup which prevents it from reading the env vars as expected from
   GitLab CI global variables. Thus, the token is passed as variable for the job itself. 

