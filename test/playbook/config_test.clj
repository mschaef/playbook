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

(ns playbook.config-test
  (:use playbook.config)
  (:require [clojure.test :refer :all]))

(deftest cval-lookup
  (with-config {:point {:x 3 :y 4}
                :value "string-value"}
    (testing "Configuration value lookup"
      (is (= 3 (cval :point :x)))
      (is (= "string-value" (cval :value))))

    (testing "Configuration value lookup of missing key"
      (is (thrown-with-msg? RuntimeException
                            #"Cannot find configuration value at path: \(:missing-value\)"
                            (cval :missing-value))))))
