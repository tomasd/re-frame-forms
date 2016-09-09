(ns re-frame-forms.cards
  (:require
    #_[om.core :as om :include-macros true]

    [re-frame-forms.core :as form]
    [re-frame-forms.input :as input]
    [re-frame-forms.coerce :as coerce]
    [re-frame-forms.handler :as handler]
    [re-frame-forms.validation :as validation]
    [reagent.core :as reagent]
    [struct.core :as st]
    [cuerdas.core :as str])
  (:require-macros
    [devcards.core :as dc :refer [defcard deftest defcard-rg]]
    [reagent.ratom :refer [reaction]]
    [cljs.test :refer [testing is]]
    ))

(enable-console-print!)

(defn struct-validator [schema]
  (validation/form-validator #(validation/validation-result
                    (-> (st/validate % schema)
                        first))))

(defn my-field [type field]
  (fn [type field]
    [:div
     [input/input field {:type  type
                         :style (if-not @(form/valid? field) {:border "1px solid red"})}]
     [:button {:type     "button"
               :on-click #(form/reset-value! field)} "Reset"]
     [:span (form/original-value field)]
     (when @(form/touched? field) "touched")
     (if @(form/valid? field) "valid" "invalid")
     (for [[i message] (map-indexed vector @(form/errors field))]
       ^{:key i} [:li message])]))

(defn my-checkbox [field]
  [:div
   [input/checkbox field]])

(defn my-radio [field value label]
  [:label [input/radio field value] label])

(defcard-rg no-validation
  (defn my-form []
    (let [form  (form/create-form {:value "value"})
          field (form/field form [:value])]
      (fn []
        [:form {:on-submit (handler/handle-valid-form form #(prn "Changed form value is" %))}
         [:label
          "Value:"
          [input/input field]]])))
  {})

(defcard-rg field-validation
  (fn []
    (let [form  (form/create-form {:value 1})
          field (form/field form [:value] :int)]
      (fn []
        [:form {:on-submit (handler/handle-valid-form form #(prn "Changed form value is" %))}
         [:label
          "Value:"
          [input/input field {:type  "text"
                              :style (when-not @(form/valid? field)
                                       {:border "1px solid red"})}]]])))
  {})

(defcard-rg form-validation
  (fn []
    (let [form  (form/create-form {:value 1} (struct-validator {:value [st/required]}))
          field (form/field form [:value])]
      (fn []
        [:form {:on-submit (handler/handle-valid-form form #(prn "Changed form value is" %))}
         [:label
          "Value:"
          [input/input field {:type  "text"
                              :style (when-not @(form/valid? field)
                                       {:border "1px solid red"})}]]])))
  {})

(defcard-rg rg-form
  (fn [state]
    (let [form      (form/create-form
                      @state
                      (struct-validator {:field     [st/required st/string]
                                         :int-field [st/required st/integer]}))
          int-field (form/field form [:int-field] :int)]
      (fn [state]

        [:form {:on-submit (handler/handle-valid-form form
                                                   #(reset! state @(form/value form {})))}
         [:div "Valid?:" (if @(form/valid? form) "T" "F")]
         [my-field "text" (form/field form [:field] :text)]
         [my-field "text" (form/field form [:int-field] :int)]
         [my-field "text" (form/field form [:number-field] :number)]
         [my-checkbox (form/field form [:bool-field] :bool)]
         [:div
          [input/select int-field {}
           (input/options 1 "1"
                          2 "2")
           ]
          [input/select int-field {}
           [[:option {:value 1} "1"]
            [:option {:value 2} "2"]]]
          [:select
           {:value     @(form/str-value int-field)
            :on-change (handler/handle-str-value int-field)}
           [:option {:value 1} "1"]
           [:option {:value 2} "2"]]]
         [:div
          [my-radio int-field 1 "1"]
          [my-radio int-field 2 "2"]
          [my-radio int-field 3 "3"]]

         [:input {:type "submit"}]
         [:input {:type "button" :on-click #(form/reset-value! form) :value "Reset"}]
         [:input {:type     "button"
                  :on-click #(form/set-error! int-field "Error")
                  :value    "Set error"}]
         [:input {:type     "button"
                  :on-click #(form/clear-error! int-field)
                  :value    "Clear error"}]
         ])))
  {:field        "value"
   :int-field    1
   :bool-field   true
   :number-field 5.4}

  {:inspect-data true}
  )

(deftest test-form
  (testing "form without validator"
    (testing "display"
      (let [form       (form/create-form {:field     "value"
                                          :int-field 1})
            text-field (form/field form [:field] :text)
            int-field  (form/field form [:int-field] :int)]
        (is (= @(form/value form nil) {:field     "value"
                                       :int-field 1}))
        (is (= @(form/value text-field) "value"))
        (is (= @(form/str-value text-field) "value"))
        (is @(form/valid? text-field))

        (is (= @(form/value int-field) 1))
        (is (= @(form/str-value int-field) "1"))
        (is @(form/valid? int-field))))

    (testing "int coercer"
      (is (coerce/valid-str? :int "1"))
      (is (not (coerce/valid-str? :int "invalid"))))


    (testing "change"
      (let [form       (form/create-form {:field     "value"
                                          :int-field 1})
            text-field (form/field form [:field] :text)
            int-field  (form/field form [:int-field] :int)
            ]
        (testing "set changed as str value"
          (form/set-str-value! text-field "changed")
          (is (= @(form/value text-field) "changed"))
          (is @(form/valid? text-field)))

        (testing "set invalid as int"
          (form/set-str-value! int-field "invalid")
          (is (= @(form/value int-field) nil) "invalid is nil int")
          (is (= @(form/str-value int-field) "invalid") "invalid as nil int, invalid str")
          (is (not @(form/valid? int-field)) "invalid, is not valid int")))))

  (testing "form with validator, int in range <1;2>"
    (let [form      (form/create-form {:value 1} (struct-validator {:value [[st/in-range 1 2]]}))
          int-field (form/field form [:value] :int)]
      (is @(form/valid? int-field) "1 is in range <1;2")


      (form/set-str-value! int-field "2")
      (is @(form/valid? int-field) "2 is in range <1;2>")


      (form/set-str-value! int-field "3")
      (is (not @(form/valid? int-field)) "3 is not in range <1;2>"))))

(defn main []
  ;; conditionally start the app based on whether the #main-app-area
  ;; node is on the page
  #_(if-let [node (.getElementById js/document "main-app-area")]
      (js/React.render (sab/html [:div "This is working"]) node)))

(main)

;; remember to run lein figwheel and then browse to
;; http://localhost:3449/cards.html

