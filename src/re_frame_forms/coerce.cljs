(ns re-frame-forms.coerce
  (:refer-clojure :exclude [keyword int])
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
(defn int []
  (->IntCoercer true))


(deftype NumberCoercer [allow-blank?]
  Coercer
  (to-str [_ obj] (str obj))
  (from-str [_ s] (if (str/blank? s) nil (js/parseFloat (str/replace-all s #"," "."))))
  (valid-str? [_ s]
    (boolean (or (and allow-blank? (str/blank? s))
                 (re-matches #"(\+|\-)?\d+(?:(\.|,)\d+)" s)))))
(defn number []
  (->NumberCoercer true))

(deftype BoolCoercer [blank-as-false?]
  Coercer
  (to-str [_ obj] (str obj))
  (from-str [_ s] (if (and blank-as-false? (str/blank? s))
                    false
                    (boolean s)))
  (valid-str? [_ s] true))
(defn bool []
  (->BoolCoercer true))

(deftype KeywordCoercer []
  Coercer
  (to-str [_ obj] (name obj))
  (from-str [_ s] (cljs.core/keyword s))
  (valid-str? [_ s] (not (str/blank? s))))
(defn keyword []
  (->KeywordCoercer))

(extend-type nil
  Coercer
  (to-str [_ obj] (str obj))
  (from-str [_ s] s)
  (valid-str? [_ _] true))