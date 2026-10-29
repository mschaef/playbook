;; Copyright (c) 2015-2026 Michael Schaeffer
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

(ns playbook.template
  "Markdown templates with {{name}} variables, rendered to HTML with
  their metadata."
  (:require [clojure.java.io :as jio]
            [markdown.core :as md]))

(defn- resolve-var [vars text]
  (let [varname (.trim text)]
    (if (> (count varname) 0)
      (or ((keyword varname) vars)
          (throw (RuntimeException. (str "Cannot find binding for template variable: " varname ", vars: " (keys vars)))))
      (throw (RuntimeException. "Empty template variable.")))))

(def ^:private md-var-regex #"\{\{([^}]+)\}\}")

(def ^:private md-var-token-regex
  ;; Stand-ins for {{variables}} while the markdown renders: letters
  ;; and digits only, so markdown leaves them alone. TEMPLATEVAR1X
  ;; isn't a prefix of TEMPLATEVAR11X, so they can't be confused.
  #"TEMPLATEVAR(\d+)X")

(defn render-markdown-template
  "Render a markdown template, substituting {{name}} variables from
  vars, and return {:html ... :metadata ...}. The values are
  substituted after the markdown is rendered, into the HTML and the
  metadata, so a value is never read as markdown: a name like
  \"[Claim your prize](https://x.co)\" stays text instead of becoming
  a link. Callers pass values ready for HTML (escaped, or deliberate
  markup)."
  [vars template-text]
  (let [names (vec (distinct (map #(.trim ^String (second %))
                                  (re-seq md-var-regex template-text))))
        index-of (zipmap names (range))
        values (mapv #(str (resolve-var vars %)) names)
        substitute (fn [s]
                     (clojure.string/replace s md-var-token-regex
                                             (fn [[_ i]] (nth values (Long/parseLong i)))))
        parsed (md/md-to-html-string-with-meta
                (clojure.string/replace template-text md-var-regex
                                        (fn [[_ name]]
                                          (str "TEMPLATEVAR" (index-of (.trim ^String name)) "X"))))]
    (-> parsed
        (update :html substitute)
        (update :metadata
                (fn [metadata]
                  (into {}
                        (map (fn [[k v]]
                               [k (if (string? v)
                                    (substitute v)
                                    (map #(if (string? %) (substitute %) %) v))]))
                        metadata))))))

(defn render-resource-template
  "Render the markdown template in the named classpath resource, or
  return nil if there is no such resource."
  ([resource-name vars]
   (when-let [template (jio/resource resource-name)]
     (render-markdown-template vars (slurp template))))

  ([resource-name]
   (render-resource-template resource-name {})))
