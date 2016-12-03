(defproject re-frame-forms "0.1.4-SNAPSHOT"
  :description "Reagent library for form processing"
  :url "https://github.com/tomasd/re-frame-forms"
  :license {:name "MIT"}

  :min-lein-version "2.5.3"

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.229"]

                 [cljsjs/react "15.2.1-0"]
                 [reagent "0.6.0"]]

  :profiles {:dev {:dependencies   [[devcards "0.2.1-7"]
                                    [funcool/struct "1.0.0"]]

                   :source-paths   ["src"
                                    "dev/cljs"]
                   :resource-paths ["dev/resources"]
                   :figwheel       {:css-dirs ["dev/resources/public/css"]}

                   :plugins        [[lein-figwheel "0.5.3-2"]
                                    [lein-cljsbuild "1.1.3" :exclusions [org.clojure/clojure]]]

                   :cljsbuild      {:builds [{:id           "devcards"
                                              :source-paths ["src" "dev/cljs"]
                                              :figwheel     {:devcards true} ;; <- note this
                                              :compiler     {:main                 "re-frame-forms.cards"
                                                             :asset-path           "js/compiled/devcards_out"
                                                             :output-to            "dev/resources/public/js/compiled/re_frame_forms_devcards.js"
                                                             :output-dir           "dev/resources/public/js/compiled/devcards_out"
                                                             :source-map-timestamp true}}
                                             {:id           "dev"
                                              :source-paths ["src" "dev/cljs"]
                                              :figwheel     true
                                              :compiler     {:main                 "re-frame-forms.core"
                                                             :asset-path           "js/compiled/out"
                                                             :output-to            "dev/resources/public/js/compiled/re_frame_forms.js"
                                                             :output-dir           "dev/resources/public/js/compiled/out"
                                                             :source-map-timestamp true}}
                                             {:id           "prod"
                                              :source-paths ["src" "dev/cljs"]
                                              :compiler     {:main          "re-frame-forms.core"
                                                             :asset-path    "js/compiled/out"
                                                             :output-to     "dev/resources/public/js/compiled/re_frame_forms.js"
                                                             :optimizations :advanced}}]}}}


  :clean-targets ^{:protect false} ["resources/public/js/compiled"
                                    "dev/resources/public/js/compiled"
                                    "target"]

  :source-paths ["src"]
  :deploy-repositories [["releases" :clojars]
                        ["snapshots" :clojars]]
  :lein-release {:scm        :git
                 :deploy-via :clojars}

  )
