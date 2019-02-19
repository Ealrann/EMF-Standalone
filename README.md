# EMF-Standalone
EMF, compatible with java 9 modules, no Eclipse dependencies.

Current EMF version on master: 2.21.0.

To easily update this fork, I chose to keep a minimal list of commit in master; Then, I re-apply these commits on the new versions of EMF. I finally "store" the result in a dedicated branch for this version.

Some GitHub packages are provided, for example, to use them in gradle:
```
repositories {
	maven {
		url "https://maven.pkg.github.com/ealrann/emf-standalone"
			credentials {
				username = findProperty("github.username")
				password = findProperty("github.token")
			}
		}
	}
}

dependencies {
	api "emf.standalone:org.eclipse.emf.common:2.21.0"
	api "emf.standalone:org.eclipse.emf.ecore:2.21.0"
	api "emf.standalone:org.eclipse.emf.ecore.xmi:2.21.0"
}
```
Credentials can be defined in a gradle.properties
To generate a github token: https://github.com/settings/tokens
