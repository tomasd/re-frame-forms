(ns re-frame-forms.format
  (:import
    [goog.i18n NumberFormat DateTimeFormat DateTimeParse]))

(let [memo (atom {})]
  (defn make-number-format
    ([pattern]
     (make-number-format pattern nil nil))
    ([pattern opt_currency]
     (make-number-format pattern opt_currency nil))
    ([pattern opt_currency opt_currencyStyle]
     (let [path   [goog.LOCALE pattern opt_currency opt_currencyStyle]
           format (get-in @memo path)]
       (if (nil? format)
         (let [format (NumberFormat. pattern opt_currency opt_currencyStyle)]
           (swap! memo assoc-in path format)
           format)
         format)))))

(let [memo (atom {})]
  (defn make-datetime-format
    ([pattern]
     (make-datetime-format pattern nil))
    ([pattern opt_dateTimeSymbols]
     (let [path   [goog.LOCALE pattern opt_dateTimeSymbols]
           format (get-in @memo path)]
       (if (nil? format)
         (let [format (DateTimeFormat. pattern opt_dateTimeSymbols)]
           (swap! memo assoc-in path format)
           format)
         format)))))

(let [memo (atom {})]
  (defn make-datetime-parser
    ([pattern]
     (make-datetime-parser pattern nil))
    ([pattern opt_dateTimeSymbols]
     (let [path   [goog.LOCALE pattern opt_dateTimeSymbols]
           format (get-in @memo path)]
       (if (nil? format)
         (let [format (DateTimeParse. pattern opt_dateTimeSymbols)]
           (swap! memo assoc-in path format)
           format)
         format)))))

(defn formatter [pattern]
  (make-number-format pattern))

(defn int-format []
  (make-number-format "#,##0"))

(defn decimal-format []
  (make-number-format NumberFormat.Format.DECIMAL))

(defn currency-format []
  (make-number-format NumberFormat.Format.CURRENCY))

(defn percent-format []
  (make-number-format NumberFormat.Format.PERCENT))

(defn format
  ([pattern]
   (fn [n]
     (format pattern n)))
  ([pattern n]
   (.format (make-number-format pattern) n)))

(defn parse
  ([pattern]
   (fn [s]
     (parse pattern s)))
  ([pattern s]
   (.parse (make-number-format pattern) s)))

(defn format-int
  ([n]
   (.format (int-format) n)))
(defn parse-int [n]
  (.parse (int-format) n))

(defn format-decimal [n]
  (.format (decimal-format) n))
(defn parse-decimal [n]
  (.parse (decimal-format) n))

(defn format-currency [n]
  (.format (currency-format) n))
(defn parse-currency [n]
  (.parse (currency-format) n))

(defn format-percent [n]
  (.format (currency-format) n))
(defn parse-percent [n]
  (.parse (currency-format) n))

(defn format-date
  ([pattern]
   (fn [value]
     (format-date pattern value)))
  ([pattern d]
   (.format (make-datetime-format pattern) d)))

(defn parse-date
  ([pattern]
   (fn [value]
     (parse-date pattern value)))
  ([pattern d]
   (let [date (js/Date. 0)]
     (if-not (= 0 (.strictParse (make-datetime-parser pattern) d date))
       (if (> (.getYear date) -900)
         date
         nil)
       nil))))