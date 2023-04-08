;; Copyright (c) 2015-2023 Michael Schaeffer
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

(ns playbook.hashid
  (:require [hashids.core :as hashids]))

(def default-opts
  {:salt ""
   :min-length 8})

(defn- typed-opts [ opts type ]
  (assoc opts :salt (str (name type) (:salt opts))))

(defn encode
  ([ type id ]
   (encode default-opts type id))

  ([ opts type id ]
   (str (name type) (hashids/encode (typed-opts (merge default-opts opts) type) id))))

(defn decode
  ([ type hid ]
   (decode default-opts type hid ))

  ([ opts type hid ]
   (let [typename (name type)]
     (and (.startsWith hid typename)
          (let [decoded (hashids/decode (typed-opts (merge default-opts opts) type)
                                        (.substring hid (count typename)))]
            (and (= 1 (count decoded))
                 (first decoded)))))))



