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

(ns playbook.hashid-test
  (:use playbook.hashid)
  (:require [clojure.test :refer :all]))

(deftest hashid-equality
  (testing "Hash ID's differ if the input is different."
    (is (not (= (encode :typea 1)
                (encode :typea 2)))))

  (testing "The ID portion of a Hash ID is different if the input type is different."
    (is (not= (.substring (encode :typea 1) 6)
              (.substring (encode :typeb 1) 6)))))

(deftest hashid-prefixes
  (testing "Hash ID's have their typename as a prefix."
    (is (.startsWith (encode :typea 1) "typea-"))
    (is (.startsWith (encode :typeb 1) "typeb-"))))

(deftest hashid-decode
  (testing "A number can be encoded and successfully decoded back to its original value."
    (is (= (decode :typea (encode :typea 42))
           42)))

  (testing "A hash ID cannot be decoded if the type does not match."
    (is (= (decode :badtype (encode :typea 42))
           false))))
