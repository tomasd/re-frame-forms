(ns re-frame-forms.protocols)


(defprotocol Value
  "Protocol for value holder"
  (value
    [this default]
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

(defprotocol DelayedValidation
  (start-validation! [this])
  (mark-ok! [this])
  (mark-error! [this error]))

(defprotocol DelayValidationContainer
  (validation-in-progress? [this]))