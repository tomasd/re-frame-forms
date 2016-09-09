(ns re-frame-forms.core
  (:require
    [reagent.core :as reagent]
    [re-frame-forms.coerce :as coerce]
    [re-frame-forms.validation :as validation])
  (:require-macros
    [reagent.ratom :refer [reaction]]))

(defprotocol Value
  "Protocol for value holder"
  (value
    [this] [this default]
    "Get reaction for current value")
  (set-value!
    [this val]
    "Set current value"))

(defprotocol PersistentError
  "Persistent error is retained between validations. Has to be explicitly set and cleared."
  (set-error!
    [this error]
    "Set 1 error for the field. Error can be cleared by calling clear-error!"))

(defn clear-error!
  "Clear field error"
  [this]
  (set-error! this nil))

(defprotocol ResetValue
  "Protocol for accessing original value"
  (original-value
    [this]
    "Get original value of the form/field")
  (reset-value!
    [this]
    "Reset form/field to the original value, clear errors"))

(defprotocol CoercedValue
  "Field protocol converting to/from string"
  (str-value
    [this]
    "Get reaction for current value converted to string")
  (set-str-value!
    [this val]
    "Set current value by converting val from string"))

(defprotocol ErrorContainer
  "Field/form errors"
  (errors
    [this]
    "Get reaction for current field/form errors"))

(defprotocol Validatable
  "Field/form validation status"
  (valid?
    [this]
    "Get reaction for current validation status of the form/field"))

(defprotocol Touchable
  "Protocol for forced validation."
  (touch!
    [this]
    "Touch the field/form")
  (touched?
    [this]
    "Get reaction with current touch status"))

(defn- field-path [type path]
  (if (#{::value ::original} type)
    (cons type path)
    [type path]))

(defn- assoc-field [form path & kvs]
  (reduce (fn [form [type value]]
            (assoc-in form (field-path type path) value))
          form
          (partition 2 kvs)))

(defn- path-value
  ([form type path]
   (path-value form type path nil))
  ([form type path default]
   (let [{:keys [value]} form]
     (reaction (get-in @value (field-path type path) default)))))

(defn- path-errors [form path]
  (let [{:keys [value]} form]
    (reaction (validation/field-errors (::validator-errors @value) path))))

(defn- validate-field [validator value]
  (if (instance? validation/FieldValidator validator)
    (validation/validate-field validator val)
    []))

(defrecord Field [form coercer validator path]
  Value
  (value [this]
    (value this nil))
  (value [_ default]
    (path-value form ::value path default))
  (set-value! [_ val]
    (swap! form assoc-field path
           ::field-errors (validation/validate-field validator val)
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
        (or str-value (coerce/to-str coercer value)))))
  (set-str-value! [this val]
    (if (coerce/valid-str? coercer val)
      (set-value! this
                  (coerce/from-str coercer val))
      (swap! form assoc-field path
             ::field-errors (validate-field coercer val)
             ::coercion-error true
             ::value nil
             ::tmp val
             ::field-touched true)))

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
  (touch! [this]
    (swap! form assoc-field path
           ::field-touched true))
  (touched? [_]
    (reaction
      (let [form-touched @(touched? form)
            path-touched @(path-value form ::field-touched path false)]
        (or form-touched path-touched)))))

(defn- validate-form
  ([validator f]
   (fn [value & args]
     (validate-form value validator f args)))
  ([value validator f & args]
   (let [new-value (apply f value args)]
     (-> new-value
         (assoc ::validator-errors (validation/validate-form validator (::value new-value)))))))

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

  Validatable
  (valid? [_]
    (reaction (and
                (->> (::coercion-error @value)
                     vals
                     (filter identity)
                     empty?)
                (->> (concat
                       (::field-errors @value)
                       (::persistent-error @value))
                     vals
                     (remove empty?)
                     empty?)
                (validation/valid? (::validator-errors @value)))))

  Touchable
  (touch! [this]
    (swap! value assoc ::form-touched true))
  (touched? [this]
    (reaction (::form-touched @value false))))

(defn field
  ([form path]
   (field form path nil nil))
  ([form path type]
   (field form path type nil))
  ([form path type validator]
   (let [coercer (if (instance? coerce/Coercer type)
                   type
                   (coerce/create-coercer type))]
     (->Field form coercer validator path))))

(defn create-form
  ([value]
   (create-form value nil))
  ([value validator]
   (->Form (reagent/atom {::value    value
                          ::original value})
           validator)))
