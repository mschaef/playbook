;; Copyright (c) 2015-2025 Michael Schaeffer
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

(ns playbook.main
  (:require [taoensso.timbre :as log]
            [playbook.core :as core]
            [playbook.config :as config]
            [playbook.logging :as logging]))

(defn app-entry
  ([entry-fn]
   (println "Loading Configuration...")
   (config/with-config (config/load-config)
     (logging/setup-logging)
     (log/report "Starting App" (config/cval :app))
     (when (config/cval :development-mode)
       (log/warn "=== DEVELOPMENT MODE ==="))
     (core/with-exception-barrier :app-entry
       (entry-fn))
     (log/info "end run."))))

(defmacro defmain [arglist & body]
  `(defn ~'-main ~arglist
     (app-entry (fn [] ~@body))))
