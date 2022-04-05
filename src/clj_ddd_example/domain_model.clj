(ns clj-ddd-example.domain-model
  "Our Domain Model lives here. In DDD, a domain model is the set of domain
  concepts modeled as data in a particular structure along with their
  invariants, i.e., the set of consistency rules that describes each valid state
  for each concept in the domain.

   In our example, we're pretending to model some business which has customer
  accounts with a balance, and where you can transfer funds from one account to
  another. We gathered from the domain experts that the business is used to
  think in terms of monetary amounts, accounts and balances. That they see a
  transfer as a debit from one account, and a credit to another. They think of
  transfers as a ledger of debits and credits that add up to each account's
  balance. They have their own number scheme to identify accounts and transfers.
  Finally, they like to track the time at which transfers were conducted.

   The above concepts will form our entities, value objects and aggregates. We
  also gathered some of the invariants the business cared about. Account numbers
  are 12 digit integers and never have 0s in them. Transfer numbers are 3
  alphabetical uppercase characters followed by 8 digits that never contain 0s
  in them. Transfers are always between two different accounts, and never
  between the same account. You cannot transfer from balances in different
  currencies. You cannot debit from an account that would result in a negative
  balance. They only handle USD and CAD currencies. Finally, a transfer always
  debits and credits the same amount from the account debited to the account
  credited.

   They refer to specific accounts by account number, and the balance on it can
  change over time and still they consider it the same account. Therefore
  account is an entity. They don't refer to specific monetary amounts, credits,
  debits or balances, those are always referred within the context of an account
  or a transfer, which means they can be value objects. Finally, they do refer
  to transfers by a number, but also don't consider a transfer something you can
  change after the fact, they serve as transactional records only, therefore
  they'll be immutable entities. We don't actually have any aggregate, even
  though a transfer conceptually describes a change between two accounts, we
  don't need a transactional boundary around transfer that makes it the only way
  for us to modify accounts or transfers, we might want to change accounts
  independently of transfers for some other use cases. Thus the transfer of
  money that reflects on the accounts and on the transfers is best handled by a
  domain service.

  In DDD, an entity is a concept that has an identity, it means the domain cares
  about referring to something by their name or some identifier, and regardless
  of their value, the name/identifier tells you what they are and can
  distinguish two entities of the same value. In general, entities will have
  their value change over time, while retaining the same name/identity, like our
  account entity. That said, this isn't always the case, and an entity can exist
  for which its value cannot change over time, such as our transfer entity. In
  those cases, they are still entities, because they still are referred to by
  their name/identity, and there can exist two of them with the same value but a
  different name/identity, and they are considered by the domain as distinct.
  Thus an entity is given existence by its name or some identifier, and a value
  is attached to it, but could change over time, and sometimes might not even
  have any attached value.

  A value object on the other hand is a concept which the domain doesn't assign
  to instances of it a name or identifier. The domain always refers to these by
  their value. That means their value gives them existence, they are simply a
  set of possible values with some invariants over what value they can have at
  any given time. When you speak of two value objects as having the same value,
  you speak of the same conceptual value object, and don't care to distinguish
  them otherwise.

  An aggregate is a set of entities that the domain refers too at times, in the
  context of making changes to the domain state, or performing actions related
  to multiple domain entities. Think of a grape for example. If in the domain,
  people only ever talk about grapes, grapes could be entities. But if in the
  domain, people talk about the grape, as well as the stem and the berries, you
  might say that the stem is an entity, berries are entities, but the grape is
  an aggregate. It exist as a concept in the domain, but it is made up of other
  entities. It turns out, the grape itself is also an entity, but since it
  contains other entities, it forms an aggregate as well. If it only contained
  value objects, it would just be an entity, and not an aggregate. The entity
  that contains the other entities in an aggregate is called the aggregate root.
  You could have state attached to both the aggregate as a whole, or the
  aggregate root itself. For example, the grape has a weight, and the stem also
  has a weight. That means you probably want an entity Grape which will have
  weight and also be the aggregate root. Its properties model that of the
  aggregate as a whole, while it contains a stem which itself contains berries.
  The weight property on the stem would be of the stem only, and on the berries
  of the berries only, but on the grape it would be of the sum total of the stem
  and all its berries.

  Here it helps to think inside-out. Your value objects are state, and there are
  consistency rules around them, which means what value can they hold which is
  consistent with what is conceptually logical in the domain. Your entities are
  state as well, which also means what value can they hold which is consistent
  with what is conceptually logical in the domain. Your aggregates also are
  state, which also means what value can they hold which is consistent with what
  is conceptually logical in the domain.

  Entities and Value Objects only capture the rules related to their valid
  state. Is the data in them valid? Can the data in them change in this way?

  An aggregate root does the same for a group of entities. Is the data in the
  aggregate valid? Can the data in the aggregate change in this way?

  Domain services capture rules about changes across entities or aggregates. Can
  we change X and Y this way?

   In OOP, these are normally modeled using classes with validation done in the
  constructors. They are often mutable, where changes to each are implemented as
  methods that check if the change can be done, and if so mutates the object's
  fields accordingly or throws. In Clojure, they are modeled as specs alongside
  functions. The spec specifies the set of all valid states, since specs are
  Turing complete, as they are based on predicates, they can express those,
  while in OOP, normally, types are too restrictive on the class, that's why
  additional validation must be done in the constructors or methods. Sometimes,
  some rules need to consider other entities or value objects, in order to
  change one entity or aggregate, you have to check properties about others, in
  those cases spec is often more complicated to rely on, so functions can simply
  encode these rules within them, similar to how methods in OOP do so.

  So specs describes our entities, value objects and aggregate roots, and can be
  used to validate arbitrary Clojure immutable data to see if they are valid to
  our domain rules and invariants. The domain model in DDD though is also used
  to provide functions to make the changes, that's why you will have functions
  for making an instance of each entity, often just returning a Clojure map
  valid to the spec. You'll also have functions that can update an entity or
  aggregate, such as debiting to an account. Those functions often return an
  updated copy of the entity with the change applied, but just like domain
  services, in theory they return a domain event about the change if it is
  allowed which describes how to correctly to the domain rules commit the change
  to the actual state and perform all side effects. In our case, returning the
  updated entity is good enough, but depending on your use case and what the
  application domain has to do, the function could return a domain event. For
  example, you could have a send-promo function on an Account, and maybe the
  domain rule is that at a balance of 1000 or more, we should send you a promo
  with a 10$ coupon. In that case, the send-promo function would take an
  account, but it would not return only a modified account, it would maybe
  return a modified account that shows the promo on it as sent, and it also
  would return an event that describes the promo of 10$ coupon. The application
  service can then interpret those to update the account state and to actually
  send the promo maybe as an email.

  Generally, when a change or computation that results in action only involves
  one entity or one aggregate, you add those to the domain model directly, while
  if they involve more than one entity or aggregate, you would add those as
  domain services. In OOP, that matters a lot if the method is on the entity
  class or not, but in Clojure its all the same as they are all just functions,
  so it is more organizational to know where to look for things. Don't get too
  bogged down by if something should be a domain service function, or a function
  in the domain model, honestly, you could even shove them all in the same
  namespace if you wanted, or put each entity in its own namespace. What really
  matters is that you have specs for entities, value objects and aggregates, and
  functions to make and validate them, and functions to modify or take actions
  that result in domain events, and that those functions are always pure and
  part of the functional core, having dependencies only on the domain model
  itself and nothing else. Feel free to replace spec with some other Clojure
  predicate data validation lib, like Malli for example as well."
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]))


