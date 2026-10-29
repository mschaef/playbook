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

(ns playbook.service
  "Running an application as a service: its scheduled jobs, and the
  body of code that is the application itself."
  (:require [playbook.scheduler :as scheduler]))

(defn call-with-service
  "Start the scheduler with the given jobs, then call f, returning its
  value. Options:

  :jobs - Map (or seq of pairs) of job name to no-argument function.
     Schedules come from the :job-schedule config map (see
     playbook.scheduler); a job with no schedule isn't run.
  :job-wrapper - Optional function of a job function, returning the
     function to schedule in its place: for example, to run each job
     with a database connection (see playbook.db/job-wrapper)."
  [{:keys [jobs job-wrapper]
    :or {job-wrapper identity}}
   f]
  (let [scheduler (scheduler/start)]
    (doseq [[job-name job-fn] jobs]
      (scheduler/schedule-job scheduler job-name (job-wrapper job-fn)))
    (f)))

(defmacro with-service
  "Run body as a service with the given options; see
  call-with-service."
  [options & body]
  `(call-with-service ~options (fn [] ~@body)))
