# Starting Vyne

A docker-compose sets up all the services you need.

To get started, simply either clone \(using git\) or download \(using your browser\) the [Vyne Taxonomy Project](https://gitlab.com/vyne/vyne-taxonomy-environment) on Gitlab.

#### Downloading

If you're not familiar with Git, that's fine.  Just head over to the [Vyne Taxonomy Project](https://gitlab.com/vyne/vyne-taxonomy-environment) on Gitlab, and click to download the source code.

![](../.gitbook/assets/image%20%2832%29.png)

Then, unzip the contents locally.

### Docker Compose file

The docker-compose file starts all the vyne components.

To get started, simply open a command prompt in the same directory as you've just donwloaded, and run:

```bash
docker-compose up -d
```

Wait a bit, and then navigate to [http://localhost:9022](http://localhost:9022).  You should have vyne running, and a simple schema exposed.

### Configuring the Schema location

#### Using the default schema location

By default, this environment will serve the schemas in the `./schemas` subdirectory in the folder you're working from.

There's a sample file in there to get you started.  You can delete the contents of that directory,  and use Visual Studio code to author your own taxonomy files.

{% page-ref page="setting-up-visual-studio-code.md" %}

#### Setting your own schema location

If you've got a larger taxi project, you'll likely want to check it out from source control.

Check it out somewhere, then **either**:

* Update the `TAXONOMY_HOME` set in the `.env` file
* **Or** set a `TAXONOMY_HOME` environment variable:

{% tabs %}
{% tab title="Linux & Mac" %}
```bash
export TAXONOMY_HOME=/path/to/project
```
{% endtab %}

{% tab title="Windows CMD" %}
```bash
C:> set TAXONOMY_HOME="c:\path\to\project"
```
{% endtab %}

{% tab title="Windows Powershell" %}
```bash
PS C:> $env:TAXONOMY_HOME="c:\path\to\project"
```
{% endtab %}
{% endtabs %}

The path you specify should either be:

* A directory containing taxi files
* A directory containing a `taxi.conf` file 

