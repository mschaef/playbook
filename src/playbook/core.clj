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
  (:require [clojure.data.json :as json]
            [clojure.edn :as edn]
            [clojure.pprint :as pprint]
            [taoensso.timbre :as log]
            [playbook.config :as config]
            [playbook.logging :as logging]))

;;; Control Flow

(defmacro unless [ condition & body ]
  `(when (not ~condition)
     ~@body))

(defmacro aand [ & forms ]
  (case (count forms)
    0 true
    1 (first forms)
    `(if-let [ ~'it ~(first forms ) ]
       (aand ~@(rest forms))
       false)))

;;; Data structure tools

(defn in?
  "true if seq contains elm"
  [seq elm]
  (some #(= elm %) seq))

(defn assoc-if [ map assoc? k v ]
  (if assoc?
    (assoc map k v)

    map))

(defn map-values [f m]
  (->> (map (fn [[k v]] [k (f v)]) m)
       (into {})))

(defn drop-nth [n coll]
  (keep-indexed #(if (not= %1 n) %2) coll))

(defn vmap [f coll]
  (into {} (for [[k v] coll] [k (f v)])))

(defn to-map [ key-fn values ]
  (into {} (map (fn [ value ]
                  [(key-fn value) value])
                values )))

(defn to-map-with-keys [ keys-fn values ]
  (into {} (mapcat (fn [ value ]
                     (map (fn [ key ]
                            [key value])
                          (keys-fn value)))
                   values)))

(defn deep-merge [a & maps]
  (if (map? a)
    (apply merge-with deep-merge a maps)
    (apply merge-with deep-merge maps)))

;;; String Tools

(defn string-empty? [ str ]
  (or (nil? str)
      (= 0 (count (.trim str)))))

(defn partition-string [ string n ]
  "Partition a full string into segments of length n, returning a
  sequence of strings of at most that length."
  (map (partial apply str) (partition-all n string)))

(defn string-leftmost
  ( [ string count ellipsis ]
      (let [length (.length string)
            leftmost (min count length)]
        (if (< leftmost length)
          (str (.substring string 0 leftmost) ellipsis)
          string)))
  ( [ string count ]
      (string-leftmost string count "")))

;;; String Parsing

(defn parsable-string? [ maybe-string ]
  "Returns the parsable text content of the string and false
  if there is no such content."
  (and
   (string? maybe-string)
   (let [ string (.trim maybe-string) ]
     (and (> (count string) 0)
          string))))

(defn try-parse-integer
  ([ str default-value ]
   (aand (parsable-string? str)
         (try
           (Integer/parseInt it)
           (catch Exception ex
             default-value))))
  ([ str ]
    (try-parse-integer str false)))

(defn try-parse-long
  ([ str default-value ]
   (aand (parsable-string? str)
         (try
           (Long/parseLong it)
           (catch Exception ex
             default-value))))
  ([ str ]
    (try-parse-long str false)))

(defn try-parse-double
  ([ str default-value ]
   (aand (parsable-string? str)
         (try
           (Double/parseDouble it)
           (catch Exception ex
             default-value))))
  ([ str ]
   (try-parse-double str false)))

(defn try-parse-json
  ([ json-string default-value ]
   (try
     (json/read-str json-string :key-fn keyword)
     (catch Exception ex
       (log/warn "Bad JSON:" (.getMessage ex) json-string)
       default-value)))

  ([ json-string ]
   (try-parse-json json-string false)))

(defn try-parse-percentage [ str ]
  (and (string? str)
       (let [ str (if (= \% (.charAt str (- (.length str) 1)))
                    (.substring str 0 (- (.length str) 1))
                    str)]
         (try-parse-double str))))

(def truthy-strings #{"yes" "true" "1" "y" "t" "on"})

(defn try-parse-boolean
  ([ str ]
   (try-parse-boolean str nil))
  ([ str default-value ]
   (if (string? str)
     (boolean
      (truthy-strings (.trim str)))
     default-value)))

;;; URI Parsing

(defn uri-path? [ uri ]
  "Returns only the path of the URI, if it is a parsable URI and false
  otherwise."
  (aand (parsable-string? uri)
        (try
          (.getPath (java.net.URI. it))
          (catch java.net.URISyntaxException ex
            (log/error "Invalid URI" uri)
            false))))

;;; Configuration Tools

(defn config-property
  ( [ name ] (config-property name nil))
  ( [ name default ]
      (let [prop-binding (System/getProperty name)]
        (if (nil? prop-binding)
          default
          (if-let [ int (try-parse-integer prop-binding) ]
            int
            prop-binding)))))

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


;;; Thread naming and process lifecycle

(defn call-with-thread-name [ fn name ]
  (let [thread (Thread/currentThread)
        initial-thread-name (.getName thread)]
    (try
      (.setName thread name)
      (fn)
      (finally
        (.setName thread initial-thread-name)))))

(defmacro with-thread-name [ thread-name & body ]
  `(call-with-thread-name (fn [] ~@body) ~thread-name))


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

(defmacro with-exception-barrier [ label & body ]
  `((exception-barrier (fn [] ~@body) ~label)))

(defmacro with-daemon-thread [ label & body ]
  `(future
     (with-exception-barrier ~label
       ~@body)))

;;; I/O

(defn binary-slurp
  [^java.io.File file]
  (let [result (byte-array (.length file))]
    (with-open [in (java.io.DataInputStream. (clojure.java.io/input-stream file))]
      (.readFully in result))
    result))

(defn edn-spit [filename collection]
  (spit (java.io.File. filename)
        (with-out-str
          (pprint/write collection :dispatch pprint/code-dispatch))))

(defn edn-slurp [ filename ]
  (edn/read-string (slurp filename)))

;;; Main

(defn app-entry
  ([ entry ]
   (config/with-config (config/load-config)
     (logging/setup-logging)
     (log/info "Starting App" (config/cval :app))
     (when (config/cval :development-mode)
       (log/warn "=== DEVELOPMENT MODE ==="))
     (with-exception-barrier :app-entry
       (entry))
     (log/info "end run."))))

(defmacro defmain [ arglist & body ]
  `(defn ~'-main ~arglist
     (app-entry (fn [ ] ~@body))))
