(ns re-frame-forms.core
  (:require
    [re-frame-forms.protocols :as proto]
    [re-frame-forms.impl :as impl])
  (:require-macros
    [reagent.ratom :refer [reaction]]))


(defn value
  "Get reaction for current value"
  ([form-or-field]
   (value form-or-field nil))
  ([form-or-field default]
   (proto/value form-or-field default)))

(defn set-value!
  "Set current value"
  [form-or-field value]
  (proto/set-value! form-or-field value))


(defn set-error!
  "Set 1 error for the field. Error can be cleared by calling clear-error!"
  [field error]
  (proto/set-error! field error))

(defn clear-error!
  "Clear field error"
  [field]
  (set-error! field nil))


(defn original-value
  "Get original value of the form/field"
  [form-or-field]
  (proto/original-value form-or-field))
(defn reset-value!
  "Reset form/field to the original value, clear errors"
  [form-or-field]
  (proto/reset-value! form-or-field))


(defn str-value
  "Get reaction for current value converted to string"
  [field]
  (proto/str-value field))
(defn set-str-value!
  "Set current value by converting val from string"
  [field value]
  (proto/set-str-value! field value))


(defn errors
  "Get reaction for current field/form errors"
  [field]
  (proto/errors field))


(defn valid?
  "Get reaction for current validation status of the form/field"
  [form-or-field]
  (proto/valid? form-or-field))


(defn touch!
  "Touch the field/form"
  [form-or-field]
  (proto/touch! form-or-field))
(defn touched?
  "Get reaction with current touch status"
  [form-or-field]
  (proto/touched? form-or-field))

(defn make-field
  ([form path]
   (make-field form path nil nil))
  ([form path type]
   (make-field form path type nil))
  ([form path type validator]
   (impl/make-field form path type validator)))

(defn make-form
  ([value]
   (make-form value nil))
  ([value validator]
   (impl/make-form value validator)))
