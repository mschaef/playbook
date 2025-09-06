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

(ns playbook.core-test
  (:use playbook.core)
  (:require [clojure.test :refer :all]))

(deftest fail-reports-exception
  (is (thrown-with-msg? RuntimeException #"test 1 xyzzy"
                        (FAIL "test " 1 " xyzzy")))

  (is (thrown-with-msg? RuntimeException #"test 2 xyzzy"
                        (FAIL (RuntimeException. "inner")
                              "test " 2 " xyzzy"))))

(deftest empty-string
  (testing "Empty string check"
    (is (= true (string-empty? "")))
    (is (= true (string-empty? "  ")))
    (is (= false (string-empty? "hi")))
    (is (= false (string-empty? " hi ")))))

(deftest parsable-string-check
  (testing "Parsable strings are properly detected"
    (is (= "string" (parsable-string? "string")))
    (is (= false (parsable-string? :not-a-string))))

  (testing "Parsable strings are trimmed"
    (is (= "string" (parsable-string? "  string  ")))))

(deftest boolean-parsing
  (testing "Numeric true values parse as true"
    (is (= true (try-parse-boolean "1"))))

  (testing "Lowercase true values parse as true"
    (is (= true (try-parse-boolean "y")))
    (is (= true (try-parse-boolean "yes")))
    (is (= true (try-parse-boolean "t")))
    (is (= true (try-parse-boolean "true"))))

  (testing "Uppercase true values parse as true"
    (is (= true (try-parse-boolean "Y")))
    (is (= true (try-parse-boolean "YES")))
    (is (= true (try-parse-boolean "T")))
    (is (= true (try-parse-boolean "TRUE"))))

  (testing "Mixed true values parse as true"
    (is (= true (try-parse-boolean "Yes")))
    (is (= true (try-parse-boolean "True"))))

  (testing "Non-true values parse as false"
    (is (= false (try-parse-boolean "false")))
    (is (= false (try-parse-boolean "0"))))

  (testing "Non-string values return fhe default value"
    (is (nil? (try-parse-boolean nil)))
    (is (= :default (try-parse-boolean 0 :default)))))

(deftest url-encoding
  (is (= "http://arlonet.com:8080/foo"
         (encode-url "http://arlonet.com:8080/foo"
                     {})))

  (is (= "http://arlonet.com:8080/foo?x=3"
         (encode-url "http://arlonet.com:8080/foo"
                     {:x 3})))

  (is (= "http://arlonet.com:8080/foo?x=3&y=4"
         (encode-url "http://arlonet.com:8080/foo"
                     {:x 3 :y 4})))

  (is (= "http://arlonet.com:8080/foo?x=3"
         (encode-url "http://arlonet.com:8080/foo"
                     {:x 3 :y nil})))

  (is (= "http://arlonet.com:8080/foo?x=3%3D4"
         (encode-url "http://arlonet.com:8080/foo"
                     {:x "3=4"}))))
