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

(ns playbook.db-test
  (:require [clojure.test :refer :all]
            [playbook.config :as config]
            [playbook.db :as db]
            [sql-file.middleware :as sql-file-middleware]
            [sql-file.query-cache :as query-cache]))

(def test-config
  {:app {:name "mem:playbook-db-test"}
   :db {:enable-query-cache true}})

(deftest database-pool
  (config/with-config test-config
    (testing "The pool is available while the database is open"
      (db/with-database {:schemas []}
        (is (some? (db/pool)))))

    (testing "There's no pool without an open database"
      (is (thrown? RuntimeException (db/pool))))))

(deftest jobs-get-a-connection-and-the-cache
  (config/with-config test-config
    (db/with-database {:schemas []}
      (let [job ((db/job-wrapper (fn []
                                   {:connection (some? (sql-file-middleware/current-db-connection))
                                    :cache (some? (query-cache/current-query-cache))})))]
        (is (= {:connection true :cache true} job))))))

(deftest query-cache-switch
  (testing "With the cache off, nothing is bound and invalidation is a no-op"
    (config/with-config (assoc-in test-config [:db :enable-query-cache] false)
      (is (nil? (db/call-with-query-cache query-cache/current-query-cache)))
      (is (nil? (db/invalidate-cache! ["x" 1])))))

  (testing "With the cache on, invalidating outside a bound cache fails"
    (config/with-config test-config
      (is (thrown-with-msg? RuntimeException #"no query cache bound"
                            (db/invalidate-cache! ["x" 1])))
      (is (some? (db/call-with-query-cache query-cache/current-query-cache))))))

(deftest backup-job
  (testing "No backup job without a backup path"
    (config/with-config test-config
      (is (= {} (db/backup-jobs {})))))

  (testing "A backup job with a backup path"
    (config/with-config (assoc-in test-config [:db :backup-path] "/tmp")
      (is (= #{:db-backup} (set (keys (db/backup-jobs {}))))))))
