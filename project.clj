;; Copyright (c) 2015-2025 Michael Schaeffer
;;
;; Licensed as below.
;;
;; Portions Copyright (c) 2014 KSM Technology Partners
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;       http://www.apache.org/licenses/LICENSE-2.0
;;
;; The license is also includes at the root of the project in the file
;; LICENSE.
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.
;;
;; You must not remove this notice, or any other, from this software.

(defproject com.mschaef/playbook "0.1.8-SNAPSHOT"
  :description "A standard set of tools for small-scale Clojure applications."

  :license {:name "The Apache Software License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}

  :dependencies [[org.clojure/clojure "1.12.0"]
                 [org.clojure/tools.logging "1.3.0"]
                 [com.taoensso/timbre "6.6.1"]
                 [com.fzakaria/slf4j-timbre "0.4.1"]
                 [cprop "0.1.20"]
                 [org.clojure/data.json "2.5.0"]
                 [jstrutz/hashids "1.0.1"]
                 [compojure "1.7.1"
                  :exclusions [commons-codec]]
                 [it.sauronsoftware.cron4j/cron4j "2.2.5"]]

  :cljfmt {:load-config-file? true}

  :plugins [[dev.weavejester/lein-cljfmt "0.13.0"]]

  :scm {:name "git"
        :url "https://github.com/mschaef/playbook.git"}

  :deploy-repositories [["releases" {:url "https://repo.clojars.org"}]])