;;;;;;;;;;;;;;;;;;;;
;;; Shared Specs ;;;

(def currency? #{:usd :cad})

;;; Shared Specs ;;;
;;;;;;;;;;;;;;;;;;;;


;;;;;;;;;;;;;;
;;; Amount ;;;

(s/def :amount/currency
  currency?)

(s/def :amount/value
  (s/and number? pos?))

(s/def :amount/amount
  (s/keys :req-un [:amount/currency
                   :amount/value]))

(defn make-amount
  [value currency]
  (s/assert :amount/amount
            {:currency currency
             :value value}))

;;; Amount ;;;
;;;;;;;;;;;;;;


;;;;;;;;;;;;;;;
;;; Balance ;;;

(s/def :balance/value
  number?)

(s/def :balance/currency
  currency?)

(s/def :balance/balance
  (s/keys :req-un [:balance/currency
                   :balance/value]))

(defn make-balance
  [value currency]
  (s/assert :balance/balance
            {:currency currency
             :value value}))

;;; Balance ;;;
;;;;;;;;;;;;;;;


;;;;;;;;;;;;;;;
;;; Account ;;;

(s/def :account/number
  (s/with-gen
    (s/and string? #(re-matches #"[1-9]{12}" %))
    #(s/gen #{"273648898836" "111234871234" "998877324561"})))

(s/def :account/account
  (s/keys :req-un [:account/number
                   :balance/balance]))

(defn make-account
  [account-number balance]
  (s/assert :account/account
            {:number account-number
             :balance balance}))

(defn debit-account
  "Returns the account with the debit applied or throws if we can't debit."
  [account debit]
  (if
      (and
       (= (-> account :balance :currency) (-> debit :amount :currency))
       (= (-> account :number) (-> debit :number))
       (pos? (- (-> account :balance :value) (-> debit :amount :value))))
    (s/assert :account/account
              (update account :balance update :value - (-> debit :amount :value)))
    (throw (ex-info "Can't debit account" {:type :illegal-operation
                                           :action :debit-account
                                           :account account
                                           :debit debit}))))

(defn credit-account
  "Returns the account with the credit applied or throws if we can't credit."
  [account credit]
  (if
      (and
       (= (-> account :balance :currency) (-> credit :amount :currency))
       (= (-> account :number) (-> credit :number)))
    (s/assert :account/account
              (update account :balance update :value + (-> credit :amount :value)))
    (throw (ex-info "Can't credit account" {:type :illegal-operation
                                            :account account
                                            :credit credit}))))

;;; Account ;;;
;;;;;;;;;;;;;;;


;;;;;;;;;;;;;
;;; Debit ;;;

(s/def :debit/debit
  (s/keys :req-un [:account/number
                   :amount/amount]))

(defn make-debit
  [account-number amount]
  (s/assert :debit/debit
            {:number account-number
             :amount amount}))

;;; Debit ;;;
;;;;;;;;;;;;;


;;;;;;;;;;;;;;
;;; Credit ;;;

(s/def :credit/credit
  (s/keys :req-un [:account/number
                   :amount/amount]))

(defn make-credit
  [account-number amount]
  (s/assert :credit/credit
            {:number account-number
             :amount amount}))

;;; Credit ;;;
;;;;;;;;;;;;;;


;;;;;;;;;;;;;;;;
;;; Transfer ;;;

(s/def :transfer/id
  uuid?)

(s/def :transfer/number
  (s/with-gen
    (s/and string? #(re-matches #"[A-Z]{3}[1-9]{8}" %))
    #(s/gen #{"AAA121468335" "ZBA88736112" "JGH36684291"})))

(s/def :transfer/creation-date
  inst?)

(s/def :transfer/transfer
  (s/and
   (s/keys :req-un [:transfer/id
                    :transfer/number
                    :debit/debit
                    :credit/credit
                    :transfer/creation-date])
   (fn[{:keys [debit credit]}]
     (and (= (:amount debit) (:amount credit))
          (not= (:number debit) (:number credit))))))

(defn make-transfer
  [transfer-number debit credit]
  (s/assert :transfer/transfer
            {:id (random-uuid)
             :number transfer-number
             :debit debit
             :credit credit
             :creation-date (java.util.Date.)}))

;;; Transfer ;;;
;;;;;;;;;;;;;;;;
