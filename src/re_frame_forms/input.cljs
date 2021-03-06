(ns re-frame-forms.input
  (:require
    [re-frame-forms.core :as form]))

(defn radio
  ([field value]
   (radio field value {}))
  ([field value attrs]
   (fn [field value attrs]
     [:input (merge attrs
                    {:type      "radio"
                     :value     value
                     :on-change (form/handle-str-value field)
                     :checked   (= @(form/value field) value)})])))

(defn checkbox
  ([field]
   (checkbox field {}))
  ([field attrs]
   (fn [field attrs]
     [:input (merge attrs
                    {:type      "checkbox"
                     :checked   @(form/value field)
                     :on-change (form/handle-checked-value field)})])))

(defn input
  ([field]
   (input field {:type "text"}))
  ([field attrs]
   (fn [field attrs]
     [:input (merge attrs
                    {:value     @(form/str-value field)
                     :on-change (form/handle-str-value field true)
                     :on-blur (form/handle-str-value field false)})])))


(defn select
  ([field attrs options]
   (fn [field attrs options]
     (into [:select
            (merge attrs
                   {:value     @(form/str-value field)
                    :on-change (form/handle-str-value field)})]
           options))))

(defn options [& options]
  (->> (partition 2 options)
       (mapv (fn [[value label]]
               ^{:key value}
               [:option {:value value} label]))))