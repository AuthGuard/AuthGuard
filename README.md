# AuthGuard

[![Build Status](https://travis-ci.com/AuthGuard/AuthGuard.svg?branch=master)](https://travis-ci.com/AuthGuard/AuthGuard)

[![Language grade: Java](https://img.shields.io/lgtm/grade/java/g/AuthGuard/AuthGuard.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/AuthGuard/AuthGuard/context:java)

An easy-to-use, and easy-to-customize, identity server. It supports multiple authentication and authorization options and 
can be extended to support other ones, or add new features. It's an API-only solution, there is currently 
no dedicated dashboard. 

## Documentation
You can see the full documentation on the website [here](https://authguard.github.io/). OpenAPI documentation is
available under the `api` module, and can also be found [here](https://authguard.github.io/). Tutorials are 
also available on the same website.

For the documentation of a specific plugin please visit the [extensions repository](https://github.com/AuthGuard/extensions) 
and check that plugin readme.

## Why Use AuthGuard?
Identity management is almost never the core part of apps, websites, or services. AuthGuard is a simple service which 
can be used to provide that so that you can focus on the important parts of what you are building. With AuthGuard you:
1. Have a ready identity management solution.
2. Have full control over your data.
3. Can easily extend it to make it fit your own needs even beyond authentication and authorization (e.g. integrating it 
   with monitoring tools, or a data processing pipeline).
4. Are not tied to a certain database, or a class of databases. You can make it work with any database even ones which 
   are not officially supported.
5. Have more advanced features available out-of-the-box like requiring OTPs, or re-entring passwords
   to perform certain actions..etc.
   
## AuthGuard Distributions
There's no "one size fits all". AuthGuard is, more or less, a "kernel". In order for you to run it and make it usable, 
you need to create a distribution. An AuthGuard distribution is essentially AuthGuard + plugins. It's required to at 
least have a plugin providing data access implementation in order for the server to run. We have three 
standard distributions pre-built for three databases: MongoDB, PostgreSQL, MySQL. Standard distributions 
come with the following plugins:
1. Standard data access (persistence + cache)
2. JWT
3. Sessions
4. Account lock
5. Verification
6. Email
7. JavaMail provider (for SMTP, IMAP, and POP3)

### Creating a Distribution
There are two ways to create a distribution:
1. Using a build system (Maven, Gradle, SBT...etc) by adding them as dependencies
2. Running the `rest` jar and setting the classpath manually

## Plugins
There are some standard plugins created and support by the AuthGuard team. Some are considered core parts and exist as 
modules in the main project, while the others get their own repository. The other standard extensions can be found in 
the [extension repository](https://github.com/AuthGuard/extensions).

### JWT
A plugin which provides JWT exchanges and other features around JWTs:
1. JWT auth exchanges
2. JWT API keys
3. OAuth and OpenID Connect support

### Sessions
A plugin to add support for sessions. Requires a session store to be provided by a DAL implementation.

### Verification
The verification plugin will send a verification email to an email which needs to be verified. Requires an email provider 
implementation.

### Account Lock
Adds support for locking accounts after a number of failed logins within a period.

### LDAP 
Adds support for LDAP-based authentication by using an LDAP server as an identity provider.

### Email
Adds subscribers for events which may require sending emails. This plugin does not come with 
an email provider, and one must be added. We have an implementation using JavaMail and another 
one (provided as an example to how to use external APIs) using SendGrid API.

### SMS
Similar to the email plugin, this one only adds definitions and subscribers, and an SMS provider 
implementation must be provided.

### JavaMail
The standard email provider, it uses JavaMail library and support SMTP, IMAP, and POP3 protocols.

## License and Price
The project in its entirety is open-source and no features are hidden behind a professional 
plan. It is also free of charge for non-commercial use. If you want to use it for a commercial 
product or a business, please support the project by purchasing a license 
from [our store](https://www.nexblocks.com/portal/). The store is temporary until we move to 
a better one, but all license will be transferred.

## Open-Source Credits
This project is made possible by other open-source projects, and they deserve the recognition. 
* Javalin
* Apache Commons
* Bouncy Castle
* Auth0 JWT 
* Guice
* Reflections
* RxJava
* Vertx
* OkHttp
* Vavr
* Unbounded LDAP
* Logback
* Jackson
* Immutables
* Mapstruct
* JUnit
* Mockito
* WireMock
* AssertJ
* Rest Assured
* JavaMail