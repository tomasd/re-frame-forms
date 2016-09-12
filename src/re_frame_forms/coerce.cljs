(ns re-frame-forms.coerce
  (:require
    [clojure.string :as str]
    ))

(defprotocol Coercer
  (to-str [this obj-value])
  (from-str [this str-value])
  (valid-str? [this str-value]))


(deftype IntCoercer [allow-blank?]
  Coercer
  (to-str [_ obj] (str obj))
  (from-str [_ s] (if (str/blank? s) nil (js/parseInt s)))
  (valid-str? [_ s]
    (boolean (or (and allow-blank? (str/blank? s))
                 (re-matches #"(\+|\-)?\d+" s)))))

(deftype NumberCoercer [allow-blank?]
  Coercer
  (to-str [_ obj] (str obj))
  (from-str [_ s] (if (str/blank? s) nil (js/parseFloat (str/replace-all s #"," "."))))
  (valid-str? [_ s]
    (boolean (or (and allow-blank? (str/blank? s))
                 (re-matches #"(\+|\-)?\d+(?:(\.|,)\d+)" s)))))

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

(defmulti make-coercer identity)

(defmethod make-coercer :default
  [_] (reify
        Coercer
        (to-str [_ obj] (str obj))
        (from-str [_ s] s)
        (valid-str? [_ _] true)
        ))
(defmethod make-coercer :int
  [_] (->IntCoercer true))
(defmethod make-coercer :number
  [_] (->NumberCoercer true))
(defmethod make-coercer :keyword
  [_] (->KeywordCoercer))
(defmethod make-coercer :bool
  [_] (->BoolCoercer true))

(def ->coercer (memoize make-coercer))
(extend-type Keyword
  Coercer
  (to-str [this obj-value]
    (to-str (->coercer this) obj-value))
  (from-str [this str-value]
    (from-str (->coercer this) str-value))
  (valid-str? [this str-value]
    (valid-str? (->coercer this) str-value)))

(extend-type nil
  Coercer
  (to-str [_ obj] (str obj))
  (from-str [_ s] s)
  (valid-str? [_ _] true))