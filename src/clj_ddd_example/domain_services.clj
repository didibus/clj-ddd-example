(ns clj-ddd-example.domain-services
  "This namespace contains our domain services for our bounded context.

  In my example, my entire project is one bounded context, it doesn't have to be
  that way, you could have multiple bounded contexts in a monolith, as separate
  sets of namespaces, each having an application service, a domain model, a set
  of domain services, a repository, a possible finder and read model, etc. Or
  you can have each one is a separate lib, or each one as a micro-service, etc.

  In DDD, a domain service is anything that does something for you to the domain
  or about the domain. It could compute some derived data about the domain, or
  it can make changes to the domain. Generally, it is used when what you want
  computed or changed involves multiple aggregates or entities that don't
  normally form an aggregate.

  In my example, transferring money is such a case, you need to check things
  about the debited account entity, the credited account entity, and you have to
  change the debited account entity, the credited account entity, and you have
  to create a transfer entity. There would be nowhere else to put such behavior,
  because it doesn't belong exclusively to an entity or aggregate. It is not
  something only about one Account, or only about one Transfer, but about two
  accounts and a transfer all together. And these don't make a logical
  aggregate, because there are other operations that can be made independently
  to an account or a transfer. That's why it is a good candidate to be offered
  as a domain service.

  Other then that, a domain service is pretty similar to functions on the domain
  model, they basically are used to tell you if you can and if so how you have
  to proceed making changes or taking actions to the domain in ways that are
  consistent with the domain rules and invariants.

  In Clojure, domain services are part of the functional core, and are pure
  functions of input to output. They can take entities, value objects or
  aggregates, as well as additional input needed, and generally return modified
  entities, value objects or aggregates, with any detail needed by the
  application service in order to perform whatever side-effects would be needed
  by the various domain use cases. Those are often called Domain Events in DDD,
  which described something important that happened in the model which needs to
  be interpreted and handled by the application service, almost always in order
  to perform some side-effect.

  You can even see how while I return the modified entities, they are named in
  the past tense, implying some event, in this case that we debited and that we
  credited some accounts, and that we made a transfer. It helps to think like
  this, when you call a domain service, it performs the actions to the domain,
  and returns events that describe those, but as it is all pure, it didn't
  actually take any action when it comes to the state of our application or
  other side-effects, thus it simply returns events describing them. In the
  simplest form, an event describing a change is just the changed entity.

  Domain services can make use of other domain services as well as the domain
  model."
  (:require [clojure.spec.alpha :as s]))

(defn- debit-account
  "Returns a debit-account domain event describing the valid debit state
   change that has happened to the Account, so that it can be applied to our app
   state eventually."
  [account amount]
  (s/assert :account/account account)
  (s/assert :amount/amount amount)
  (if (and
       (= (:account/currency account) (:amount/currency amount))
       (>= (- (:account/balance account) (:amount/value amount)) 0))
    {:account/number (:account/number account)
     :amount/value (:amount/value amount)
     :amount/currency (:amount/currency amount)}
    (throw (ex-info "Can't debit account" {:type :illegal-operation
                                           :action :debit-account
                                           :account account
                                           :amount amount}))))

(defn- credit-account
  "Returns a credit-account domain event describing the valid credit state
   change that has happened to the Account, so that it can be applied to our app
   state eventually."
  [account amount]
  (s/assert :account/account account)
  (s/assert :amount/amount amount)
  (if (= (:account/currency account) (:amount/currency amount))
    {:account/number (:account/number account)
     :amount/value (:amount/value amount)
     :amount/currency (:amount/currency amount)}
    (throw (ex-info "Can't credit account" {:type :illegal-operation
                                            :account account
                                            :amount amount}))))

(defn transfer-money
  "Returns if money can be transferred from one account to another for some
  specific amount, if it can, it'll return the domain event describing how to
  make the valid changes that respect the domain rules and invariants of
  transferring money. Otherwise throws an exception about the transfer not being
  possible."
  [transfer-number from-account to-account amount]
  (s/assert :account/account from-account)
  (s/assert :account/account to-account)
  (s/assert :amount/amount amount)
  (try
    (let [debit-account (debit-account from-account amount)
          credit-account (credit-account to-account amount)]
      ;; Returns a posted-transfer domain event describing the valid posted state
      ;; change that has happened to the Transfer, so that it can be applied to our
      ;; app state eventually.
      {:transfer/id (random-uuid)
       :transfer/number transfer-number
       :account/debit debit-account
       :account/credit credit-account
       :transfer/creation-date (java.util.Date.)})
    (catch Exception e
      (throw (ex-info "Money cannot be transferred."
                      {:type :illegal-operation
                       :action :transfer-money
                       :from-account from-account
                       :to-account to-account
                       :amount amount}
                      e)))))

(defn apply-debit-account
  "Returns an updated account of the given account with the debit described
   by debit-account-event applied to it."
  [account debit-account]
  (s/assert :account/account account)
  (s/assert :account/debit debit-account)
  (update account :account/balance - (:amount/value debit-account)))

(defn apply-credit-account
  "Returns an updated account of the given account with the credit described
   by credit-account-event applied to it."
  [account credit-account]
  (s/assert :account/account account)
  (s/assert :account/credit credit-account)
  (update account :account/balance + (:amount/value credit-account)))