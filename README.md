[![Clojars Project](https://img.shields.io/clojars/v/re-frame-forms.svg)](https://clojars.org/re-frame-forms)

# re-frame-forms

Reagent library for form processing

## Motivation

Input processing is hard in react. I needed a solution for html inputs
with these characteristics:

* 1 Central value for the form
* Each field can edit specific part of the form value
* Field has built in conversion with validation between string (html input) and object (form value)
* Field validation
* Form value validation
* Cross form fields validation
* Local state (form value should not be stored in MVC)
* Remote validations (e.g. unique email)

## Setup

Add dependency to your project:

[![Clojars Project](http://clojars.org/re-frame-forms/latest-version.svg)](http://clojars.org/re-frame-forms)

Then require the library:
```clojure
(ns some.ns
  (:require [re-frame-forms.core :as form]
            [re-frame-forms.input :as input]))
```

re-frame-forms provides 2 namespaces. 

* form - core library, provides Form and Field abstraction
* input - helper namespace for creating html inputs

## Usage

### Without validation

Create form with 1 field:

```clojure
(defn my-form []
  (let [form (form/create-form {:value "value"})
        field (form/field form [:value])]
    (fn []
      [:form {:on-submit (form/handle-valid-form form #(prn "Changed form value is" %))}
       [:label
        "Value:"
        [input/input field]]])))
```

### Field validation

Field value is validated as integer:

```clojure
(defn my-form []
  (let [form  (form/create-form {:value 1})
        field (form/field form [:value] :int)]
    (fn []
      [:form {:on-submit (form/handle-valid-form form #(prn "Changed form value is" %))}
       [:label
        "Value:"
        [input/input field {:type  "text"
                            :style (when-not @(form/valid? field)
                                     {:border "1px solid red"})}]]])))
````

### Form validation
Here funcool/struct is used as validation library.

To use your favorite validation library just wrap it into `Validator` protocol and return `PathErrors` protocol from the `validate` method

```clojure
(defn struct-validator [schema]
  (form/validator #(form/validation-result
                    (-> (st/validate % schema)
                        first))))
                        
(defn my-form []
  (let [form  (form/create-form {:value 1} (struct-validator {:value [st/required]}))
        field (form/field form [:value])]
    (fn []
      [:form {:on-submit (form/handle-valid-form form #(prn "Changed form value is" %))}
       [:label
        "Value:"
        [input/input field {:type  "text"
                            :style (when-not @(form/valid? field)
                                     {:border "1px solid red"})}]]])))
                      
```
## Coercion
## Validation
