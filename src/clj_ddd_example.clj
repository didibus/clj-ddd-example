(ns clj-ddd-example
  "Clojure implementation of DDD, based on example here:
  * http://blog.sapiensworks.com/post/2016/07/14/DDD-Aggregate-Decoded-2
  * http://blog.sapiensworks.com/post/2016/08/19/DDD-Application-Services-Explained

  This namespace contains our Application Service.

  In DDD, the application service is the entry-point to a bounded context. It
  implements top-level (at the bounded context), domain use cases. The
  application service is allowed to make use of the domain model, the domain
  services, and everything that is considered infrastructure, such as the
  repository, logging, security, and all that.

  In Clojure, the application service is also our imperative shell, and is the
  only thing performing side-effects, directly or through making use of the
  infrastructure to do so.

  Its main responsibility is to orchestrate each domain case (aka business case)
  end to end. In our example, it orchestrates the transfer-money use case, where
  a user can initiate a transfer of money of some specified amount in a given
  currency from one account to another. The logic for transferring the money,
  and all the rules and invariants are managed by the domain model and services,
  the application service simply delegates to them. That said, they are pure
  functions, they form our functional core, so it is the application service
  that must coordinate getting the required inputs which we do here by using the
  repository to get the account to debit and the account to credit, and by
  making a domain amount using the provided user input. It must also coordinate
  the use of the domain model and services to deliver on the use case, such as
  how it first uses the domain model to make an amount, and then uses the domain
  service to transfer money between accounts. And finally it must also reflect
  the changes made by the domain model and services wherever needed, i.e., it
  must perform the necessary side-effects the use case demand, such as in our
  case updating our persistent state using the repository to update our
  datastore about the transfer of money that occurred."
  (:require [clj-ddd-example.repository :as repository]
            [clj-ddd-example.domain-model :as dm]
            [clj-ddd-example.domain-services :as ds]))


(defn transfer-money
  "Our first use case, transfer-money, can be used to transfer money from one
  account to another using the respective account numbers, for some specified
  amount in a given currency. The user must provide the number they want to use
  for referring to the transfer later on.

   Remark how in DDD, the application service inputs and outputs do not leak the
  domain model, you don't have to provide entities or value objects in the shape
  and structure defined by our domain model, and it won't return them either.
  Now in Clojure, everything is just data, so we could use data of a similar
  shape/structure if we wanted, but you'd still want to create your domain model
  inside here, which can mean just validating that you were given valid domain
  model shapes, or if not mapping the inputs to them."
  [& {:keys [transfer-number from to amount currency]}]
  (try
    (let [from-account (repository/get-account from)
          to-account (repository/get-account to)
          domain-amount (dm/make-amount amount currency)
          transfered-money (ds/transfer-money transfer-number from-account to-account domain-amount)]
      (repository/commit-transfer
       (:transfered transfered-money)
       (:debited-account transfered-money)
       (:credited-account transfered-money))
      {:status :done
       :transfered [(-> domain-amount :value) (-> domain-amount :currency)]
       :debited-account (-> transfered-money :debited-account :number)
       :debited-account-balance [(-> transfered-money :debited-account :balance :value)
                                 (-> transfered-money :debited-account :balance :currency)]
       :credited-account (-> transfered-money :credited-account :number)
       :credited-account-balance [(-> transfered-money :credited-account :balance :value)
                                  (-> transfered-money :credited-account :balance :currency)]})
    (catch Exception e
      {:status :error
       :transfer-number transfer-number
       :from from
       :to to
       :amount amount
       :currency currency
       :error e})))

#_(transfer-money
   :transfer-number "ABC12345678"
   :from "125746398235"
   :to "234512768893"
   :amount 200
   :currency :usd)
