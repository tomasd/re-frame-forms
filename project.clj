(defproject reframe-forms "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :min-lein-version "2.5.3"

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.229"]

                 [cljsjs/react "15.2.1-0"]
                 [reagent "0.6.0-rc"]]

  :profiles {:dev {:dependencies [[devcards "0.2.1-7"]
                                  [funcool/struct "1.0.0"]]}}

  :plugins [[lein-figwheel "0.5.3-2"]
            [lein-cljsbuild "1.1.3" :exclusions [org.clojure/clojure]]]

  :clean-targets ^{:protect false} ["resources/public/js/compiled"
                                    "target"]

  :source-paths ["src"]

  :cljsbuild {
              :builds [{:id           "devcards"
                        :source-paths ["src"]
                        :figwheel     {:devcards true}      ;; <- note this
                        :compiler     {:main                 "reframe-forms.cards"
                                       :asset-path           "js/compiled/devcards_out"
                                       :output-to            "resources/public/js/compiled/reframe_forms_devcards.js"
                                       :output-dir           "resources/public/js/compiled/devcards_out"
                                       :source-map-timestamp true}}
                       {:id           "dev"
                        :source-paths ["src"]
                        :figwheel     true
                        :compiler     {:main                 "reframe-forms.core"
                                       :asset-path           "js/compiled/out"
                                       :output-to            "resources/public/js/compiled/reframe_forms.js"
                                       :output-dir           "resources/public/js/compiled/out"
                                       :source-map-timestamp true}}
                       {:id           "prod"
                        :source-paths ["src"]
                        :compiler     {:main          "reframe-forms.core"
                                       :asset-path    "js/compiled/out"
                                       :output-to     "resources/public/js/compiled/reframe_forms.js"
                                       :optimizations :advanced}}]}

  :figwheel {:css-dirs ["resources/public/css"]})
