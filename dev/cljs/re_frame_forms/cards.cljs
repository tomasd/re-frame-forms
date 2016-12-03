(ns re-frame-forms.cards
  (:require
    #_[om.core :as om :include-macros true]

    [re-frame-forms.core :as form]
    [re-frame-forms.input :as input]
    [re-frame-forms.coerce :as coerce]
    [re-frame-forms.validation :as validation]
    [reagent.core :as reagent]
    [struct.core :as st]
    [cuerdas.core :as str]
    [re-frame-forms.format :as fmt])
  (:require-macros
    [devcards.core :as dc :refer [defcard deftest defcard-rg]]
    [reagent.ratom :refer [reaction]]
    [cljs.test :refer [testing is]]
    ))
(defn set-locale! [locale]
  (set! goog.i18n.NumberFormatSymbols (aget goog.i18n (str "NumberFormatSymbols_" locale)))
  (set! goog.LOCALE locale))

(set-locale! "sk")



(enable-console-print!)

(deftest format-test
  (testing "int format"
    (is (= "12" (fmt/format-int 12.34567)))
    (is (= "1 234 567" (fmt/format-int 1234567))))

  (testing "number format"
    (is (= "12,346" (fmt/format-decimal 12.34567)))
    (is (= "1 234 567,346" (fmt/format-decimal 1234567.34567))))

  (testing "currency format"
    (is (= "12,35 €" (fmt/format-currency 12.34567)))
    (is (= "1 234 567,35 €" (fmt/format-currency 1234567.34567)))))

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

     [:button {:type     "button"
               :on-click #(form/start-validation! field)}
      "Start validation"]
     [:button {:type     "button"
               :on-click #(form/mark-ok! field)}
      "Mark ok!"]
     [:button {:type     "button"
               :on-click #(form/mark-error! field "ERROR!")}
      "Mark error!"]
     [:span (when @(form/validation-in-progress? field)
              "Validation in progress...")]
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
    (let [form  (form/make-form {:value "value"})
          field (form/make-field form [:value])]
      (fn []
        [:form {:on-submit (form/handle-valid-form form #(prn "Changed form value is" %))}
         [:label
          "Value:"
          [input/input field]]])))
  {})

(defcard-rg field-validation
  (fn []
    (let [form  (form/make-form {:value 1})
          field (form/make-field form [:value] (coerce/int))]
      (fn []
        [:form {:on-submit (form/handle-valid-form form #(prn "Changed form value is" %))}
         [:label
          "Value:"
          [input/input field {:type  "text"
                              :style (when-not @(form/valid? field)
                                       {:border "1px solid red"})}]]])))
  {})

(defcard-rg form-validation
  (fn []
    (let [form  (form/make-form {:value 1} (struct-validator {:value [st/required]}))
          field (form/make-field form [:value])]
      (fn []
        [:form {:on-submit (form/handle-valid-form form #(prn "Changed form value is" %))}
         [:label
          "Value:"
          [input/input field {:type  "text"
                              :style (when-not @(form/valid? field)
                                       {:border "1px solid red"})}]]])))
  {})

(defcard-rg external-field-validation
  (fn []
    (let [form  (form/make-form {:value 1} (struct-validator {:value [st/required]}))
          field (form/make-field form [:value])]
      (fn []
        [:form {:on-submit (form/handle-valid-form form #(prn "Changed form value is" %))}
         [:label
          "Value:"
          [input/input field {:type  "text"
                              :style (when-not @(form/valid? field)
                                       {:border "1px solid red"})}]]])))
  {})

(defcard-rg rg-form
  (fn [state]
    (let [form      (form/make-form
                      @state
                      (struct-validator {:field     [st/required st/string]
                                         :other     [st/required]
                                         :int-field [st/required st/integer]}))
          int-field (form/make-field form [:int-field] (coerce/int))]
      (fn [state]

        [:form {:on-submit (form/handle-valid-form form
                                                   #(reset! state @(form/value form {})))}
         [:div "Valid?:" (if @(form/valid? form) "T" "F")]
         [my-field "text" (form/make-field form [:field])]
         [my-field "text" (form/make-field form [:int-field] (coerce/int))]
         [my-field "text" (form/make-field form [:number-field] (coerce/number))]
         [my-field "text" (form/make-field form [:date-field] (coerce/date "dd.MM.yyyy"))]
         [my-field "text" (form/make-field form [:other])]
         [my-checkbox (form/make-field form [:bool-field] (coerce/bool))]
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
            :on-change (form/handle-str-value int-field)}
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
      (let [form       (form/make-form {:field     "value"
                                        :int-field 1})
            text-field (form/make-field form [:field])
            int-field  (form/make-field form [:int-field] (coerce/int))]
        (is (= @(form/value form) {:field     "value"
                                   :int-field 1}))
        (is (= @(form/value text-field) "value"))
        (is (= @(form/str-value text-field) "value"))
        (is @(form/valid? text-field))

        (is (= @(form/value int-field) 1))
        (is (= @(form/str-value int-field) "1"))
        (is @(form/valid? int-field))))

    (testing "change"
      (let [form       (form/make-form {:field     "value"
                                        :int-field 1})
            text-field (form/make-field form [:field])
            int-field  (form/make-field form [:int-field] (coerce/int))
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
    (let [form      (form/make-form {:value 1} (struct-validator {:value [[st/in-range 1 2]]}))
          int-field (form/make-field form [:value] (coerce/int))]
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

