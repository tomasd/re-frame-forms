(ns re-frame-forms.impl
  (:require
    [re-frame-forms.coerce :as coerce]
    [re-frame-forms.protocols :as proto]
    [re-frame-forms.validation :as validation]
    [reagent.core :as reagent]
    [reagent.ratom :refer-macros [reaction]]))


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

(defn- validate-field [validator val]
  (if (instance? validation/FieldValidator validator)
    (validation/validate-field validator val)
    []))

(defrecord Field [form coercer validator path]
  proto/Value
  (value [_ default]
    (path-value form ::value path default))
  (set-value! [_ val]
    (swap! form assoc-field path
           ::field-errors (validation/validate-field validator val)
           ::coercion-error false
           ::value val
           ::tmp nil
           ::field-touched true))

  proto/ResetValue
  (original-value [_]
    @(path-value form ::original path nil))
  (reset-value! [this]
    (swap! form assoc-field path
           ::field-errors []
           ::coercion-error false
           ::value (proto/original-value this)
           ::persistent-error nil
           ::delayed-validation false
           ::tmp nil
           ::field-touched false))

  proto/CoercedValue
  (str-value [this]
    (reaction
      (let [str-value @(path-value form ::tmp path nil)
            value     @(proto/value this nil)]
        (or str-value (coerce/to-str coercer value)))))
  (set-str-value! [this val]
    (if (coerce/valid-str? coercer val)
      (proto/set-value! this
                        (coerce/from-str coercer val))
      (swap! form assoc-field path
             ::field-errors (validate-field coercer val)
             ::coercion-error true
             ::value nil
             ::tmp val
             ::field-touched true)))

  proto/ErrorContainer
  (errors [_]
    (reaction (->> (concat @(path-value form ::field-errors path nil)
                           @(path-errors form path)
                           [@(path-value form ::persistent-error path nil)])
                   (remove empty?)
                   (remove nil?))))

  proto/PersistentError
  (set-error! [_ error]
    (swap! form assoc-field path ::persistent-error error))

  proto/Validatable
  (valid? [this]
    (reaction (and (not @(path-value form ::coercion-error path false))
                   (empty? @(proto/errors this)))))

  proto/Touchable
  (touch! [_]
    (swap! form assoc-field path
           ::field-touched true))
  (touched? [_]
    (reaction
      (let [form-touched @(proto/touched? form)
            path-touched @(path-value form ::field-touched path false)]
        (or form-touched path-touched))))

  proto/DelayedValidation
  (start-validation! [_]
    (swap! form assoc-field path ::delayed-validation true))

  (mark-ok! [_]
    (swap! form assoc-field path
           ::persistent-error nil
           ::delayed-validation false))
  (mark-error! [_ error]
    (swap! form assoc-field path
           ::persistent-error error
           ::delayed-validation false))

  proto/DelayValidationContainer
  (validation-in-progress? [_]
    (reaction @(path-value form ::delayed-validation path false))))

(defn- validate-form
  ([validator f]
   (fn [value & args]
     (validate-form value validator f args)))
  ([value validator f & args]
   (let [new-value (apply f value args)]
     (-> new-value
         (assoc ::validator-errors (validation/validate-form validator (::value new-value)))))))

(defn- validation-in-progress? [form-value]
  (->> (::delayed-validation form-value {})
       vals
       (filter true?)
       not-empty)
  )

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

  proto/Value
  (value [_ default]
    (reaction (get @value ::value default)))
  (set-value! [_ val]
    (swap! value
           validate-form assoc ::value val))

  proto/ResetValue
  (original-value [_]
    (get @value ::original))
  (reset-value! [_]
    (swap! value (fn [value]
                   {::value    (::original value)
                    ::original (::original value)})))

  proto/Validatable
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
                (validation/valid? (::validator-errors @value))
                (not (validation-in-progress? @value)))))

  proto/Touchable
  (touch! [_]
    (swap! value assoc ::form-touched true))
  (touched? [_]
    (reaction (::form-touched @value false)))

  proto/DelayValidationContainer
  (validation-in-progress? [_]
    (reaction (validation-in-progress? @value) )))

(defn make-field [form path type validator]
  (let [coercer (if (instance? coerce/Coercer type)
                  type
                  (coerce/make-coercer type))]
    (->Field form coercer validator path)))

(defn make-form [value validator]
  (->Form (reagent/atom {::value    value
                         ::original value})
          validator))