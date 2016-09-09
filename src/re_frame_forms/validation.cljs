(ns re-frame-forms.validation)

(defprotocol FieldValidator
  (validate-field [this value]))

(defprotocol FormValidator
  (validate-form [this value]))

(defprotocol ValidationResult
  (field-errors [this path])
  (valid? [this]))

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