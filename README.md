[![Clojars Project](https://img.shields.io/clojars/v/re-frame-forms.svg)](https://clojars.org/re-frame-forms)

# re-frame-forms

Reagent library for form processing

## Motivation

Input processing in React is hard. I needed a solution for html inputs
with these characteristics:

* 1 Central value for the form
* Each field can edit specific part of the form value
* Field has built in conversion with validation between string (html input) and object (form value)
* Field validation
* Form value validation
* Cross form fields validation
* Local state (form value should not be stored in MVC)
* Remote validations (e.g. unique email)

## Design concepts

There 2 core components:
* Form
* Field

### Form

Form is kind of atom with associated validator wrapping form value.

Operations:
* value, set-value!
* original-value, reset-value!
* valid?
* touch!
* touched?

Can by created by calling `make-form`

### Field
Field is kind of cursor to the form. It contains path within form value, coercer and validator. 

Operations:
* value, set-value!
* set-error!, cler-error!
* original-value, reset-value!
* str-value, set-str-value!
* errors
* valid?
* touch!
* touched?

Can be created by calling `make-field`

In html user input is processed by input fields like `input`, `select`.
Value is displayed as string, so there should be a function converting value
to string and from string. This process is called coercion.

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
  (let [form (form/make-form {:value "value"})
        field (form/make-field form [:value])]
    (fn []
      [:form 
       [:label
        "Value:"
        [input/input field]]])))
```

### Field validation

Field value is validated as integer:

```clojure
(defn my-form []
  (let [form  (form/make-form {:value 1})
        field (form/make-field form [:value] :int)]
    (fn []
      [:form 
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
  (validation/form-validator #(validation/validation-result
                               (-> (st/validate % schema)
                                   first))))
                        
(defn my-form []
  (let [form  (form/make-form {:value 1} (struct-validator {:value [st/required]}))
        field (form/make-field form [:value])]
    (fn []
      [:form 
       [:label
        "Value:"
        [input/input field {:type  "text"
                            :style (when-not @(form/valid? field)
                                     {:border "1px solid red"})}]]])))
                      
```

### External validation

## Coercion

Coercion is the process of converting value from/to string. There are several built-in coercers:

* IntCoercer or `:int`
* NumberCoercer or `:number`
* BoolCoercer or `:bool`
* KeywordCoercer or `:keyword`

You can implement coercer by implementing `coerce/Coercer` protocol and passing the instance to the `make-field` field constructor.

You can also use keyword instead of `coerce/Coercer` instance. But you must register your coercer in `make-coercer` constructor multimethod.

By default failed coercion just marks the field as invalid. You can add validation messages by implementing `validation/FieldValidator`, even to existing ones by `extend-type` functionality. It's not supported by default because of localization issues.

## Validation

Both field and form can be validated. Just pass instance of corresponding validator into the constructor:

* `validator/FieldValidator`
* `validator/FormValidator`

### FieldValidator

Field validator return list of validation messages

### FormValidator

Form validator returns instance of `validation/ValidationResult` containing error messages for each field. You can use structural map containing errors with wrapper `validation/validation-result`. This is suitable for validation libraries like funcool/struct or bouncer.

## License

Copyright © 2016 [Tomas Drencak](http://tomasd.github.io)

Distributed under the [MIT License](http://opensource.org/licenses/MIT).