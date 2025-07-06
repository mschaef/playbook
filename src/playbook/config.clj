;; Copyright (c) 2015-2024 Michael Schaeffer
;;
;; Licensed as below.
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

(ns playbook.config
  (:use playbook.core)
  (:require [taoensso.timbre :as log]
            [cprop.core :as cprop]
            [cprop.source :as cprop-source]
            [playbook.core :as core]))

;;; Configuration Tools

(defn property
  ([name]
   (property name nil))

  ([name default]
   (let [prop-binding (System/getProperty name)]
     (if (nil? prop-binding)
       default
       (if-let [int (core/try-parse-integer prop-binding)]
         int
         prop-binding)))))

(defn- maybe-config-file [prop-name]
  (if-let [prop (property prop-name)]
    (if (.exists (java.io.File. prop))
      (do
        (log/info (str "Config file found: " prop " (specified by property: " prop-name ")"))
        (cprop-source/from-file prop))
      (do
        (log/error (str "CONFIG FILE NOT FOUND: " prop " (specified by property: " prop-name ")"))
        {}))
    {}))

(defn load-config []
  (cprop/load-config :merge [(cprop-source/from-resource "config.edn")
                             (maybe-config-file "conf")
                             (maybe-config-file "creds")]))

(def ^:dynamic *config* nil)

(defmacro with-config [new-config & body]
  `(binding [*config* ~new-config]
     ~@body))

(defn cval [& keys]
  (when (not *config*)
    (throw (RuntimeException. "No configuration loaded.")))
  (get-in *config* keys))

(defn wrap-config [app]
  (let [config *config*]
    (fn [req]
      (with-config config
        (app req)))))

(defmacro with-extended-config [additional-config & body]
  `(with-config (deep-merge (cval) ~additional-config)
     ~@body))

(defn wrap-with-current-config [f]
  (let [config (cval)]
    #(with-config config
       (f))))
