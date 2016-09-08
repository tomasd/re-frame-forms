(ns reframe-forms.core
  (:require
    #_[om.core :as om :include-macros true]
    [sablono.core :as sab :include-macros true]
    [reagent.core :as reagent]
    [struct.core :as st]
    [cuerdas.core :as str])
  (:require-macros
    [devcards.core :as dc :refer [defcard deftest defcard-rg]]
    [reagent.ratom :refer [reaction]]
    [cljs.test :refer [testing is]]
    ))

(enable-console-print!)


(defprotocol Value
  (value [this]
         [this default])
  (set-value! [this val]))

(defprotocol ResetValue
  (original-value [this])
  (reset-value! [this]))

(defprotocol Coercer
  (to-str [this obj-value])
  (from-str [this str-value]))

(defprotocol PathValue
  (path-value [this type path]
              [this type path default]))

(defprotocol PathErrors
  (path-errors [this path]))

(defprotocol CoercedValue
  (str-value [this])
  (set-str-value! [this val]))

(defprotocol ErrorContainer
  (errors [this]))

(defprotocol Validatable
  (valid? [this]))

(defprotocol Validator
  (validate [this value]))


;Validator by mohol vracat nejaky Errors kde bude povedane ci je ok a path

(defprotocol Touchable
  (touch [this])
  (touched? [this]))

