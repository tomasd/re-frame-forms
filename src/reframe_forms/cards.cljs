(ns reframe-forms.cards
  (:require
    #_[om.core :as om :include-macros true]

    [reframe-forms.core :as form]
    [reagent.core :as reagent]
    [struct.core :as st]
    [cuerdas.core :as str])
  (:require-macros
    [devcards.core :as dc :refer [defcard deftest defcard-rg]]
    [reagent.ratom :refer [reaction]]
    [cljs.test :refer [testing is]]
    ))

(enable-console-print!)

(deftype StructValidator [schema]
  form/Validator
  (validate [this value]
    (form/validation-result
      (-> (st/validate value schema)
          first))))
(defn struct-validator [schema]
  (->StructValidator schema))

(deftest test-form
  (testing "form without validator"
    (testing "display"
      (let [form       (form/create-form {:field     "value"
                                          :int-field 1})
            text-field (form/field form :text [:field])
            int-field  (form/field form :int [:int-field])]
        (is (= @(form/value form nil) {:field     "value"
                                       :int-field 1}))
        (is (= @(form/value text-field) "value"))
        (is (= @(form/str-value text-field) "value"))
        (is @(form/valid? text-field))

        (is (= @(form/value int-field) 1))
        (is (= @(form/str-value int-field) "1"))
        (is @(form/valid? int-field))))


    (testing "change"
      (let [form       (form/create-form {:field     "value"
                                          :int-field 1})
            text-field (form/field form :text [:field])
            int-field  (form/field form :int [:int-field])
            ]
        (form/set-str-value! text-field "changed")
        (is (= @(form/value text-field) "changed"))
        (is @(form/valid? text-field))

        (form/set-str-value! int-field "invalid")
        (is (= @(form/value int-field) nil))
        (is (= @(form/str-value int-field) "invalid"))
        (is (not @(form/valid? int-field)))
        )))

  (testing "form with validator, int in range <1;2>"
    (let [form      (form/create-form {:value 1} (struct-validator {:value [[st/in-range 1 2]]}))
          int-field (form/field form :int [:value])]
      (is @(form/valid? int-field) "1 is in range <1;2")


      (form/set-str-value! int-field "2")
      (is @(form/valid? int-field) "2 is in range <1;2>")


      (form/set-str-value! int-field "3")
      (is (not @(form/valid? int-field)) "3 is not in range <1;2>"))))

(defn my-field [type field]
  (fn [type field]
    [:div
     [:input {:type      type
              :value     @(form/str-value field)
              :on-change (form/handle-str-value field)}]
     [:button {:type     "button"
               :on-click #(form/reset-value! field)} "Reset"]
     [:span (form/original-value field)]
     (when @(form/touched? field) "touched")
     (for [[i message] (map-indexed vector @(form/errors field))]
       ^{:key i} [:li message])]))


(defcard-rg rg-form
  (fn [state]
    (let [form      (form/create-form
                      @state
                      (struct-validator {:field     [st/required st/string]
                                              :int-field [st/required st/integer]}))
          int-field (form/field form :int [:int-field])]
      (fn [state]
        [:form {:on-submit #(do (reset! state @(form/value form {}))
                                (.preventDefault %))}
         [:div "Valid?:" (if @(form/valid? form) "T" "F")]
         [my-field "text" (form/field form :text [:field])]
         [my-field "text" (form/field form :int [:int-field])]
         [:select
          {:value     @(form/str-value int-field)
           :on-change (form/handle-str-value int-field)}
          [:option {:value 1} "1"]
          [:option {:value 2} "2"]]
         [:input {:type "submit"}]
         ])))
  {:field     "value"
   :int-field 1}

  {:inspect-data true}
  )

(defn main []
  ;; conditionally start the app based on whether the #main-app-area
  ;; node is on the page
  #_(if-let [node (.getElementById js/document "main-app-area")]
      (js/React.render (sab/html [:div "This is working"]) node)))

(main)

;; remember to run lein figwheel and then browse to
;; http://localhost:3449/cards.html

