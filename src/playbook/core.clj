;; Copyright (c) 2015-2023 Michael Schaeffer
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

(ns playbook.core
  (:require [taoensso.timbre :as log]
            [clojure.data.json :as json]))

(defmacro unless [ condition & body ]
  `(when (not ~condition)
     ~@body))

(defn string-empty? [ str ]
  (or (nil? str)
      (= 0 (count (.trim str)))))

(defn in?
  "true if seq contains elm"
  [seq elm]
  (some #(= elm %) seq))

(defn assoc-if [ map assoc? k v ]
  (if assoc?
    (assoc map k v)
    map))

(defn string-leftmost
  ( [ string count ellipsis ]
      (let [length (.length string)
            leftmost (min count length)]
        (if (< leftmost length)
          (str (.substring string 0 leftmost) ellipsis)
          string)))
  ( [ string count ]
      (string-leftmost string count "")))

(defn try-parse-integer
  ([ str default-value ]
   (try
     (Integer/parseInt str)
     (catch Exception ex
       default-value)))
  ([ str ]
    (try-parse-integer str false)))

(defn try-parse-long
  ([ str default-value ]
   (try
     (Long/parseLong str)
     (catch Exception ex
       default-value)))
  ([ str ]
    (try-parse-long str false)))

(defn try-parse-double
  ([ str default-value ]
   (try
     (Double/parseDouble str)
     (catch Exception ex
       default-value)))
  ([ str ]
   (try-parse-double str false)))

(defn safe-json-read-str [ json-string ]
  (try
    (json/read-str json-string)
    (catch Exception ex
      (log/warn "Bad JSON:" (.getMessage ex) json-string)
      false)))

(defn try-parse-percentage [ str ]
  (and (string? str)
       (let [ str (if (= \% (.charAt str (- (.length str) 1)))
                    (.substring str 0 (- (.length str) 1))
                    str)]
         (try-parse-double str))))

(defn ensure-number [ val ]
  (if (number? val)
    val
    (try-parse-double val)))

(defn config-property
  ( [ name ] (config-property name nil))
  ( [ name default ]
      (let [prop-binding (System/getProperty name)]
        (if (nil? prop-binding)
          default
          (if-let [ int (try-parse-integer prop-binding) ]
            int
            prop-binding)))))

(defn add-shutdown-hook [ shutdown-fn ]
  (.addShutdownHook (Runtime/getRuntime)
                    (Thread. (fn []
                               (shutdown-fn)))))

(defn exception-barrier
  ([ fn label ]
   #(try
      (fn)
      (catch Exception ex
        (log/error ex (str "Uncaught exception: " label))))))

;;; Date utilities

(defn current-time []
  (java.util.Date.))

(defn add-days [ date days ]
  "Given a date, advance it forward n days, leaving it at the
  beginning of that day"
  (let [c (java.util.Calendar/getInstance)]
    (.setTime c date)
    (.add c java.util.Calendar/DATE days)
    (.set c java.util.Calendar/HOUR_OF_DAY 0)
    (.set c java.util.Calendar/MINUTE 0)
    (.set c java.util.Calendar/SECOND 0)
    (.set c java.util.Calendar/MILLISECOND 0)
    (.getTime c)))
