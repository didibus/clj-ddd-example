(ns clj-ddd-example.repository
  "This namespace manages the state of our application, you can use it to get
  domain entities, or to commit to our app state changes to existing entities or
  new entities or delete entities.

   In DDD, the repository is meant for the purpose of writes, and as such it
  goes along with our domain model and services to perform our state changes.
  The domain model and services are a pure representation of what states are
  valid, and what are the rules that dictate how to change them and how they are
  allowed to change. The repository makes use of the domain model to create
  valid domain model entities from the state it maintains when requested. It
  also knows how to understand the domain model in order to interpret what
  changes to the state needs to be made to reflect the changes that were made to
  the domain model, and commit them permanently to our app state.

  In my example, I use an atom to maintain state, and the structure of the atom
  mimics on purpose that of a relational database, to give an idea how it would
  work if say it was using MySQL instead to maintain state.

  In DDD, writes and reads tend to be kept separate. But since an update is just
  a read followed by a write, the repository also provides read capability for
  such updates to the state. We call that a get in DDD, a get is always by some
  identifier that will return one and only one entity per id, if they exist. If
  the entity is an aggregate root, the repository would return the whole
  aggregate. Thus the repository operates at the same transactional boundary as
  the domain model, getting an entity or aggregate by id for purpose of changing
  them, or creating new entities or aggregates, or deleting existing ones.

  For query-like use cases, what you could think of as search, or reading but
  not for the purpose of changing the read data, but simply for viewing or
  displaying, the repository is not supposed to be used, but instead something
  called a Finder is used. A finder does not return data in the shape/structure
  of our domain model, the domain model and services is all for the purpose of
  acting on our state/domain, when only reading, separate read models, aka
  views, should be used instead, as designed for purpose of viewing. The Finder
  would be what allows the application service to provide these view-like read
  only use cases. If after having viewed something, a user wants to change the
  data, or perform some action on it, you would switch to the repository to
  lookup, aka get, the entities and aggregates in charge of the required
  changes, and use the domain model/services to perform the change that the
  application service would commit back using the repository. In that sense,
  Finder is read only, while Repository is write only with reads done as
  necessary in order to write, but switching from one to the other is possible."
  (:require [clj-ddd-example.domain-model :as dm]))


(def ^:private datastore
  (atom {:account-table #{["125746398235" 500.34 :usd]
                          ["234512768893" 12.05 :usd]}
         :transfer-table #{}}))

(defn- get-account-row
  "Returns the DB specific account structure, not one from
   our domain model, nil if there isn't one."
  [account-table account-number]
  (some
   (fn[row] (when (= (first row) account-number) row))
   account-table))

(defn- account->account-table-row
  [account]
  [(-> account :number)
   (-> account :balance :value)
   (-> account :balance :currency)])

(defn- transfer->transfer-table-row
  [transfer]
  [(-> transfer :id)
   (-> transfer :number)
   (-> transfer :debit :number)
   (-> transfer :credit :number)
   (-> transfer :debit :amount :value)
   (-> transfer :debit :amount :currency)
   (-> transfer :creation-date)])

(defn get-account
  "Returns Account entity for the account identified by account-number,
   nil if there isn't one."
  [account-number]
  (when-let [account-row (get-account-row (:account-table @datastore) account-number)]
    (dm/make-account
     (nth account-row 0)
     (dm/make-balance (nth account-row 1)
                      (nth account-row 2)))))

(defn commit-transfer
  "Commits to our app state a transfer, this implies adding a transfer entry for
  the transfer entity created as part of the transfer, as well as updating the
  debited account and the credited account with their new balance."
  [transfer debited-account credited-account]
  (swap! datastore
         (fn[currentstore]
           (let [account-table (:account-table currentstore)
                 debit-account-row (get-account-row account-table (-> debited-account :number))
                 credit-account-row (get-account-row account-table (-> credited-account :number))
                 new-debit-account-row (account->account-table-row debited-account)
                 new-credit-account-row (account->account-table-row credited-account)
                 transfer-row (transfer->transfer-table-row transfer)]
             (-> currentstore
                 (update :account-table disj debit-account-row)
                 (update :account-table conj new-debit-account-row)
                 (update :account-table disj credit-account-row)
                 (update :account-table conj new-credit-account-row)
                 (update :transfer-table conj transfer-row))))))
