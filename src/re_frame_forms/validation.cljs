(ns re-frame-forms.validation)

(defprotocol FieldValidator
  "Validate field"
  (validate-field
    [this value]
    "Return vector of errors for the field. Empty vector if valie"))

(defprotocol FormValidator
  "Validate form"
  (validate-form
    [this value]
    "Return instance of ValidationResult."))

(defprotocol ValidationResult
  "Holder for validation result"
  (field-errors
    [this path]
    "Get errors for the field on path")
  (valid?
    [this]
    "Is this form valid?"))

(deftype ValidResult []
  ValidationResult
  (field-errors [this path]
    [])
  (valid? [this] true))

(deftype MapValidationResult [errors]
  ValidationResult
  (field-errors [_ path]
    (remove nil? [(get-in errors path)]))
  (valid? [_]
    (empty? errors)))

(defn validation-result [result]
  (->MapValidationResult result))

(defn form-validator [f]
  (reify
    FormValidator
    (validate-form [_ value]
      (f value))))

(extend-type nil
  ValidationResult
  (field-errors [this path]
    [])
  (valid? [this] true)

  FieldValidator
  (validate-field [this value]
    [])

  FormValidator
  (validate-form [this value]
    (->ValidResult)))