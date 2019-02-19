# EMF-Standalone
EMF, compatible with java 9 modules, no Eclipse dependencies.

Current EMF version on master: 2.32.0.

To easily update this fork, I chose to keep a minimal list of commit in master; Then, I re-apply these commits on the new versions of EMF. I finally "store" the result in a dedicated branch for this version.

## Using with Gradle

Github packages are published. To use them, you first need to generate a [Github Access Token](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/creating-a-personal-access-token), and store it in a local file `~/.gradle/gradle.properties`:
```
github.username=
github.token=
```

Then, you can import these libraries in your build.gradle:
```
repositories {
	maven {
		url "https://maven.pkg.github.com/ealrann/emf-standalone"
		credentials {
			username = findProperty("github.username") ?: System.getenv("USERNAME")
			password = findProperty("github.token") ?: System.getenv("TOKEN")
		}
	}
}

dependencies {
	api "emf.standalone:org.eclipse.emf.common:2.32.0"
	api "emf.standalone:org.eclipse.emf.ecore:2.32.0"
	api "emf.standalone:org.eclipse.emf.ecore.xmi:2.32.0"
}
```
Credentials can be defined in a gradle.properties
To generate a github token: https://github.com/settings/tokens
