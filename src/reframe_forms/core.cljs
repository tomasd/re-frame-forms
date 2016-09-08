(ns reframe-forms.core
  (:require
    [clojure.string :as str]
    [reagent.core :as reagent]
    )
  (:require-macros
    [reagent.ratom :refer [reaction]]))

(defprotocol Value
  (value [this]
         [this default])
  (set-value! [this val]))

(defprotocol PersistentError
  (set-error! [this error]))
(defn clear-error! [this]
  (set-error! this nil))

(defprotocol ResetValue
  (original-value [this])
  (reset-value! [this]))

(defprotocol Coercer
  (to-str [this obj-value])
  (from-str [this str-value])
  (valid-str? [this str-value]))

(defprotocol PathValue
  (path-value [this type path]
              [this type path default]))

(defprotocol PathErrors
  (path-errors [this path]))

(defprotocol CoercedValue
  (str-value [this])
  (set-str-value! [this val]))

(defprotocol ErrorContainer
  (errors [this]))

(defprotocol Validatable
  (valid? [this]))

(defprotocol Validator
  (validate [this value]))

(defprotocol Touchable
  (touch [this])
  (touched? [this]))

(defn- field-path [type path]
  (if (#{::value ::original ::validator-errors} type)
    (cons type path)
    [type path]))

(defn- assoc-field [form path & kvs]
  (reduce (fn [form [type value]]
            (assoc-in form (field-path type path) value))
          form
          (partition 2 kvs)))

(defrecord Field [form coercer validator path]
  Value
  (value [this]
    (value this nil))
  (value [_ default]
    (path-value form ::value path default))
  (set-value! [_ val]
    (swap! form assoc-field path
           ::field-errors (validate validator val)
           ::coercion-error false
           ::value val
           ::tmp nil
           ::field-touched true))

  ResetValue
  (original-value [this]
    @(path-value form ::original path nil))
  (reset-value! [this]
    (swap! form assoc-field path
           ::field-errors []
           ::coercion-error false
           ::value (original-value this)
           ::tmp nil
           ::field-touched false))

  CoercedValue
  (str-value [this]
    (reaction
      (let [str-value @(path-value form ::tmp path nil)
            value     @(value this)]
        (or str-value (to-str coercer value)))))
  (set-str-value! [this val]
    (if (valid-str? coercer val)
      (->> (from-str coercer val)
           (set-value! this))
      (let [errors (if (instance? Validator coercer)
                     (validate coercer errors)
                     [])]
        (swap! form assoc-field path
               ::field-errors errors
               ::coercion-error true
               ::value nil
               ::tmp val
               ::field-touched true))))

  ErrorContainer
  (errors [this]
    (reaction (->> (concat @(path-value form ::field-errors path nil)
                           @(path-errors form path)
                           [@(path-value form ::persistent-error path nil)])
                   (remove empty?)
                   (remove nil?))))

  PersistentError
  (set-error! [_ error]
    (swap! form assoc-field path ::persistent-error error))

  Validatable
  (valid? [this]
    (reaction (and (not @(path-value form ::coercion-error path false))
                   (empty? @(errors this)))))

  Touchable
  (touch [this]
    (swap! form assoc-field path
           ::field-touched true))
  (touched? [_]
    (reaction
      (let [form-touched @(touched? form)
            path-touched @(path-value form ::field-touched path false)]
        (or form-touched path-touched)))))


(defmulti create-coercer identity)
(defmethod create-coercer :default
  [_] (reify
        Coercer
        (to-str [_ obj] (str obj))
        (from-str [_ s] s)
        (valid-str? [_ _] true)

        Validator
        (validate [_ s])))

(deftype IntCoercer [allow-blank?]
  Coercer
  (to-str [_ obj] (str obj))
  (from-str [_ s] (if (str/blank? s) nil (js/parseInt s)))
  (valid-str? [_ s]
    (boolean (or (and allow-blank? (str/blank? s))
                 (re-matches #"(\+|\-)?\d+" s)))))

(deftype BoolCoercer [blank-as-false?]
  Coercer
  (to-str [_ obj] (str obj))
  (from-str [_ s] (if (and blank-as-false? (str/blank? s))
                    false
                    (boolean s)))
  (valid-str? [_ s] true))

(deftype KeywordCoercer []
  Coercer
  (to-str [_ obj] (name obj))
  (from-str [_ s] (keyword s))
  (valid-str? [_ s] (not (str/blank? s))))

(defmethod create-coercer :int
  [_] (->IntCoercer true))
(defmethod create-coercer :keyword
  [_] (->KeywordCoercer))
(defmethod create-coercer :bool
  [_] (->BoolCoercer true))


(defn- validate-form
  ([validator f]
   (fn [value & args]
     (validate-form value validator f args)))
  ([value validator f & args]
   (let [new-value (apply f value args)]
     (-> new-value
         (assoc ::validator-errors (validate validator (::value new-value)))))))

(def ->coercer (memoize create-coercer))

(extend-type Keyword
  Coercer
  (to-str [this obj-value]
    (to-str (->coercer this) obj-value))
  (from-str [this str-value]
    (from-str (->coercer this) str-value))
  (valid-str? [this str-value]
    (valid-str? (->coercer this) str-value)))

(extend-type nil
  Validator
  (validate [this value]
    nil)

  Validatable
  (valid? [this]
    true)

  PathErrors
  (path-errors [this path]
    [])

  Coercer
  (to-str [_ obj] (str obj))
  (from-str [_ s] s)
  (valid-str? [_ _] true))

(defrecord Form [value validator]
  ISwap
  (-swap! [o f]
    (swap! value validate-form validator f))
  (-swap! [o f a]
    (swap! value validate-form validator f a))
  (-swap! [o f a b]
    (swap! value validate-form validator f a b))
  (-swap! [o f a b xs]
    (apply swap! value validate-form validator f a b xs))

  Value
  (value [this]
    (reaction (get @value ::value)))
  (value [this default]
    (reaction (get @value ::value default)))
  (set-value! [this val]
    (swap! value
           validate-form assoc ::value val))

  ResetValue
  (original-value [this]
    (get @value ::original))
  (reset-value! [this]
    (swap! value (fn [value]
                   {::value    (::original value)
                    ::original (::original value)})))

  PathValue
  (path-value [_ type path]
    (path-value _ type path nil))
  (path-value [_ type path default]
    (reaction (get-in @value (field-path type path) default)))

  PathErrors
  (path-errors [_ path]
    (reaction (path-errors (::validator-errors @value) path)))

  Validatable
  (valid? [_]
    (reaction (and
                (->> (concat (::field-errors @value)
                             (::persistent-error @value))
                     vals
                     (remove empty?)
                     empty?)
                (valid? (::validator-errors @value)))))

  Touchable
  (touch [this]
    (swap! value assoc ::form-touched true))
  (touched? [this]
    (reaction (::form-touched @value false))))

(defn field
  ([form path]
   (field form path nil nil))
  ([form path type]
   (field form path type nil))
  ([form path type validator]
   (let [coercer (if (instance? Coercer type)
                   type
                   (create-coercer type))]
     (->Field form coercer validator path))))



(deftype ValidationResult [errors]
  PathErrors
  (path-errors [this path]
    (remove nil? [(get-in errors path)]))

  Validatable
  (valid? [this]
    (empty? errors)))

(defn validation-result [result]
  (->ValidationResult result))


(defn create-form
  ([value]
   (create-form value nil))
  ([value validator]
   (->Form (reagent/atom {::value    value
                          ::original value
                          })
           validator)))

(defn handle-str-value [field]
  #(set-str-value! field (-> % .-target .-value)))

(defn handle-checked-value [field]
  #(set-value! field (-> % .-target .-checked)))

(defn handle-valid-form [form callback]
  (fn [e]
    (touch form)
    (when @(valid? form)
      (callback @(value form)))
    (.preventDefault e)))











