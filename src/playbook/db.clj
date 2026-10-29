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

(ns playbook.db
  "An application's database: an embedded sql-file database, opened
  from configuration, with its query cache, scheduled-job support and
  backups.

  This namespace needs sql-file, which playbook doesn't bring with it
  (it's a provided dependency): an application that uses it lists
  com.mschaef/sql-file among its own dependencies. Applications without
  a database don't load this namespace and don't need sql-file.

  Configuration:

  [:db :subname] - The database name; defaults to [:app :name]. The
     db.subname system property takes precedence over both.
  [:db :pool-size] - The most connections the pool keeps open
     (default 4, sql-file's default). Size it for the web server's
     request threads plus the scheduled jobs that can run at once.
  [:db :connection-timeout-sec] - How long to wait for a free
     connection before failing (default 30, HikariCP's default). A
     short timeout makes requests fail quickly, rather than queue,
     while the database is blocked (e.g. by the backup's CHECKPOINT
     DEFRAG).
  [:db :enable-query-cache] - Whether the query cache is on.
  [:db :backup-path] - Directory for scheduled backups (the :db-backup
     job); with none, there are no backups."
  (:use playbook.core)
  (:require [taoensso.timbre :as log]
            [playbook.config :as config]
            [sql-file.core :as sql-file]
            [sql-file.middleware :as sql-file-middleware]
            [sql-file.query-middleware :as query-middleware]
            [sql-file.query-cache :as query-cache]))

;;; The database

(defn- db-conn-spec [schemas]
  (let [pool-size (config/cval-or [:db :pool-size] nil)
        connection-timeout-sec (config/cval-or [:db :connection-timeout-sec] nil)]
    (cond-> {:name (or (config/property "db.subname")
                       (config/cval-or [:db :subname] (config/cval :app :name)))
             :schema-path ["sql/"]
             :schemas schemas}
      pool-size (assoc :pool-size pool-size)
      connection-timeout-sec (assoc :pool {:connection-timeout (* 1000 connection-timeout-sec)}))))

(defn call-with-database
  "Open the application's database (as a connection pool), installing
  the given schemas, and call f with it available through pool.
  Options:

  :schemas - sql-file schema list, e.g. [[\"myapp\" 0]]. Schema
     scripts are found under sql/ on the classpath."
  [{:keys [schemas]} f]
  (sql-file/with-pool [db-pool (db-conn-spec schemas)]
    (config/with-extended-config {:db-pool db-pool}
      (f))))

(defmacro with-database
  "Run body with the application's database open; see
  call-with-database."
  [options & body]
  `(call-with-database ~options (fn [] ~@body)))

(defn pool
  "The application's database connection pool. Fails if no database
  is open (see with-database)."
  []
  (config/cval :db-pool))

;;; Query cache
;;;
;;; One cache, shared by every thread that binds it: request handlers
;;; (through the application's web layer) and scheduled jobs (through
;;; job-wrapper) alike, so invalidation from either reaches the
;;; results cached by the other.

(defonce ^:private shared-query-cache (query-cache/make-query-cache))

(defn query-cache-enabled?
  "Whether the query cache is on: the [:db :enable-query-cache]
  configuration value."
  []
  (config/cval-or [:db :enable-query-cache] false))

(defn call-with-query-cache
  "Call f with the application's query cache bound, when it's enabled.
  When it isn't, just call f."
  [f]
  (if (query-cache-enabled?)
    (query-cache/call-with-query-cache shared-query-cache f)
    (f)))

(defn- fail-if-cache-enabled []
  (when (query-cache-enabled?)
    (FAIL "invalidate-cache! or clear-cache! called"
          " with no query cache bound on this thread. Is this"
          " running outside call-with-query-cache?")))

(defn clear-cache!
  "Clear the application's query cache."
  []
  (if (query-cache/current-query-cache)
    (query-cache/clear-cache!)
    (fail-if-cache-enabled)))

(defn invalidate-cache!
  "Clear what's cached under cache-name (see sql-file.query-cache) in
  the application's query cache."
  [cache-name]
  (if (query-cache/current-query-cache)
    (query-cache/invalidate-cache! cache-name)
    (fail-if-cache-enabled)))

(def query-middleware
  "The middleware for application queries, for yesql's defqueries:
  the current connection, the query cache (between the connection and
  the query mode, so cached results are already shaped), query mode,
  and logging."
  (comp query-middleware/current-connection-middleware
        query-cache/query-cache-middleware
        query-middleware/query-mode-middleware
        query-middleware/log-query-middleware))

;;; Scheduled jobs

(defn job-wrapper
  "Wrap a job function to run with a connection to the application's
  database and with its query cache bound: the :job-wrapper for
  playbook.service. Scheduled jobs run on the scheduler's threads,
  outside anything a request would bind, so without this a job's
  queries would have no connection and its cache invalidation would
  never reach the shared cache."
  [job-fn]
  (let [db-pool (pool)]
    #(sql-file-middleware/with-db-connection db-pool
       (call-with-query-cache job-fn))))

;;; Backups

(def ^:private backup-date-format "yyyyMMdd-HHmm")

(defn- backup-filename [backup-path]
  (format "%s/%s-backup-%s.tgz"
          backup-path
          (config/cval :app :name)
          (.format (java.text.SimpleDateFormat. backup-date-format) (current-time))))

(defn- backup-database [backup-path archive]
  (let [filename (backup-filename backup-path)
        conn (sql-file-middleware/current-db-connection)]
    (log/report "Backing database up to: " filename)
    (sql-file/checkpoint-defragment conn)
    (sql-file/backup-to-file-online conn filename)
    (log/report "Database backup complete")
    (when archive
      (archive (java.io.File. filename)))))

(defn backup-jobs
  "The scheduled jobs for backing up the database: a :db-backup job,
  if [:db :backup-path] is configured, and otherwise none. Each backup
  checkpoints and defragments the database, then writes an online
  backup to a new file under the backup path. Options:

  :archive - Optional function of the backup file (a java.io.File),
     called after each backup: for example, to copy it elsewhere."
  [{:keys [archive]}]
  (if-let [backup-path (config/cval-or [:db :backup-path] nil)]
    (do
      (log/info "Database backup configured with path: " backup-path)
      {:db-backup #(backup-database backup-path archive)})
    (do
      (log/warn "NO BACKUP PATH. AUTOMATIC BACKUP DISABLED!!!")
      {})))
