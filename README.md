# Welcome

![](https://img.shields.io/badge/dynamic/xml.svg?label=Latest&url=http%3A%2F%2Frepo.vyne.co%2Frelease%2Fio%2Fvyne%2Fplatform%2Fmaven-metadata.xml&query=%2F%2Frelease&colorB=green&prefix=v&style=for-the-badge&logo=kotlin&logoColor=white)

Vyne is a platform for automating integration between services.

![Simple, easy to read integration code.](.gitbook/assets/carbon-9-.png)

Vyne provides intelligent on-the-fly integrations, that automatically upgrade themselves as your services update. 

{% hint style="warning" %}
Vyne is cool.  It's also pretty early in it's implementation.  Vyne has bugs, and features are being stablised.  You **will** find bugs.  When you do,  please let us know by [filing an issue](https://gitlab.com/vyne/vyne/issues).

Also, these docs are evolving.  Please bear with us while we work to document Vyne in it's entirety.

A good starting point is working through the examples.
{% endhint %}

{% page-ref page="developers-guide/basic-walkthrough-hello-world.md" %}

## Maven setup

To grab our artifacts, you'll need the following repository info in your maven build.

```markup
 <repositories>
      <repository>
          <id>taxi-releases</id>
          <url>https://dl.bintray.com/taxi-lang/releases</url>
      </repository>
      <repository>
          <id>vyne-releases</id>
          <url>http://repo.vyne.co/release</url>
      </repository>
      <repository>
          <id>vyne-snapshots</id>
          <url>http://repo.vyne.co/snapshot</url>
          <snapshots>
              <enabled>true</enabled>
          </snapshots>
      </repository>
  </repositories>
```

