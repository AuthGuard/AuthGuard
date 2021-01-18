# AuthGuard

[![Build Status](https://travis-ci.com/AuthGuard/AuthGuard.svg?branch=master)](https://travis-ci.com/AuthGuard/AuthGuard)

An easy-to-use, and easy-to-customize, identity server. It supports multiple authentication and authorization options and 
can be extended to support other ones, or add new features.

## Why Use AuthGuard?
Identity management is almost never the core part of apps, websites, or services. AuthGuard is a simple service which 
can be used to provide that so that you can focus on the important parts of what you are building. With AuthGuard you:
1. Have a ready identity management solution.
2. Have full control over your data.
3. Can easily extend it to make it fit your own needs even beyond authentication and authorization (e.g. integrating it 
   with monitoring tools, or a data processing pipeline).
4. Are not tied to a certain database, or a class of databases. You can an make it work with any database even ones which 
   are not officially supported.
   
## AuthGuard Distributions
AuthGuard is, more or less, a "kernel". In order for you to run it and make it usable, you need to create a distribution. 
An AuthGuard distribution is essentially AuthGuard + plugins. At least a plugin providing data access implementation is 
required in order for the server to run.

### Creating a Distribution
There are two ways to create a distribution:
1. Using a build system (Maven, Gradle, SBT...etc) by adding them as dependencies
2. Running the `rest` jar and setting the classpath manually

## Plugins
There are some standard plugins created and support by the AuthGuard team. Some are considered core parts and exist as 
modules in the main project, while the others get their own repository.

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

### LDAP (experimental)
Adds support for LDAP-based authentication by using an LDAP server as an identity provider.

### JavaMail
An email provider implementation which uses JavaMail to send emails to users.

### Kafka Bridge
A plugin which connected AuthGuard's internal event system to Kafka. It can be configured to publish all or only specific 
events to Kafka for external systems to process.

### Mongo DAL
A data access implementation which uses Mongo for persistence, and a complementary module to use it for caching as well.

### Hibernate DAL
A data access implementation for SQL databases using Hibernate.

### Local Testing Plugins
You can use some basic implementation which are mainly used to test AuthGuard, those include:
* Memory DAL: A DAL implementation (both persistence and cache which stores things in memory)
* Log EMB: A plugin which prints all events published to any channel in the event system 

## Documentation
You can see the full documentation on the official website [here](https://authguard.github.io/), and the OpenAPI 
documentation is available under the `api` module.
