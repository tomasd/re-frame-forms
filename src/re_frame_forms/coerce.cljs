(ns re-frame-forms.coerce
  (:refer-clojure :exclude [keyword int])
  (:require
    [clojure.string :as str]
    [re-frame-forms.format :as fmt]
    ))

(defprotocol Coercer
  (to-str [this obj-value])
  (from-str [this str-value])
  (valid-str? [this str-value]))

(defn- normalize-num-str [s]
  (-> s
      (str/replace-all #"," ".")
      (str/replace-all #"\s" "")))

(deftype IntCoercer [allow-blank? format parse]
  Coercer
  (to-str [_ obj] (if (nil? obj) "" (format obj)))
  (from-str [_ s] (if (str/blank? s) nil (parse s)))
  (valid-str? [_ s]
    (boolean (or (and allow-blank? (str/blank? s))
                 (re-matches #"(\+|\-)?[\s\d]+" s)))))
(defn int
  ([]
   (int "0"))
  ([format]
   (->IntCoercer true (fmt/format format) #(-> % normalize-num-str js/parseInt))))


(deftype NumberCoercer [allow-blank? format parse]
  Coercer
  (to-str [_ obj] (if (nil? obj) "" (format obj)))
  (from-str [_ s] (if (str/blank? s) nil (parse s #_(str/replace-all s #"," "."))))
  (valid-str? [_ s]
    (boolean (or (and allow-blank? (str/blank? s))
                 (re-matches #"(\+|\-)?[\s\d]+(?:(\.|,)[\s\d]+)?" s)))))
(defn number
  ([]
   (number "0.####"))
  ([format]
   (->NumberCoercer true (fmt/format format) #(-> % normalize-num-str js/parseFloat))))

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
  (to-str [_ obj] (if (nil? obj) "" (name obj)))
  (from-str [_ s] (if (str/blank? s) nil (cljs.core/keyword s)))
  (valid-str? [_ s] (not (str/blank? s))))
(defn keyword []
  (->KeywordCoercer))

(deftype DateCoercer [format parse]
  Coercer
  (to-str [_ obj] (if (nil? obj) "" (format obj)))
  (from-str [_ s] (if (str/blank? s) nil (parse s)))
  (valid-str? [_ s] (boolean (or (str/blank? s)
                                 (not (nil? (parse s)))))))

(defn date [pattern]
  (->DateCoercer (fmt/format-date pattern) (fmt/parse-date pattern)))

(extend-type nil
  Coercer
  (to-str [_ obj] (str obj))
  (from-str [_ s] s)
  (valid-str? [_ _] true))