(defn- field-path [type path]
  (if (#{::value ::original ::validator-errors} type)
    (cons type path)
    [type path]))

(defn- assoc-field [form path & kvs]
  (reduce (fn [form [type value]]
            (assoc-in form (field-path type path) value))
          form
          (partition 2 kvs)))

(defrecord Field [form coercer validator path]
  Value
  (value [this]
    (value this nil))
  (value [_ default]
    (path-value form ::value path default))
  (set-value! [_ val]
    (swap! form assoc-field path
           ::field-errors (validate validator val)
           ::value val
           ::tmp nil
           ::touched true))

  ResetValue
  (original-value [this]
    @(path-value form ::original path nil))
  (reset-value! [this]
    (swap! form assoc-field path
           ::field-errors []
           ::value (original-value this)
           ::tmp nil
           ::touched false))

  CoercedValue
  (str-value [this]
    (reaction (or @(path-value form ::tmp path nil)
                  (->> @(value this)
                       (to-str coercer)))))
  (set-str-value! [this val]
    (let [errors (validate coercer val)]
      (if (empty? errors)
        (->> (from-str coercer val)
             (set-value! this))
        (swap! form assoc-field path
               ::field-errors errors
               ::value nil
               ::tmp val
               ::touched true))))

  ErrorContainer
  (errors [this]
    (reaction (->> (concat @(path-value form ::field-errors path nil)
                           @(path-errors form path))
                   (remove empty?))))
  Validatable
  (valid? [this]
    (reaction (empty? @(errors this))))

  Touchable
  (touch [this]
    (swap! form assoc-field path
           ::touched true))
  (touched? [_]
    (reaction @(path-value form ::touched path false))))

(defmulti coercer identity)
(defmethod coercer :default
  [_] (reify
        Coercer
        (to-str [_ obj] (str obj))
        (from-str [_ s] s)

        Validator
        (validate [_ s])))

(defmethod coercer :int
  [_] (reify
        Coercer
        (to-str [_ obj] (str obj))
        (from-str [_ s] (if (str/empty? s) nil (js/parseInt s)))

        Validator
        (validate [_ s]
          (if (or (str/empty? s) (re-matches #"(\+|\-)?\d+" s))
            []
            ["Neplatné číslo"]
            ))))


(defn- validate-form
  ([validator f]
   (fn [value & args]
     (validate-form value validator f args)))
  ([value validator f & args]
   (let [new-value (apply f value args)]
     (-> new-value
         (assoc ::validator-errors (validate validator (::value new-value)))))))

(extend-type nil
  Validator
  (validate [this value]
    nil)

  Validatable
  (valid? [this]
    true)

  PathErrors
  (path-errors [this path]
    []))

(defrecord Form [value validator]
  ISwap
  (-swap! [o f]
    (swap! value validate-form validator f))
  (-swap! [o f a]
    (swap! value validate-form validator f a))
  (-swap! [o f a b]
    (swap! value validate-form validator f a b))
  (-swap! [o f a b xs]
    (apply swap! value validate-form validator f a b xs))

  Value
  (value [this]
    (reaction (get @value ::value)))
  (value [this default]
    (reaction (get @value ::value default)))
  (set-value! [this val]
    (swap! value
           validate-form assoc ::value val))

  ResetValue
  (original-value [this]
    (get @value ::original))
  (reset-value! [this]
    (swap! value (fn [value]
                   {::value    (::original value)
                    ::original (::original value)})))

  PathValue
  (path-value [_ type path]
    (path-value _ type path nil))
  (path-value [_ type path default]
    (reaction (get-in @value (field-path type path) default)))

  PathErrors
  (path-errors [_ path]
    (reaction (path-errors (::validator-errors @value) path)))

  Validatable
  (valid? [_]
    (reaction (and
                (->> (::field-errors @value)
                     vals
                     (remove empty?)
                     empty?)
                (valid? (::validator-errors @value))))))

(defn field [form type path]
  (->Field form (coercer type) (reify Validator
                                 (validate [_ value] [])) path))



(deftype ValidationResult [errors]
  PathErrors
  (path-errors [this path]
    (remove nil? [(get-in errors path)]))

  Validatable
  (valid? [this]
    (empty? errors)))

(deftype StructValidator [schema]
  Validator
  (validate [this value]
    (->ValidationResult (-> (st/validate value schema)
                            first))))
(defn struct-validator [schema]
  (->StructValidator schema))

(defn create-form
  ([value]
   (create-form value nil))
  ([value validator]
   (->Form (reagent/atom {::value    value
                          ::original value
                          })
           validator)))

(defn handle-str-value [field]
  #(set-str-value! field (-> % .-target .-value)))

(defcard first-card
         (sab/html [:div
                    [:h1 "This is your first devcard!"]]))


(defn my-field [type field]
  (fn [type field]
    [:div
     [:input {:type      type
              :value     @(str-value field)
              :on-change (handle-str-value field)}]
     [:button {:type     "button"
               :on-click #(reset-value! field)} "Reset"]
     [:span (original-value field)]
     (when @(touched? field) "touched")
     (for [[i message] (map-indexed vector @(errors field))]
       ^{:key i} [:li message])]))

(deftest test-form
  (testing "form without validator"
    (testing "display"
      (let [form       (create-form {:field     "value"
                                     :int-field 1})
            text-field (field form :text [:field])
            int-field  (field form :int [:int-field])]
        (is (= @(value form nil) {:field     "value"
                                  :int-field 1}))
        (is (= @(value text-field) "value"))
        (is (= @(str-value text-field) "value"))
        (is @(valid? text-field))

        (is (= @(value int-field) 1))
        (is (= @(str-value int-field) "1"))
        (is @(valid? int-field))))


    (testing "change"
      (let [form  (create-form {:field "value"
                                :int-field 1})
            text-field (field form :text [:field])
            int-field (field form :int [:int-field])
            ]
        (set-str-value! text-field "changed")
        (is (= @(value text-field) "changed"))
        (is @(valid? text-field))

        (set-str-value! int-field "invalid")
        (is (= @(value int-field) nil))
        (is (= @(str-value int-field ) "invalid"))
        (is (not @(valid? int-field)))
        )))

  (testing "form with validator, int in range <1;2>"
    (let [form (create-form {:value 1} (struct-validator {:value [[st/in-range 1 2]]}))
          int-field (field form :int [:value])]
      (is @(valid? int-field) "1 is in range <1;2")


      (set-str-value! int-field "2")
      (is @(valid? int-field) "2 is in range <1;2>")


      (set-str-value! int-field "3")
      (is (not @(valid? int-field)) "3 is not in range <1;2>"))))

(defcard-rg rg-form
            (fn [state]
              (let [form      (create-form
                                @state
                                (struct-validator {:field     [st/required st/string]
                                                   :int-field [st/required st/integer]}))
                    int-field (field form :int [:int-field])]
                (fn [state]
                  [:form {:on-submit #(do (reset! state @(value form {}))
                                          (.preventDefault %))}
                   [:div "Valid?:" (if @(valid? form) "T" "F")]
                   [my-field "text" (field form :text [:field])]
                   [my-field "text" (field form :int [:int-field])]
                   [:select
                    {:value     @(str-value int-field)
                     :on-change (handle-str-value int-field)}
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
  (if-let [node (.getElementById js/document "main-app-area")]
    (js/React.render (sab/html [:div "This is working"]) node)))

(main)

;; remember to run lein figwheel and then browse to
;; http://localhost:3449/cards.html

