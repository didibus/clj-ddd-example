# Domain Driven Design (DDD) in Clojure
*An example implementation with explanation*

An example implementation of Domain Driven Design in Clojure with lots of information about DDD and its concepts and how it's implemented in Clojure along the way.

We're taking the same modeling example as from this blog: http://blog.sapiensworks.com/post/2016/07/14/DDD-Aggregate-Decoded-2 and re-doing it in Clojure. We also add the additional requirements from the follow up blog post: http://blog.sapiensworks.com/post/2016/08/19/DDD-Application-Services-Explained

In a nutshell, we're trying to build a banking application, and we know that it needs to support transferring money from one account to another. We've talked with the business users about how they currently manage their bank and what concepts and rules and actions and relationships they currently seem to make use of to manage their bank.

As part of trying to become a domain expert, we've started to learn about the ubiquitous language that the business users are used too. We know there are Accounts, there's this concept of Debit and Credit, there's special identifiers for referring to Accounts, they have the concept of Transfers that tracks all debits and credits between accounts. And most importantly, users can request for money to be transferred from one account to another.

This is what we thus model in this example banking application, with our only feature for now being able to transfer money between existing accounts.

The code base is structured in classic DDD patterns:

* Application Service: `clj-ddd-example` namespace, also the main namespace that can be considered the API to clients, side-effectfull.
* Domain Model: `clj-ddd-example.domain-model` namespace, where we define our domain model, pure.
* Domain Services: `clj-ddd-example.domain-services` namespace, where we define our domain services, pure.
* Repository: `clj-ddd-example.repository` namespace, where we maintain our application state, side-effectful.

Refer to each namespace's doc-string to get a full explanation of each of these concepts, and how it is implemented in Clojure.
