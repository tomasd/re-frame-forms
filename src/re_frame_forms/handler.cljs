(ns re-frame-forms.handler
  (:require
    [re-frame-forms.core :as form]))

(defn handle-str-value [field]
  #(form/set-str-value! field (-> % .-target .-value)))

(defn handle-checked-value [field]
  #(form/set-value! field (-> % .-target .-checked)))

(defn handle-valid-form [form callback]
  (fn [e]
    (form/touch! form)
    (when @(form/valid? form)
      (callback @(form/value form)))
    (.preventDefault e)))

